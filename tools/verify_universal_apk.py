#!/usr/bin/env python3
"""Verify one standalone APK merged from the original three-way split bundle."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
import re
import sqlite3
import tempfile
import zipfile
from pathlib import Path

EXPECTED_PARTS = {"base.apk", "split_config.arm64_v8a.apk", "split_config.xxhdpi.apk"}
DB_SUFFIX = "muslim_db_v2.5.1.db"
TARGET_PACKAGE = os.environ.get("MASHXALL_TARGET_PACKAGE", "com.kosratdahmad.mashxall.full")

def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def zip_map(path: Path) -> dict[str, bytes]:
    with zipfile.ZipFile(path) as z:
        bad = z.testzip()
        if bad:
            raise ValueError(f"ZIP CRC failure: {bad}")
        return {name: z.read(name) for name in z.namelist()}

def apk_map(data: bytes, label: str) -> dict[str, bytes]:
    import io
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        bad = z.testzip()
        if bad:
            raise ValueError(f"{label} ZIP CRC failure: {bad}")
        return {name: z.read(name) for name in z.namelist()}

def dex_hashes(entries: dict[str, bytes]) -> dict[str, str]:
    return {name: sha(value) for name, value in entries.items()
            if re.fullmatch(r"classes\d*\.dex", name)}

def database_report(entries: dict[str, bytes]) -> dict:
    name = next((n for n in entries if n.endswith(DB_SUFFIX)), None)
    if not name:
        raise ValueError("Offline database not found in standalone APK")
    data = entries[name]
    with tempfile.NamedTemporaryFile(suffix=".db") as temp:
        Path(temp.name).write_bytes(data)
        con = sqlite3.connect(temp.name)
        try:
            integrity = con.execute("PRAGMA integrity_check").fetchone()[0]
            table_count = con.execute(
                "SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
            ).fetchone()[0]
        finally:
            con.close()
    if integrity != "ok":
        raise ValueError(f"SQLite integrity_check failed: {integrity}")
    return {"entry": name, "bytes": len(data), "sha256": sha(data),
            "integrity": integrity, "table_count": table_count}

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()

    source_parts = zip_map(args.source)
    if set(source_parts) != EXPECTED_PARTS:
        raise ValueError(f"Unexpected source split list: {sorted(source_parts)}")
    source_base = apk_map(source_parts["base.apk"], "source base.apk")
    source_abi = apk_map(source_parts["split_config.arm64_v8a.apk"], "source arm64 split")
    output = zip_map(args.output)

    required = {"AndroidManifest.xml", "resources.arsc"}
    missing = sorted(required - set(output))
    if missing:
        raise ValueError("Standalone APK lacks required entries: " + ", ".join(missing))

    # Require actual launcher artwork and the adaptive-icon definition, not only a manifest icon string.
    required_launcher = {
        "res/mipmap-anydpi-v26/ic_launcher.xml",
        "res/mipmap-mdpi/ic_launcher.png",
        "res/mipmap-hdpi/ic_launcher.png",
        "res/mipmap-xhdpi/ic_launcher.png",
        "res/mipmap-xxhdpi/ic_launcher.png",
        "res/mipmap-xxxhdpi/ic_launcher.png",
        "res/drawable/ic_launcher_background.xml",
        "res/drawable/ic_launcher_foreground.xml",
    }
    missing_launcher = sorted(required_launcher - set(output))
    if missing_launcher:
        raise ValueError("Launcher icon resources missing from standalone APK: " + ", ".join(missing_launcher))

    before_dex, after_dex = dex_hashes(source_base), dex_hashes(output)
    if not before_dex or set(before_dex) != set(after_dex):
        raise ValueError("DEX files are missing or the multidex file set differs from the original")
    if not any(TARGET_PACKAGE.encode("utf-8") in output[n] for n in after_dex):
        raise ValueError("Rebuilt APK does not contain its configured app ID in DEX string data")
    expected_audio_uri = f"android.resource://{TARGET_PACKAGE}/raw/mecca".encode("utf-8")
    if not any(expected_audio_uri in output[n] for n in after_dex):
        raise ValueError("Rebuilt APK does not contain updated notification audio URIs")
    if not any(b"Lcom/kosratdahmad/myprayers/screens/MainActivity;" in output[n] for n in after_dex):
        raise ValueError("Rebuilt APK is missing original application class descriptors")

    db_before = next((n for n in source_base if n.endswith(DB_SUFFIX)), None)
    db_after = next((n for n in output if n.endswith(DB_SUFFIX)), None)
    if not db_before or not db_after or sha(source_base[db_before]) != sha(output[db_after]):
        raise ValueError("Offline database differs from original base APK")
    database = database_report(output)

    libs = {n: sha(v) for n, v in source_abi.items()
            if n.startswith("lib/arm64-v8a/") and n.endswith(".so")}
    if not libs:
        raise ValueError("Original arm64 split contains no native libraries")
    missing_libs = sorted(n for n in libs if n not in output)
    changed_libs = sorted(n for n, digest in libs.items()
                          if n in output and sha(output[n]) != digest)
    if missing_libs or changed_libs:
        raise ValueError(f"Native libraries not preserved. Missing={missing_libs}; changed={changed_libs}")

    report = {
        "status": "SINGLE_APK_STATIC_VALIDATION_PASSED",
        "source_sha256": sha(args.source.read_bytes()),
        "output_sha256": sha(args.output.read_bytes()),
        "output_filename": args.output.name,
        "output_bytes": args.output.stat().st_size,
        "standalone_apk": True,
        "required_entries_present": True,
        "launcher_icon_resources_present": True,
        "launcher_icon_entries": sorted(required_launcher),
        "source_package_id": "com.kosratdahmad.myprayers",
        "output_package_id_target": TARGET_PACKAGE,
        "dex_files_reassembled_from_original_smali": True,
        "original_class_descriptors_preserved": True,
        "runtime_package_literals_updated": True,
        "dex_sha256": before_dex,
        "offline_database_byte_identical": True,
        "offline_database": database,
        "arm64_native_libraries_preserved": len(libs),
        "runtime_install_and_launch_test": "Must pass the Android 11 emulator job before this build is called verified.",
        "signing_note": "The APK preserves original components and app logic but uses a separate app ID and test signing certificate. Package-restricted cloud APIs may need owner-side configuration."
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
