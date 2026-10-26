#!/usr/bin/env python3
"""Audit the bundled source reciter and adhan inventories without fabricating entries."""
from __future__ import annotations

import argparse
import json
import re
import sqlite3
import unicodedata
from collections import Counter
from pathlib import Path
from urllib.parse import urlparse

REQUESTED = {
    "Ahmed Al-Nafis": ["ahmed al-nafis", "ahmad al nafees", "ahmed al nafis", "أحمد النفيس", "احمد النفيس"],
    "Yasser Al-Dosari": ["yasser al-dosari", "yasser al dosari", "ياسر الدوسري"],
    "Nasser Al-Qatami": ["nasser al-qatami", "nasser al qatami", "ناصر القطامي"],
    "Islam Sobhi": ["islam sobhi", "islam subhi", "إسلام صبحي", "اسلام صبحي"],
}


def normalize(value: str) -> str:
    text = unicodedata.normalize("NFKC", value or "").casefold()
    text = "".join(ch for ch in unicodedata.normalize("NFD", text) if unicodedata.category(ch) != "Mn")
    text = re.sub(r"[ـ\u0640\s._'-]+", " ", text)
    return text.strip()


def inspect_reciters(database: Path) -> dict:
    if not database.is_file():
        raise FileNotFoundError(f"Reciter source database missing: {database}")
    connection = sqlite3.connect(database.resolve().as_uri() + "?mode=ro", uri=True, timeout=15)
    connection.row_factory = sqlite3.Row
    try:
        integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
        tables = {row[0] for row in connection.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        if "audio" not in tables:
            return {"integrity": integrity, "status": "fail", "reason": "audio table missing", "count": 0, "entries": []}
        columns = {row[1] for row in connection.execute('PRAGMA table_info("audio")')}
        needed = {"id", "name", "name_english", "audiotype", "url", "q_order"}
        missing = sorted(needed - columns)
        if missing:
            return {"integrity": integrity, "status": "fail", "reason": "audio columns missing", "missing_columns": missing, "count": 0, "entries": []}
        rows = connection.execute(
            "SELECT id,name,name_english,audiotype,url,q_order FROM audio ORDER BY q_order,name"
        ).fetchall()
        entries = []
        for row in rows:
            url = (row["url"] or "").strip()
            entries.append({
                "id": row["id"],
                "name_arabic": row["name"] or "",
                "name_english": row["name_english"] or "",
                "audio_type": row["audiotype"],
                "url_host": urlparse(url).hostname or "",
                "has_http_url": urlparse(url).scheme.lower() in {"http", "https"} and bool(urlparse(url).hostname),
                "order": row["q_order"],
            })
        counts = Counter(str(item["audio_type"]) for item in entries)
        matches = {}
        for requested, aliases in REQUESTED.items():
            normalized_aliases = [normalize(alias) for alias in aliases]
            matched = [
                item for item in entries
                if any(alias and alias in normalize(item["name_arabic"] + " " + item["name_english"]) for alias in normalized_aliases)
            ]
            matches[requested] = {
                "present_in_original_source": bool(matched),
                "matched_source_entries": matched,
            }
        valid_urls = sum(1 for item in entries if item["has_http_url"])
        return {
            "status": "pass" if integrity == "ok" and not missing else "fail",
            "integrity": integrity,
            "database": database.name,
            "source_reciter_count": len(entries),
            "audio_type_counts": dict(counts),
            "entries_with_valid_http_url": valid_urls,
            "entries_without_valid_http_url": len(entries) - valid_urls,
            "requested_reciters": matches,
            "entries": entries,
            "note": "Names/URLs are read only from the source database. Missing requested names are reported, never invented.",
        }
    finally:
        connection.close()


def inspect_adhan_assets(catalog_path: Path) -> dict:
    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    audio = catalog.get("audio", [])
    if not isinstance(audio, list):
        audio = []
    matches = []
    for item in audio:
        path = str(item.get("asset_path") or item.get("path") or "")
        title = str(item.get("title") or item.get("name") or "")
        combined = (path + " " + title).casefold()
        if any(term in combined for term in ("adhan", "azan", "muezzin", "muazzin", "athan", "اذان", "أذان", "مؤذن")):
            matches.append({
                "title": title,
                "asset_path": path,
                "source_app": item.get("source_app") or item.get("source_id") or "",
                "size_bytes": item.get("size_bytes"),
            })
    return {
        "source_audio_file_count": len(audio),
        "adhan_named_asset_count": len(matches),
        "adhan_named_assets": matches,
        "note": "Only assets named or labeled as adhan are counted here; reciter URLs remain a separate inventory.",
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", required=True, type=Path)
    parser.add_argument("--catalog", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    report = {
        "schema_version": 1,
        "reciter_catalog": inspect_reciters(args.database),
        "adhan_assets": inspect_adhan_assets(args.catalog),
    }
    report["status"] = "pass" if report["reciter_catalog"].get("status") == "pass" else "fail"
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    summary = {
        "status": report["status"],
        "source_reciter_count": report["reciter_catalog"].get("source_reciter_count", 0),
        "audio_type_counts": report["reciter_catalog"].get("audio_type_counts", {}),
        "requested_reciters_found": {
            name: value["present_in_original_source"]
            for name, value in report["reciter_catalog"].get("requested_reciters", {}).items()
        },
        "adhan_named_asset_count": report["adhan_assets"].get("adhan_named_asset_count", 0),
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
