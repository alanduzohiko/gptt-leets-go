#!/usr/bin/env python3
"""Restore Android install-friendly ZIP storage for DEX, resource table and native libraries."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import tempfile
import zipfile
from pathlib import Path


DEX_RE = re.compile(r"classes\d*\.dex$")


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def normalize_apk(input_path: Path) -> dict:
    input_path = input_path.resolve()
    tmp = input_path.with_name(input_path.name + ".normalized.tmp")
    before = {}
    source_meta = {}
    with zipfile.ZipFile(input_path, "r") as source:
        bad = source.testzip()
        if bad:
            raise ValueError(f"Input APK ZIP CRC failure: {bad}")
        infos = source.infolist()
        names = [i.filename for i in infos]
        if len(names) != len(set(names)):
            raise ValueError("Input APK has duplicate ZIP entry names")
        if "AndroidManifest.xml" not in names or "resources.arsc" not in names:
            raise ValueError("Input APK is missing AndroidManifest.xml or resources.arsc")
        with zipfile.ZipFile(tmp, "w", allowZip64=True) as output:
            for info in infos:
                if info.flag_bits & 0x1:
                    raise ValueError("Encrypted ZIP entries are not supported in APKs")
                payload = source.read(info.filename)
                before[info.filename] = sha(payload)
                is_dex = bool(DEX_RE.fullmatch(info.filename))
                is_native = info.filename.startswith("lib/") and info.filename.endswith(".so")
                is_resource_table = info.filename == "resources.arsc"
                method = zipfile.ZIP_STORED if (is_dex or is_native or is_resource_table) else info.compress_type
                copied = zipfile.ZipInfo(info.filename, info.date_time)
                copied.compress_type = method
                copied.comment = info.comment
                copied.create_system = info.create_system
                copied.external_attr = info.external_attr
                copied.internal_attr = info.internal_attr
                copied.extra = b""  # zipalign will add proper padding after this normalization
                output.writestr(copied, payload, compress_type=method, compresslevel=6)
                source_meta[info.filename] = {
                    "source_method": info.compress_type,
                    "output_method": method,
                    "bytes": len(payload),
                }

    after = {}
    with zipfile.ZipFile(tmp, "r") as output:
        bad = output.testzip()
        if bad:
            raise ValueError(f"Normalized APK ZIP CRC failure: {bad}")
        for info in output.infolist():
            after[info.filename] = sha(output.read(info.filename))
            if info.filename not in before or before[info.filename] != after[info.filename]:
                raise ValueError("APK entry content changed during compression normalization: " + info.filename)
            if DEX_RE.fullmatch(info.filename) and info.compress_type != zipfile.ZIP_STORED:
                raise ValueError("DEX is still compressed after normalization: " + info.filename)
            if info.filename == "resources.arsc" and info.compress_type != zipfile.ZIP_STORED:
                raise ValueError("resources.arsc is still compressed after normalization")
            if info.filename.startswith("lib/") and info.filename.endswith(".so") and info.compress_type != zipfile.ZIP_STORED:
                raise ValueError("Native library is still compressed after normalization: " + info.filename)
    if set(before) != set(after):
        raise ValueError("APK ZIP entry inventory changed during normalization")
    tmp.replace(input_path)
    return {
        "status": "APK_STORAGE_NORMALIZATION_PASSED",
        "input_apk": input_path.name,
        "entry_count": len(before),
        "payloads_byte_identical": True,
        "dex_entries_stored_uncompressed": sum(bool(DEX_RE.fullmatch(name)) for name in before),
        "native_library_entries_stored_uncompressed": sum(name.startswith("lib/") and name.endswith(".so") for name in before),
        "resources_arsc_stored_uncompressed": True,
        "changes": {
            name: item for name, item in source_meta.items()
            if item["source_method"] != item["output_method"]
        },
        "note": "The APK must still be zipaligned, signed, apksigner-verified and package-manager install-tested after this step.",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    report = normalize_apk(args.input)
    serialized = json.dumps(report, ensure_ascii=False, indent=2)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(serialized, encoding="utf-8")
    print(serialized)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
