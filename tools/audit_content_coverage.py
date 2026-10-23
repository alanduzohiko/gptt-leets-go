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

    def require_columns(db_path: str, table_name: str, expected_columns: tuple[str, ...]) -> None:
        db = databases.get(db_path)
        if db is None:
            record(f"columns:{db_path}:{table_name}", False, "database missing", sorted(expected_columns))
            return
        table = next((t for t in db.get("tables", []) if t.get("name") == table_name), None)
        if table is None:
            record(f"columns:{db_path}:{table_name}", False, "table missing", sorted(expected_columns))
            return
        actual = set(table.get("columns", []))
        missing = sorted(set(expected_columns) - actual)
        passed = db.get("integrity") == "ok" and not missing
        detail = ""
        if db.get("integrity") != "ok":
            detail = f"database integrity is {db.get('integrity')!r}"
        elif missing:
            detail = "missing required columns: " + ", ".join(missing)
        record(
            f"columns:{db_path}:{table_name}",
            passed,
            sorted(actual),
            sorted(expected_columns),
            detail,
        )

    noor = "content/noor_tafsir_1.2.2/base/assets/"
    salawati = "content/salawati_3.1.3/base/assets/database/"
    khairah = "content/khairah_1.2/base/assets/"

    # Quran, surahs, translations, source reciter metadata and tajweed geometry.
    require_table(noor + "quran.db", "quran", 6236)
    require_table(noor + "quran.db", "surah_name", 114)
    require_table(noor + "quran.db", "audio", 80)
    require_columns(
        noor + "quran.db", "quran",
        ("surah_id", "verse_id", "quran_arabic", "quran_withoutharkat", "page", "has_hadith", "note",
         "translation_puxta", "translation_raman", "translation_asan", "translation_sanahi"),
    )
    require_columns(
        noor + "quran.db", "surah_name",
        ("surah_no", "name_arabic", "name_english", "surah_mean_english", "ayah_number", "place", "unique_title"),
    )
    require_table(noor + "kurdish_transliteration.db", "quran", 6236)
    require_table(noor + "ayah_info.db", "page_data_line", 13000)
    require_table(noor + "ayah_info.db", "v2_tajweed_page_data_line", 12000)
    require_columns(noor + "ayah_info.db", "page_data_line",
        ("page_number", "line_number", "surah_id", "verse_id", "min_x", "max_x", "min_y", "max_y"))
    require_columns(noor + "ayah_info.db", "v2_tajweed_page_data_line",
        ("page_number", "glyph_id", "line_number", "surah_id", "verse_id", "min_x", "max_x", "min_y", "max_y"))
    require_asset(khairah + "mushaf/pages.bin")
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
    require_table(noor + "db_hadith.db", "articles", 11)
    require_table(noor + "db_hadith.db", "author", 11)
    require_table(noor + "db_hadith.db", "book", 13)
    require_table(noor + "db_hadith.db", "kitab", 427)
    require_table(noor + "db_hadith.db", "chapters", 6150)
    require_table(noor + "db_hadith_of_ayah.db", "hadith_on_ayah", 1141)
    require_columns(noor + "db_hadith.db", "hadiths",
        ("id", "sort", "book_id", "sort_in_book", "kitab_id", "chapter_id", "text", "text_ar", "text_en", "note", "footnote"))
    require_columns(noor + "db_hadith.db", "book", ("id", "name", "name_ar", "name_en", "count", "type", "sort"))
    require_columns(noor + "db_hadith.db", "kitab", ("id", "book_id", "kitab_sort", "title", "title_ar", "title_en"))
    require_columns(noor + "db_hadith.db", "chapters", ("id", "kitab_id", "chapter_sort", "title", "title_ar", "title_en"))
    require_columns(noor + "db_hadith.db", "articles",
        ("id", "description", "description_text_ar", "description_text_en", "text", "text_ar", "text_en"))
    require_columns(noor + "db_hadith_of_ayah.db", "hadith_on_ayah",
        ("id", "reference", "english", "collection", "verse_reference", "arabic", "kurdish"))

    # Full word-by-word morphology and the independent lexicon.
    require_table(noor + "db_word_by_word.db", "quran_corpus", 77429)
    require_table(noor + "db_word_by_word.db", "word_type", 45)
    require_table(noor + "db_words_lexicon.db", "word_list", 620)
    require_columns(noor + "db_word_by_word.db", "quran_corpus",
        ("surah_id", "verse_id", "position", "irab", "arabic1", "root", "word_count",
         "arabic2", "arabic3", "arabic4", "arabic5", "word_type_id1", "word_type_id2",
         "word_type_id3", "word_type_id4", "word_type_id5"))
    require_columns(noor + "db_word_by_word.db", "word_type", ("id", "tag", "english", "arabic", "kurdish", "color"))
    require_columns(noor + "db_words_lexicon.db", "word_list",
        ("id", "title", "content", "reference", "root", "reference_page", "title_no_diacritics"))

    # Prayer sources and the complete offline worship collections.
    require_table(noor + "prayertimes.db", "staticprayertimes", 28000)
    require_table(noor + "prayertimes.db", "cities", 170)
    require_table(noor + "prayertimes.db", "extra", 180)
    require_table(noor + "prayertimes.db", "tasbih", 16)
    require_table(noor + "prayertimes.db", "allah_names", 99)
    require_table(noor + "prayertimes.db", "azkar_categories", 11)
    require_table(noor + "prayertimes.db", "azkar_chapters", 133)
    require_table(noor + "prayertimes.db", "azkar_items", 287)
    require_columns(noor + "prayertimes.db", "staticprayertimes",
        ("city", "date", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"))
    require_columns(noor + "prayertimes.db", "cities", ("city", "lat", "lon"))
    require_columns(noor + "prayertimes.db", "extra", ("city", "name", "c_order", "c_base", "correction", "has_static"))
    require_columns(noor + "prayertimes.db", "allah_names",
        ("_id", "arabic", "surah_ayah", "highlight_section", "sorani", "badini", "english", "farsi", "nu"))
    require_columns(noor + "prayertimes.db", "azkar_categories", ("_id", "ckb", "ar", "en"))
    require_columns(noor + "prayertimes.db", "azkar_chapters", ("_id", "category_id", "ckb", "ar", "en"))
    require_columns(noor + "prayertimes.db", "azkar_items", ("_id", "chapter_id", "counter", "ckb", "ar", "en"))
    require_columns(noor + "prayertimes.db", "tasbih", ("id", "zikr", "count", "time", "meaning"))
    require_table(salawati + "muslim_db_v2.5.1.db", "country", 252)
    require_table(salawati + "muslim_db_v2.5.1.db", "location", 166404)
    require_table(salawati + "muslim_db_v2.5.1.db", "prayer_time", 45000)
    require_table(salawati + "muslim_db_v2.5.1.db", "azkar_category", 11)
    require_table(salawati + "muslim_db_v2.5.1.db", "azkar_chapter", 133)
    require_table(salawati + "muslim_db_v2.5.1.db", "azkar_item", 287)
    require_table(salawati + "muslim_db_v2.5.1.db", "name", 99)
    require_table(salawati + "muslim_db_v2.5.1.db", "name_translation", 594)
    require_columns(salawati + "muslim_db_v2.5.1.db", "location",
        ("_id", "country_id", "name", "latitude", "longitude", "has_fixed_prayer_time", "prayer_dependent_id"))
    require_columns(salawati + "muslim_db_v2.5.1.db", "prayer_time",
        ("_id", "location_id", "date", "fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha"))
    require_columns(salawati + "muslim_db_v2.5.1.db", "name_translation", ("_id", "name_id", "language", "name"))
    require_columns(salawati + "muslim_db_v2.5.1.db", "azkar_item_translation", ("_id", "item_id", "language", "item_translation"))

    # Additional source catalogues.
    require_table(noor + "farmuda.db", "farmuda", 240)
    require_columns(noor + "farmuda.db", "farmuda",
        ("_id", "title", "ar_desc", "ku_desc", "badini_title", "badini", "fav_time"))
    require_table(noor + "tn_books.db", "book_data", 94)
    require_columns(noor + "tn_books.db", "book_data",
        ("id", "branch", "unique_text", "title", "writer", "description", "page_number", "url", "url_cover",
         "fav_time", "sorter", "extra_text", "extra_number"))

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
            "hadith_articles_minimum": 11,
            "hadith_author_records_minimum": 11,
            "ayah_related_hadith_minimum": 1141,
            "salawati_name_translation_minimum": 594,
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
