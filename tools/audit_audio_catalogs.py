#!/usr/bin/env python3
"""Validate the source-linked Quran-reciter and adhan catalogs used in Mashxall."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from urllib.parse import unquote, urlparse

RECITER_EXPECTED = 288
ADHAN_EXPECTED = 231
FULL_SURAH_EXPECTED = 202
REQUIRED_RECITERS = {
    "ahmad al nufais",
    "nasser alqatami",
    "yasser al-dosari",
    "islam sobhi",
}
CC_BY_SA = "https://creativecommons.org/licenses/by-sa/4.0/"
ADHAN_PREFIX = "https://raw.githubusercontent.com/Kiwifu/adhan-mp3/main/"


def fail(message: str) -> None:
    raise SystemExit(f"AUDIO CATALOG AUDIT FAILED: {message}")


def read_json(path: Path) -> dict:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as exc:
        fail(f"Cannot parse {path}: {exc}")
    if not isinstance(data, dict):
        fail(f"{path} root must be a JSON object")
    return data


def audit_reciters(path: Path) -> None:
    data = read_json(path)
    records = data.get("records")
    if not isinstance(records, list) or len(records) != RECITER_EXPECTED:
        fail(f"reciter record count must be {RECITER_EXPECTED}")
    if data.get("catalog_license_url") != CC_BY_SA:
        fail("reciter metadata attribution/license URL is missing or changed")
    ids = [item.get("numeric_id") for item in records]
    if any(not isinstance(value, int) or value <= 0 for value in ids) or len(ids) != len(set(ids)):
        fail("reciter numeric IDs must be positive and unique")
    full_surah = 0
    names = set()
    for item in records:
        name = str(item.get("name", "")).strip()
        server = str(item.get("server", "")).strip()
        total = item.get("surah_total")
        source_id = str(item.get("source_id", "")).strip()
        if not name or not source_id:
            fail("reciter name/source_id is empty")
        parsed = urlparse(server)
        if parsed.scheme != "https" or parsed.netloc not in {"server1.mp3quran.net", "server2.mp3quran.net", "server3.mp3quran.net", "server4.mp3quran.net", "server5.mp3quran.net", "server6.mp3quran.net", "server7.mp3quran.net", "server8.mp3quran.net", "server9.mp3quran.net", "server10.mp3quran.net", "server11.mp3quran.net", "server12.mp3quran.net", "server13.mp3quran.net", "server14.mp3quran.net", "server15.mp3quran.net", "server16.mp3quran.net", "server17.mp3quran.net", "server18.mp3quran.net", "server19.mp3quran.net", "server20.mp3quran.net", "server21.mp3quran.net", "server22.mp3quran.net", "server23.mp3quran.net", "server24.mp3quran.net", "server25.mp3quran.net", "server26.mp3quran.net", "server27.mp3quran.net", "server28.mp3quran.net", "server29.mp3quran.net", "server30.mp3quran.net", "server31.mp3quran.net", "server32.mp3quran.net", "server33.mp3quran.net", "server34.mp3quran.net", "server35.mp3quran.net", "server36.mp3quran.net", "server37.mp3quran.net", "server38.mp3quran.net", "server39.mp3quran.net", "server40.mp3quran.net"}:
            fail(f"reciter {name!r} has an unexpected source host: {server!r}")
        if not isinstance(total, int) or not 0 <= total <= 114:
            fail(f"reciter {name!r} has invalid surah_total: {total!r}")
        if total == 114:
            full_surah += 1
        names.add(name.lower())
    if full_surah != FULL_SURAH_EXPECTED:
        fail(f"expected {FULL_SURAH_EXPECTED} complete-surah editions, found {full_surah}")
    missing = sorted(REQUIRED_RECITERS - names)
    if missing:
        fail("required reciters are missing: " + ", ".join(missing))
    print(f"PASS: {len(records)} attributed MP3Quran recitation editions; {full_surah} list all 114 surahs.")


def audit_adhan(path: Path) -> None:
    data = read_json(path)
    records = data.get("records")
    if not isinstance(records, list) or len(records) != ADHAN_EXPECTED:
        fail(f"adhan record count must be {ADHAN_EXPECTED}")
    ids = [item.get("id") for item in records]
    filenames = [item.get("source_filename") for item in records]
    if any(not isinstance(value, str) or not value for value in ids + filenames):
        fail("adhan IDs and original filenames must be non-empty")
    if len(ids) != len(set(ids)) or len(filenames) != len(set(filenames)):
        fail("adhan IDs and source filenames must be unique")
    total_bytes = 0
    for item in records:
        url = str(item.get("url", ""))
        filename = str(item.get("source_filename", ""))
        if not url.startswith(ADHAN_PREFIX):
            fail(f"adhan recording does not use the attributed original source: {url}")
        suffix = unquote(url[len(ADHAN_PREFIX):])
        if suffix != filename:
            fail(f"adhan URL path does not match the source filename: {filename}")
        if not filename.lower().endswith(".mp3"):
            fail(f"non-MP3 audio entry: {filename}")
        size = item.get("size_bytes")
        if not isinstance(size, int) or size <= 512:
            fail(f"invalid source size for {filename}: {size!r}")
        total_bytes += size
    if int(data.get("record_count", -1)) != len(records):
        fail("adhan catalog record_count metadata does not match records")
    if int(data.get("total_source_bytes", -1)) != total_bytes:
        fail("adhan catalog total_source_bytes metadata does not match file sizes")
    print(f"PASS: {len(records)} source-linked adhan/mosque tracks; {total_bytes:,} source bytes are streamed or downloaded on demand.")


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("Usage: audit_audio_catalogs.py RECITERS_JSON ADHAN_JSON")
    audit_reciters(Path(sys.argv[1]))
    audit_adhan(Path(sys.argv[2]))


if __name__ == "__main__":
    main()
