#!/usr/bin/env python3
"""Verify the signed split APK set against the original and prepared unsigned files."""
from __future__ import annotations
import argparse, hashlib, json, os, re, sqlite3, tempfile, zipfile
from pathlib import Path

EXPECTED = {"base.apk", "split_config.arm64_v8a.apk", "split_config.xxhdpi.apk"}
DB_SUFFIX = "muslim_db_v2.5.1.db"
TARGET_PACKAGE = os.environ.get("MASHXALL_TARGET_PACKAGE", "com.kosratdahmad.mashxall.full")

def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def read_bundle(path: Path) -> dict[str, bytes]:
    with zipfile.ZipFile(path) as z:
        bad = z.testzip()
        if bad:
            raise ValueError(f"Container ZIP CRC error: {bad}")
        if set(z.namelist()) != EXPECTED:
            raise ValueError(f"Split filenames are not the expected set: {sorted(z.namelist())}")
        return {name: z.read(name) for name in z.namelist()}

def read_apk(data: bytes, label: str) -> dict[str, bytes]:
    import io
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        bad = z.testzip()
        if bad:
            raise ValueError(f"{label} ZIP CRC error: {bad}")
        return {name: z.read(name) for name in z.namelist()}

def dex_hashes(entries: dict[str, bytes]) -> dict[str, str]:
    return {name: sha(value) for name, value in entries.items() if re.fullmatch(r"classes\d*\.dex", name)}

def database_info(entries: dict[str, bytes]) -> dict:
    name = next((n for n in entries if n.endswith(DB_SUFFIX)), None)
    if name is None:
        raise ValueError("Bundled offline database is missing")
    payload = entries[name]
    with tempfile.NamedTemporaryFile(suffix=".db") as temp:
        Path(temp.name).write_bytes(payload)
        con = sqlite3.connect(temp.name)
        try:
            integrity = con.execute("PRAGMA integrity_check").fetchone()[0]
            tables = con.execute("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").fetchone()[0]
        finally:
            con.close()
    if integrity != "ok":
        raise ValueError(f"SQLite integrity check failed: {integrity}")
    return {"entry": name, "bytes": len(payload), "sha256": sha(payload), "integrity": integrity, "table_count": tables}

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--unsigned-dir", required=True, type=Path)
    parser.add_argument("--prepare-report", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()
    source, final = read_bundle(args.source), read_bundle(args.output)
    prepare = json.loads(args.prepare_report.read_text(encoding="utf-8"))
    rows = []
    for name in sorted(EXPECTED):
        src = read_apk(source[name], "source/" + name)
        out = read_apk(final[name], "output/" + name)
        unsigned_path = args.unsigned_dir / name
        if not unsigned_path.exists():
            raise ValueError("Unsigned prepared APK is missing: " + str(unsigned_path))
        unsigned = read_apk(unsigned_path.read_bytes(), "unsigned/" + name)
        if set(dex_hashes(src)) != set(dex_hashes(out)) or set(dex_hashes(src)) != set(dex_hashes(unsigned)):
            raise ValueError("DEX file set changed in " + name)
        if dex_hashes(out) != dex_hashes(unsigned):
            raise ValueError("Unsigned and signed DEX differ in " + name)
        if name != "base.apk" and dex_hashes(src) != dex_hashes(out):
            raise ValueError("Unexpected DEX modification in configuration split " + name)
        if sha(out["AndroidManifest.xml"]) != sha(unsigned["AndroidManifest.xml"]):
            raise ValueError("Signing step changed AndroidManifest.xml in " + name)
        if name != "base.apk" and sha(src["AndroidManifest.xml"]) != sha(out["AndroidManifest.xml"]):
            raise ValueError("Configuration split manifest was modified: " + name)
        rows.append({"file": name, "source_bytes": len(source[name]), "output_bytes": len(final[name]),
                     "dex_file_set_matches_original": True, "package_scoped_dex_literals_reassembled": name == "base.apk", "signed_manifest_matches_prepared": True})
        if name == "base.apk":
            before = next((n for n in src if n.endswith(DB_SUFFIX)), None)
            after = next((n for n in out if n.endswith(DB_SUFFIX)), None)
            if not before or not after or sha(src[before]) != sha(out[after]):
                raise ValueError("Offline database changed during build")
            db = database_info(out)
    if prepare.get("source_package_id") != "com.kosratdahmad.myprayers":
        raise ValueError("Preparation report does not identify the expected source package ID")
    if prepare.get("package_id_target") != TARGET_PACKAGE:
        raise ValueError("Preparation report does not identify the configured per-build Mashxall package ID")
    if prepare.get("manifest_semantics_preserved_except_explicit_identity_remap_and_split_metadata_removal") is not True:
        raise ValueError("Preparation report does not confirm manifest semantics outside the explicit identity remap and split-metadata removal")
    report = {
        "status": "STATIC_VALIDATION_PASSED",
        "source_sha256": sha(args.source.read_bytes()),
        "output_sha256": sha(args.output.read_bytes()),
        "source_package_id": "com.kosratdahmad.myprayers",
        "package_id": TARGET_PACKAGE,
        "manifest_semantics_preserved_except_explicit_identity_remap_and_split_metadata_removal": True,
        "dex_files_reassembled_from_original_smali": True,
        "offline_database": db,
        "parts": rows,
        "runtime_test": "Not covered by static split-set verification; use the standalone APK and Android 11 emulator result.",
        "install_note": "The split set carries a distinct app ID. The standalone APK is the recommended test artifact because it combines the three original splits."
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
