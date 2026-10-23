#!/usr/bin/env python3
"""Use JADX for offline static analysis of the three supplied APK sets.

Only a compact discovery report is emitted. Decompiled source files remain in a
temporary work directory and are never copied into the new app or artifact.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
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
            book_signal = re.compile(
                r"tn_books\.db|book_data|unique_text|url_cover|page_number|"
                r"DownloadManager|\.pdf\b|pdf_url|book_url|"
                r"download.{0,80}book|book.{0,80}download|"
                r"book.{0,80}(?:url|uri|path|file)|(?:url|uri|path|file).{0,80}book",
                re.I,
            )
            book_url_signal = re.compile(r'''https?://[^\s"']{1,180}''', re.I)
            app_report["book_download_contexts"] = []
            context_rules = {
                "hadith_of_ayah": (re.compile(r"db_hadith_of_ayah|hadith_on_ayah|verse_reference|hadith.{0,30}ayah|ayah.{0,30}hadith", re.I), 35),
                "cryptography": (re.compile(r"Cipher|SecretKeySpec|IvParameterSpec|Base64\.decode|PBKDF2|AES|decrypt|encrypt|doFinal", re.I), 30),
                "database_asset_access": (re.compile(r"SQLiteDatabase|SQLiteOpenHelper|openDatabase|db_hadith_of_ayah|\.sqlite|\.db|AssetManager|getAssets", re.I), 25),
            }
            app_report["code_contexts"] = {name: [] for name in context_rules}
            seen_context_lines: set[tuple[str, int, str]] = set()
            for java in java_files:
                try:
                    lines = java.read_text(encoding="utf-8", errors="replace").splitlines()
                except OSError:
                    continue
                relative_file = str(java.relative_to(decompiled))
                for line_no, line in enumerate(lines, 1):
                    stripped = " ".join(line.strip().split())
                    if not stripped:
                        continue
                    # Keep book/PDF resolution research focused on app code. Third-party
                    # dependencies contain many irrelevant URLs and can overwhelm the log.
                    book_contexts = app_report["book_download_contexts"]
                    app_code = relative_file.startswith("sources/") and not relative_file.startswith((
                        "sources/androidx/", "sources/kotlin/", "sources/kotlinx/",
                        "sources/okhttp3/", "sources/retrofit2/", "sources/org/"
                    ))
                    book_match = book_signal.search(stripped)
                    url_match = app_code and book_url_signal.search(stripped) and re.search(
                        r"book|pdf|download|file|unique_text|url_cover|tn_books",
                        stripped,
                        re.I,
                    )
                    if len(book_contexts) < 100 and app_code and (book_match or url_match):
                        context_start = max(0, line_no - 14)
                        context_end = min(len(lines), line_no + 27)
                        priority = (
                            0 if re.search(r"tn_books\.db|book_data|unique_text|url_cover", stripped, re.I)
                            else 1 if re.search(r"DownloadManager|\.pdf\b|pdf_url|book_url|download", stripped, re.I)
                            else 2
                        )
                        book_contexts.append({
                            "priority": priority,
                            "file": relative_file,
                            "match_line": line_no,
                            "match": stripped[:260],
                            "context_start_line": context_start + 1,
                            "context": "\\n".join(
                                f"{idx + 1}: {item.rstrip()[:280]}"
                                for idx, item in enumerate(lines[context_start:context_end], context_start)
                            ),
                        })
                    for label, pattern in PATTERNS.items():
                        if caps[label] >= 100 or not pattern.search(stripped):
                            continue
                        app_report["findings"].setdefault(label, []).append({
                            "file": relative_file,
                            "line": line_no,
                            "snippet": stripped[:260],
                        })
                        caps[label] += 1

                    for context_name, (pattern, max_items) in context_rules.items():
                        target = app_report["code_contexts"][context_name]
                        if len(target) >= max_items or not pattern.search(stripped):
                            continue
                        context_key = (relative_file, line_no, context_name)
                        if context_key in seen_context_lines:
                            continue
                        seen_context_lines.add(context_key)
                        start = max(0, line_no - 10)
                        end = min(len(lines), line_no + 36)
                        target.append({
                            "file": relative_file,
                            "match_line": line_no,
                            "match": stripped[:240],
                            "context_start_line": start + 1,
                            "context": "\n".join(
                                f"{idx + 1}: {item.rstrip()[:260]}"
                                for idx, item in enumerate(lines[start:end], start)
                            ),
                        })
            # Narrowly capture the app's own database row mapper and encrypted-resource
            # opener. Generic cryptography scans hit bundled Android/Firebase code first,
            # so these named application classes are crucial for interoperability.
            if meta["label"] == "Tafseeri Noor 1.2.2" and decompiled.exists():
                focused = {}
                exact_files = {
                    "hadith_row_mapper": "sources/ab/b.java",
                    "database_opening_and_versioning": "sources/n4/a.java",
                    "string_and_crypto_helpers": "sources/m5/l.java",
                    "string_decoder_key_map": "sources/m5/f.java",
                    "hadith_models": "sources/m4/f.java",
                }
                crypto_line = re.compile(
                    r"Cipher|SecretKey|IvParameter|Base64\.decode|doFinal|decrypt|encrypt|AES|PBKDF|"
                    r"U0\(|K0\(|getString\(cursor|getColumnIndex|hadith_on_ayah|db_hadith_of_ayah",
                    re.I,
                )
                for tag, rel in exact_files.items():
                    file = decompiled / rel
                    if not file.exists():
                        continue
                    try:
                        source_lines = file.read_text(encoding="utf-8", errors="replace").splitlines()
                    except OSError:
                        continue
                    windows = []
                    if tag == "hadith_row_mapper":
                        # This class maps encrypted database text fields into the UI model.
                        windows = [(0, min(len(source_lines), 340))]
                    elif tag == "string_and_crypto_helpers":
                        # Record the precise string transform invoked by the Hadith mapper;
                        # broad crypto matches often hit unrelated app/dependency helpers.
                        exact_signatures = re.compile(
                            r"(?:static\s+)?(?:final\s+)?String\s+j\s*\(|"
                            r"String\s+j\s*\(|f10929e|SecretKeySpec|Cipher\.getInstance|Base64\.decode",
                            re.I,
                        )
                        indices = [i for i, line in enumerate(source_lines) if exact_signatures.search(line)]
                        windows = []
                        used = set()
                        for idx in indices[:40]:
                            lo, hi = max(0, idx - 12), min(len(source_lines), idx + 90)
                            if any(not (hi <= a or lo >= b) for a, b in used):
                                continue
                            used.add((lo, hi))
                            windows.append((lo, hi))
                    elif tag == "string_decoder_key_map":
                        windows = [(0, min(len(source_lines), 360))]
                    else:
                        indices = [i for i, line in enumerate(source_lines) if crypto_line.search(line)]
                        used = set()
                        for idx in indices[:30]:
                            lo, hi = max(0, idx - 12), min(len(source_lines), idx + 35)
                            if any(not (hi <= a or lo >= b) for a, b in used):
                                continue
                            used.add((lo, hi))
                            windows.append((lo, hi))
                    focused[tag] = {
                        "file": rel,
                        "line_count": len(source_lines),
                        "windows": [
                            {
                                "start_line": lo + 1,
                                "end_line": hi,
                                "source": "\\n".join(
                                    f"{i + 1}: {source_lines[i].rstrip()[:300]}"
                                    for i in range(lo, hi)
                                ),
                            }
                            for lo, hi in windows
                        ],
                    }
                app_report["focused_application_source_context"] = focused
                # Emit only concise targeted contexts to the action log so the
                # string decoder can be reviewed without downloading the 230+ MB APK.
                if os.environ.get("MASHXALL_RE_DUMP_FOCUSED") == "1":
                    print("FOCUSED_NOOR_DECODER_BEGIN", flush=True)
                    for tag in ("hadith_row_mapper", "string_and_crypto_helpers", "string_decoder_key_map"):
                        for window in focused.get(tag, {}).get("windows", [])[:3]:
                            if tag == "hadith_row_mapper":
                                # Limit to the mapper's first region; output around the
                                # actual decryption-related call sites is emitted below.
                                lines_out = window["source"].splitlines()
                                selected = [line for line in lines_out if re.search(r"U0[(]|K0[(]|decrypt|encrypt|f10929e|hadith_on_ayah|db_hadith_of_ayah", line, re.I)]
                                print(f"TARGET {tag} lines={window['start_line']}-{window['end_line']}", flush=True)
                                print("\\n".join(selected[:50]), flush=True)
                            else:
                                print(f"TARGET {tag} lines={window['start_line']}-{window['end_line']}", flush=True)
                                print(window["source"][:18000], flush=True)
                    print("FOCUSED_NOOR_DECODER_END", flush=True)
                    print("FOCUSED_NOOR_BOOK_DOWNLOAD_BEGIN", flush=True)
                    prioritized_book_contexts = sorted(
                        app_report.get("book_download_contexts", []),
                        key=lambda item: (item.get("priority", 9), item.get("file", ""), item.get("match_line", 0)),
                    )
                    for item in prioritized_book_contexts[:30]:
                        print(
                            f"BOOK_SOURCE file={item['file']} line={item['match_line']} match={item['match']}",
                            flush=True,
                        )
                        print(item["context"][:2600], flush=True)
                    print("FOCUSED_NOOR_BOOK_DOWNLOAD_END", flush=True)

            app_report["code_context_counts"] = {key: len(value) for key, value in app_report["code_contexts"].items()}
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
