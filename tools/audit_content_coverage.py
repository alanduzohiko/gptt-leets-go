#!/usr/bin/env python3
"""Fail the build if core content from any source app is missing or unexpectedly small."""
from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any


def verify(catalog: dict[str, Any]) -> dict[str, Any]:
    files = {
        item.get("asset_path"): item
        for item in catalog.get("files", [])
        if isinstance(item, dict) and item.get("asset_path")
    }
    databases = {
        item.get("asset_path"): item
        for item in catalog.get("databases", [])
        if isinstance(item, dict) and item.get("asset_path")
    }
    generated = {
        item.get("asset_path"): item
        for item in catalog.get("generated_assets", [])
        if isinstance(item, dict) and item.get("asset_path")
    }
    checks: list[dict[str, Any]] = []

    def record(name: str, passed: bool, actual: Any, expected: Any, detail: str = "") -> None:
        checks.append({
            "name": name, "passed": bool(passed), "actual": actual, "expected": expected,
            **({"detail": detail} if detail else {}),
        })

    def require_asset(path: str) -> None:
        item = files.get(path) or generated.get(path)
        record(f"asset:{path}", item is not None, "present" if item else "missing", "present")

    def require_table(db_path: str, table_name: str, minimum_rows: int) -> None:
        db = databases.get(db_path)
        if db is None:
            record(f"table:{db_path}:{table_name}", False, "database missing", f">={minimum_rows} rows")
            return
        table = next((t for t in db.get("tables", []) if t.get("name") == table_name), None)
        if table is None:
            record(f"table:{db_path}:{table_name}", False, "table missing", f">={minimum_rows} rows")
            return
        rows = table.get("rows")
        passed = db.get("integrity") == "ok" and isinstance(rows, int) and rows >= minimum_rows
        detail = "" if db.get("integrity") == "ok" else f"database integrity is {db.get('integrity')!r}"
        record(f"table:{db_path}:{table_name}", passed, rows, f">={minimum_rows} rows", detail)

    noor = "content/noor_tafsir_1.2.2/base/assets/"
    salawati = "content/salawati_3.1.3/base/assets/database/"
    khairah = "content/khairah_1.2/base/assets/"

    # Quran, surahs, translations, source reciter metadata and tajweed geometry.
    require_table(noor + "quran.db", "quran", 6236)
    require_table(noor + "quran.db", "surah_name", 114)
    require_table(noor + "quran.db", "audio", 80)
    require_table(noor + "kurdish_transliteration.db", "quran", 6236)
    require_table(noor + "ayah_info.db", "page_data_line", 13000)
    require_table(noor + "ayah_info.db", "v2_tajweed_page_data_line", 12000)
    require_asset(khairah + "assets/mushaf/pages.bin")
    mushaf_index = generated.get("mushaf_svg_index.json")
    mushaf_pages = mushaf_index.get("page_count") if mushaf_index else None
    record("mushaf_svg_index", mushaf_pages == 604, mushaf_pages, 604)

    noor_tafsirs = [
        "tafseeri_roshn.db", "tafseeri_hazhar.db", "tafseer_maisar.db",
        "tafseer_sahih_international.db", "tafseeri_noor.db", "tafseeri_hawrami.db",
        "tafseeri_runahi_badini.db", "tafseeri_zhian_badini.db", "tafseeri_saadi.db",
        "tafseeri_mukhtasar.db", "tafseer_rebar.db", "tafseeri_tawhid.db",
    ]
    for filename in noor_tafsirs:
        require_table(noor + filename, "quran", 6236)
    khairah_tafsirs = [
        ("ibn-kathir.sqlite", 5000, 1900),
        ("muyassar.sqlite", 5000, 1900),
        ("saadi.sqlite", 2000, 1900),
        ("tabari.sqlite", 48000, 1900),
    ]
    for filename, minimum_pages, minimum_ranges in khairah_tafsirs:
        path = khairah + "tafsir/" + filename
        require_table(path, "pages", minimum_pages)
        require_table(path, "ayahs", minimum_ranges)

    # Hadith, all source collections and Hadith linked to Quran verses.
    require_table(noor + "db_hadith.db", "hadiths", 8202)
    require_table(noor + "db_hadith.db", "book", 13)
    require_table(noor + "db_hadith.db", "kitab", 427)
    require_table(noor + "db_hadith.db", "chapters", 6150)
    require_table(noor + "db_hadith_of_ayah.db", "hadith_on_ayah", 1141)

    # Full word-by-word morphology and the independent lexicon.
    require_table(noor + "db_word_by_word.db", "quran_corpus", 77429)
    require_table(noor + "db_word_by_word.db", "word_type", 45)
    require_table(noor + "db_words_lexicon.db", "word_list", 620)

    # Prayer sources and the complete offline worship collections.
    require_table(noor + "prayertimes.db", "staticprayertimes", 28000)
    require_table(noor + "prayertimes.db", "cities", 170)
    require_table(noor + "prayertimes.db", "extra", 180)
    require_table(noor + "prayertimes.db", "tasbih", 16)
    require_table(noor + "prayertimes.db", "allah_names", 99)
    require_table(noor + "prayertimes.db", "azkar_categories", 11)
    require_table(noor + "prayertimes.db", "azkar_chapters", 133)
    require_table(noor + "prayertimes.db", "azkar_items", 287)
    require_table(salawati + "muslim_db_v2.5.1.db", "country", 252)
    require_table(salawati + "muslim_db_v2.5.1.db", "location", 166404)
    require_table(salawati + "muslim_db_v2.5.1.db", "prayer_time", 45000)
    require_table(salawati + "muslim_db_v2.5.1.db", "azkar_category", 11)
    require_table(salawati + "muslim_db_v2.5.1.db", "azkar_chapter", 133)
    require_table(salawati + "muslim_db_v2.5.1.db", "azkar_item", 287)
    require_table(salawati + "muslim_db_v2.5.1.db", "name", 99)

    # Additional source catalogues.
    require_table(noor + "farmuda.db", "farmuda", 240)
    require_table(noor + "tn_books.db", "book_data", 94)

    counts = catalog.get("counts", {})
    record("source_app_count", counts.get("source_apps") == 3, counts.get("source_apps"), 3)
    record("extracted_database_count", counts.get("databases", 0) >= 20, counts.get("databases"), ">=20")
    record("audio_asset_count", counts.get("audio_files", 0) > 0, counts.get("audio_files"), ">0")
    record("font_asset_count", counts.get("font_files", 0) > 0, counts.get("font_files"), ">0")
    record("pdf_asset_count", counts.get("pdf_documents", 0) > 0, counts.get("pdf_documents"), ">0")

    configured_tafsir_paths = [
        *[noor + name for name in noor_tafsirs],
        *[khairah + "tafsir/" + name for name, _, _ in khairah_tafsirs],
    ]
    for path in configured_tafsir_paths:
        require_asset(path)

    passed = all(item["passed"] for item in checks)
    return {
        "schema_version": 1,
        "status": "pass" if passed else "fail",
        "passed_checks": sum(item["passed"] for item in checks),
        "failed_checks": sum(not item["passed"] for item in checks),
        "checks": checks,
        "catalog_counts": counts,
        "summary": {
            "noor_tafsir_works_required": len(noor_tafsirs),
            "khairah_page_tafsir_works_required": len(khairah_tafsirs),
            "total_tafsir_works_required": len(configured_tafsir_paths),
            "quran_verses_minimum": 6236,
            "hadith_records_minimum": 8202,
            "ayah_related_hadith_minimum": 1141,
            "word_analysis_records_minimum": 77429,
            "lexicon_entries_minimum": 620,
            "prayer_locations_minimum": 166404,
            "book_catalogue_entries_minimum": 94,
            "farmuda_records_minimum": 240,
            "mushaf_pages_required": 604,
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    report = verify(catalog)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "status": report["status"],
        "passed_checks": report["passed_checks"],
        "failed_checks": report["failed_checks"],
        "summary": report["summary"],
        "failures": [check for check in report["checks"] if not check["passed"]],
    }, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
