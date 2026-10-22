#!/usr/bin/env python3
"""Use JADX for offline static analysis of the three supplied APK sets.

Only a compact discovery report is emitted. Decompiled source files remain in a
temporary work directory and are never copied into the new app or artifact.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import re
import shutil
import subprocess
import tempfile
import zipfile
from pathlib import Path

SOURCES = {
    "salawati-3.1.3.apks": {
        "label": "Salawati 3.1.3",
        "sha256": "bba56d408b552e31be43071af6a02dcdd66608b594b1fa10ca1a63fb51a504e3",
    },
    "khairah-1.2.apks": {
        "label": "Khairah 1.2",
        "sha256": "36b605314a66e5809c111565e7452f91578d4b0de3e009801118d40f1def85b8",
    },
    "noor-tafsir-1.2.2.apks": {
        "label": "Tafseeri Noor 1.2.2",
        "sha256": "fa9259501abd4b1c7f870109a1ad49567badb27792510cee3d5ce2ee28e11ce8",
    },
}
PATTERNS = {
    "binary_page_store": re.compile(r"pages\.bin|\.bin\b|byte_count|search_segments|sha256|page_id", re.I),
    "cryptography_and_integrity": re.compile(r"Cipher\.getInstance|SecretKeySpec|IvParameterSpec|MessageDigest|AES|PBKDF2|decrypt|encrypt|CRC32|Base64", re.I),
    "compression": re.compile(r"Inflater|InflaterInputStream|GZIP|Deflater|ZipInputStream|zstd|brotli|LZ4|XZInputStream", re.I),
    "asset_loading": re.compile(r"getAssets\(|openFd\(|AssetManager|assets/tafsir|\.sqlite|quran\.db|pages\.bin|db_hadith_of_ayah", re.I),
    "quran_audio": re.compile(r"quranicaudio|everyayah|audiotype|q_order|download\.quran|\.mp3", re.I),
    "hadith_of_ayah": re.compile(r"hadith_on_ayah|db_hadith_of_ayah|verse_reference|hadith.{0,30}ayah|ayah.{0,30}hadith", re.I),
}

FOCUS_CONTEXT = re.compile(
    r"db_hadith_of_ayah|hadith_on_ayah|verse_reference|Base64\.decode|Cipher\.getInstance|SecretKeySpec|IvParameterSpec|PBKDF2|AES|decrypt|encrypt|sqlcipher",
    re.I,
)


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def sha_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def run(cmd: list[str], timeout: int) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=timeout)

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle", required=True, type=Path)
    parser.add_argument("--jadx", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--workdir", type=Path, default=None)
    parser.add_argument("--timeout-per-app", type=int, default=1200)
    args = parser.parse_args()

    report = {
        "bundle_sha256": sha_file(args.bundle),
        "tool": "JADX",
        "purpose": "Static discovery only; no original decompiled sources are copied into the new app.",
        "applications": [],
    }
    temp_context = tempfile.TemporaryDirectory(prefix="mashxall-re-") if args.workdir is None else None
    work = Path(temp_context.name) if temp_context else args.workdir.resolve()
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True, exist_ok=True)

    with zipfile.ZipFile(args.bundle) as bundle:
        bad = bundle.testzip()
        if bad:
            raise SystemExit("Source bundle ZIP CRC error at " + bad)
        for filename, meta in SOURCES.items():
            member = "input/" + filename
            if member not in bundle.namelist():
                raise SystemExit("Required input is missing: " + member)
            apks_bytes = bundle.read(member)
            actual = digest(apks_bytes)
            if actual != meta["sha256"]:
                raise SystemExit("Original input hash mismatch for " + member)
            app_dir = work / filename.removesuffix(".apks")
            app_dir.mkdir()
            with tempfile.NamedTemporaryFile(suffix=".apks") as tmp_set:
                tmp_set.write(apks_bytes)
                tmp_set.flush()
                with zipfile.ZipFile(tmp_set.name) as split_set:
                    if split_set.testzip():
                        raise SystemExit("Split set ZIP CRC error: " + filename)
                    if "base.apk" not in split_set.namelist():
                        raise SystemExit("base.apk not found in " + filename)
                    base_bytes = split_set.read("base.apk")
                    base_apk = app_dir / "base.apk"
                    base_apk.write_bytes(base_bytes)
                    split_names = sorted(n for n in split_set.namelist() if n.endswith(".apk"))

            decompiled = app_dir / "jadx-output"
            cmd = [str(args.jadx.resolve()), "--no-res", "--threads-count", "4", "-d", str(decompiled), str(base_apk)]
            try:
                proc = run(cmd, args.timeout_per_app)
                status = "completed" if proc.returncode == 0 else "completed_with_errors"
                tool_output = proc.stdout[-1800:]
            except subprocess.TimeoutExpired:
                status = "timeout"
                tool_output = "JADX exceeded timeout of " + str(args.timeout_per_app) + " seconds."
            java_files = sorted(decompiled.rglob("*.java")) if decompiled.exists() else []
            app_report = {
                "label": meta["label"],
                "source_archive": filename,
                "source_sha256": actual,
                "base_apk_sha256": digest(base_bytes),
                "split_names": split_names,
                "jadx_status": status,
                "java_file_count": len(java_files),
                "findings": {},
                "jadx_tail": tool_output,
            }
            caps = {name: 0 for name in PATTERNS}
            app_report["code_contexts"] = []
            seen_context_lines: set[tuple[str, int]] = set()
            for java in java_files:
                try:
                    lines = java.read_text(encoding="utf-8", errors="replace").splitlines()
                except OSError:
                    continue
                for line_no, line in enumerate(lines, 1):
                    stripped = " ".join(line.strip().split())
                    if not stripped:
                        continue
                    for label, pattern in PATTERNS.items():
                        if caps[label] >= 100 or not pattern.search(stripped):
                            continue
                        app_report["findings"].setdefault(label, []).append({
                            "file": str(java.relative_to(decompiled)),
                            "line": line_no,
                            "snippet": stripped[:260],
                        })
                        caps[label] += 1

                    # Compact context excerpts help identify real decoder methods,
                    # DB-opening logic and key construction without shipping the
                    # original decompiled source tree. Context stays scoped to
                    # lines explicitly mentioning sensitive data flow indicators.
                    if FOCUS_CONTEXT.search(stripped) and len(app_report["code_contexts"]) < 45:
                        context_key = (str(java.relative_to(decompiled)), line_no)
                        if context_key in seen_context_lines:
                            continue
                        seen_context_lines.add(context_key)
                        start = max(0, line_no - 9)
                        end = min(len(lines), line_no + 32)
                        app_report["code_contexts"].append({
                            "file": str(java.relative_to(decompiled)),
                            "match_line": line_no,
                            "match": stripped[:240],
                            "context_start_line": start + 1,
                            "context": "\\n".join(
                                f"{idx + 1}: {line.rstrip()[:260]}"
                                for idx, line in enumerate(lines[start:end], start)
                            ),
                        })
            app_report["finding_counts"] = {key: len(value) for key, value in app_report["findings"].items()}
            # Manifest/resources and binaries stay temporary; only metadata and snippets are emitted.
            shutil.rmtree(decompiled, ignore_errors=True)
            base_apk.unlink(missing_ok=True)
            report["applications"].append(app_report)
            print(meta["label"], status, "java_files=", len(java_files), "findings=", app_report["finding_counts"], flush=True)

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    if temp_context:
        temp_context.cleanup()
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
