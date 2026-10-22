#!/usr/bin/env python3
"""Focused JADX extraction for Noor's Hadith string-decoding call chain."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import tempfile
import zipfile
from pathlib import Path

EXPECTED_NOOR_SHA256 = "fa9259501abd4b1c7f870109a1ad49567badb27792510cee3d5ce2ee28e11ce8"
TARGETS = [
    ("hadith_row_mapper", "ab.b"),
    ("database_opening", "n4.a"),
    ("string_crypto_helper", "m5.l"),
    ("string_decoder_key_map", "m5.f"),
    ("hadith_model", "m4.f"),
    ("tajweed_mushaf_activity", "com.dev.hazhanjalal.tafseerinoor.ui.page_style.AyahMushafActivity"),
]
INTERESTING = re.compile(
    r"U0[(]|K0[(]|f10929e|Cipher|SecretKeySpec|IvParameterSpec|Base64|doFinal|"
    r"decrypt|encrypt|hadith_on_ayah|db_hadith_of_ayah|StringBuilder|new String|"
    r"Map<|HashMap|put[(]|v2_tajweed_page_data_line|page_data_line|glyph_id|"
    r"ayah_info|tajweed|mushaf|Mushaf",
    re.I,
)


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle", required=True, type=Path)
    parser.add_argument("--jadx", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--timeout-per-class", type=int, default=180)
    args = parser.parse_args()

    with zipfile.ZipFile(args.bundle) as outer:
        member = "input/noor-tafsir-1.2.2.apks"
        noor_data = outer.read(member)
        actual = sha(noor_data)
        if actual != EXPECTED_NOOR_SHA256:
            raise SystemExit(f"Noor source fingerprint mismatch: {actual}")
        with tempfile.TemporaryDirectory(prefix="mashxall-noor-focused-") as temp:
            root = Path(temp)
            set_path = root / "noor.apks"
            set_path.write_bytes(noor_data)
            with zipfile.ZipFile(set_path) as apkset:
                base_apk = root / "base.apk"
                base_apk.write_bytes(apkset.read("base.apk"))

            records = []
            for label, class_name in TARGETS:
                target_root = root / label
                command = [
                    str(args.jadx.resolve()),
                    "--no-res",
                    "--single-class",
                    class_name,
                    "--threads-count",
                    "2",
                    "-d",
                    str(target_root),
                    str(base_apk),
                ]
                try:
                    proc = subprocess.run(
                        command,
                        stdout=subprocess.PIPE,
                        stderr=subprocess.STDOUT,
                        text=True,
                        timeout=args.timeout_per_class,
                    )
                    status = "completed" if proc.returncode == 0 else "completed_with_errors"
                    tool_tail = proc.stdout[-1000:]
                except subprocess.TimeoutExpired:
                    status = "timeout"
                    tool_tail = f"JADX exceeded {args.timeout_per_class}s for {class_name}"

                rel = "sources/" + class_name.replace(".", "/") + ".java"
                source_path = target_root / rel
                if not source_path.exists():
                    alternatives = list(target_root.rglob(class_name.split(".")[-1] + ".java"))
                    source_path = next(
                        (p for p in alternatives if p.as_posix().endswith(rel)),
                        alternatives[0] if alternatives else source_path,
                    )
                source = source_path.read_text(encoding="utf-8", errors="replace") if source_path.exists() else ""
                lines = source.splitlines()
                if label == "hadith_row_mapper":
                    selected = [(0, min(len(lines), 340))]
                elif label == "string_decoder_key_map":
                    selected = [(0, min(len(lines), 360))]
                else:
                    windows = []
                    for index, line in enumerate(lines):
                        if not INTERESTING.search(line):
                            continue
                        low, high = max(0, index - 12), min(len(lines), index + 55)
                        if any(not (high <= a or low >= b) for a, b in windows):
                            continue
                        windows.append((low, high))
                        if len(windows) >= 12:
                            break
                    selected = windows

                snippets = [
                    {
                        "start_line": lo + 1,
                        "end_line": hi,
                        "source": "\n".join(
                            f"{i + 1}: {lines[i].rstrip()[:320]}"
                            for i in range(lo, hi)
                        ),
                    }
                    for lo, hi in selected
                ]
                record = {
                    "label": label,
                    "class": class_name,
                    "status": status,
                    "source_file": str(source_path.relative_to(target_root)) if source_path.exists() else None,
                    "line_count": len(lines),
                    "snippets": snippets,
                    "jadx_tail": tool_tail,
                }
                records.append(record)
                print(f"FOCUSED_CLASS {class_name} status={status} lines={len(lines)}", flush=True)
                for snippet in snippets:
                    print(f"--- {label} lines {snippet['start_line']}-{snippet['end_line']} ---", flush=True)
                    print(snippet["source"], flush=True)
                if not source:
                    print(tool_tail, flush=True)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps({
        "source": "Tafseeri Noor 1.2.2",
        "source_sha256": EXPECTED_NOOR_SHA256,
        "targets": records,
    }, ensure_ascii=False, indent=2), encoding="utf-8")
    print("FOCUSED_DECODER_REPORT", str(args.out), flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
