#!/usr/bin/env python3
"""Verify every catalogued source asset is present, full-size and readable in the built APK."""
from __future__ import annotations

import argparse
import hashlib
import json
import sqlite3
import zipfile
from pathlib import Path
from typing import Any

SQLITE_HEADER = b"SQLite format 3\x00"
REQUIRED_SQLITE = (
    "content/noor_tafsir_1.2.2/base/assets/quran.db",
    "content/noor_tafsir_1.2.2/base/assets/prayertimes.db",
    "content/noor_tafsir_1.2.2/base/assets/db_hadith.db",
    "content/noor_tafsir_1.2.2/base/assets/db_hadith_of_ayah.db",
    "content/noor_tafsir_1.2.2/base/assets/db_word_by_word.db",
    "content/noor_tafsir_1.2.2/base/assets/db_words_lexicon.db",
    "content/noor_tafsir_1.2.2/base/assets/kurdish_transliteration.db",
    "content/noor_tafsir_1.2.2/base/assets/ayah_info.db",
    "content/noor_tafsir_1.2.2/base/assets/farmuda.db",
    "content/noor_tafsir_1.2.2/base/assets/tn_books.db",
    "content/salawati_3.1.3/base/assets/database/muslim_db_v2.5.1.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseeri_roshn.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseeri_hazhar.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseer_maisar.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseer_sahih_international.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseeri_noor.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseeri_hawrami.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseeri_runahi_badini.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseeri_zhian_badini.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseeri_saadi.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseeri_mukhtasar.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseer_rebar.db",
    "content/noor_tafsir_1.2.2/base/assets/tafseeri_tawhid.db",
    "content/khairah_1.2/base/assets/tafsir/ibn-kathir.sqlite",
    "content/khairah_1.2/base/assets/tafsir/muyassar.sqlite",
    "content/khairah_1.2/base/assets/tafsir/saadi.sqlite",
    "content/khairah_1.2/base/assets/tafsir/tabari.sqlite",
)
REQUIRED_OTHER = (
    "content/khairah_1.2/base/assets/mushaf/pages.bin",
    "mushaf_svg_index.json",
    "assets/content_catalog.json",
)


def _catalog_assets(catalog: dict[str, Any]) -> dict[str, dict[str, Any]]:
    declared: dict[str, dict[str, Any]] = {}
    for section in ("files", "generated_assets"):
        for item in catalog.get(section, []):
            if not isinstance(item, dict):
                continue
            path = item.get("asset_path")
            if isinstance(path, str) and path:
                declared[path] = {
                    "size_bytes": item.get("size_bytes"),
                    "sha256": item.get("sha256"),
                }
    return declared


def audit(apk_path: Path, catalog_path: Path) -> dict[str, Any]:
    if not apk_path.is_file() or apk_path.stat().st_size == 0:
        raise FileNotFoundError(f"APK is missing or empty: {apk_path}")
    if not catalog_path.is_file():
        raise FileNotFoundError(f"Content catalog is missing: {catalog_path}")

    catalog = json.loads(catalog_path.read_text(encoding="utf-8"))
    declared = _catalog_assets(catalog)
    problems: list[dict[str, Any]] = []
    required_not_declared = sorted(set(REQUIRED_SQLITE + REQUIRED_OTHER) - set(declared) - {"assets/content_catalog.json"})
    for path in required_not_declared:
        problems.append({"kind": "required_asset_not_in_catalog", "asset_path": path})

    total_entries = 0
    matched_entries = 0
    hash_verified_entries = 0
    size_mismatches: list[dict[str, Any]] = []
    hash_mismatches: list[dict[str, Any]] = []
    missing: list[str] = []
    invalid_size_catalog: list[str] = []
    invalid_hash_catalog: list[str] = []
    asset_headers: dict[str, bytes] = {}
    sqlite_checks: list[dict[str, Any]] = []
    stored_asset_checks: list[dict[str, Any]] = []

    with zipfile.ZipFile(apk_path, "r") as archive:
        bad_member = archive.testzip()
        if bad_member:
            problems.append({"kind": "apk_zip_crc_failure", "asset_path": bad_member})

        info_by_name = {info.filename: info for info in archive.infolist()}
        total_entries = len(info_by_name)
        for asset_path, metadata in sorted(declared.items()):
            apk_name = "assets/" + asset_path
            info = info_by_name.get(apk_name)
            if info is None:
                missing.append(apk_name)
                problems.append({"kind": "missing_packaged_asset", "asset_path": asset_path})
                continue

            expected_size = metadata.get("size_bytes")
            if not isinstance(expected_size, int) or expected_size < 0:
                invalid_size_catalog.append(asset_path)
                problems.append({"kind": "asset_size_missing_from_catalog", "asset_path": asset_path})
            elif info.file_size != expected_size:
                item = {
                    "asset_path": asset_path,
                    "expected_uncompressed_bytes": expected_size,
                    "apk_uncompressed_bytes": info.file_size,
                }
                size_mismatches.append(item)
                problems.append({"kind": "asset_size_mismatch", **item})

            expected_hash = metadata.get("sha256")
            if not isinstance(expected_hash, str) or len(expected_hash) != 64:
                invalid_hash_catalog.append(asset_path)
                problems.append({"kind": "asset_sha256_missing_from_catalog", "asset_path": asset_path})

            try:
                digest = hashlib.sha256()
                with archive.open(info, "r") as stream:
                    header = stream.read(len(SQLITE_HEADER))
                    asset_headers[asset_path] = header
                    digest.update(header)
                    for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                        digest.update(chunk)
                actual_hash = digest.hexdigest()
            except (OSError, zipfile.BadZipFile, RuntimeError) as exc:
                problems.append({"kind": "asset_hash_read_failed", "asset_path": asset_path, "error": str(exc)})
                continue

            hash_matches = isinstance(expected_hash, str) and len(expected_hash) == 64 and actual_hash == expected_hash.lower()
            if isinstance(expected_hash, str) and len(expected_hash) == 64:
                if not hash_matches:
                    mismatch = {
                        "asset_path": asset_path,
                        "expected_sha256": expected_hash.lower(),
                        "apk_sha256": actual_hash,
                    }
                    hash_mismatches.append(mismatch)
                    problems.append({"kind": "asset_sha256_mismatch", **mismatch})
                else:
                    hash_verified_entries += 1

            if (
                isinstance(expected_size, int) and expected_size >= 0
                and info.file_size == expected_size
                and hash_matches
            ):
                matched_entries += 1

        for asset_path in REQUIRED_SQLITE:
            apk_name = "assets/" + asset_path
            info = info_by_name.get(apk_name)
            if info is None:
                continue
            try:
                header = asset_headers.get(asset_path, b"")
                passed = header == SQLITE_HEADER
                check = {"asset_path": asset_path, "passed": passed, "apk_bytes": info.file_size}
                sqlite_checks.append(check)
                if not passed:
                    problems.append({"kind": "invalid_sqlite_header", **check})
            except (OSError, zipfile.BadZipFile, RuntimeError) as exc:
                problems.append({"kind": "sqlite_header_read_failed", "asset_path": asset_path, "error": str(exc)})

        for asset_path in (
            "content/khairah_1.2/base/assets/mushaf/pages.bin",
        ):
            info = info_by_name.get("assets/" + asset_path)
            if info is not None:
                passed = info.compress_type == zipfile.ZIP_STORED
                check = {
                    "asset_path": asset_path,
                    "passed": passed,
                    "zip_compression": "stored" if passed else info.compress_type,
                }
                stored_asset_checks.append(check)
                if not passed:
                    problems.append({"kind": "random_access_asset_is_compressed", **check})

        catalog_info = info_by_name.get("assets/content_catalog.json")
        if catalog_info is None:
            problems.append({"kind": "missing_packaged_catalog", "asset_path": "assets/content_catalog.json"})
        else:
            try:
                embedded_catalog = json.loads(archive.read(catalog_info).decode("utf-8"))
                if embedded_catalog.get("bundle_sha256") != catalog.get("bundle_sha256"):
                    problems.append({"kind": "embedded_catalog_differs_from_build_catalog"})
            except (UnicodeDecodeError, json.JSONDecodeError, OSError, zipfile.BadZipFile) as exc:
                problems.append({"kind": "embedded_catalog_unreadable", "error": str(exc)})

        index_info = info_by_name.get("assets/mushaf_svg_index.json")
        if index_info is None:
            problems.append({"kind": "missing_mushaf_page_index", "asset_path": "mushaf_svg_index.json"})
        else:
            try:
                index = json.loads(archive.read(index_info).decode("utf-8"))
                pages = index.get("pages", [])
                page_count = index.get("page_count")
                passed = page_count == 604 and len(pages) == 604
                if not passed:
                    problems.append({
                        "kind": "invalid_mushaf_page_index",
                        "page_count": page_count,
                        "indexed_pages": len(pages),
                        "expected": 604,
                    })
            except (UnicodeDecodeError, json.JSONDecodeError, OSError, zipfile.BadZipFile) as exc:
                problems.append({"kind": "mushaf_index_unreadable", "error": str(exc)})

    return {
        "schema_version": 1,
        "ok": not problems,
        "apk": apk_path.name,
        "apk_size_bytes": apk_path.stat().st_size,
        "catalog_bundle_sha256": catalog.get("bundle_sha256"),
        "catalog_counts": catalog.get("counts", {}),
        "catalog_declared_asset_count": len(declared),
        "apk_zip_entry_count": total_entries,
        "assets_size_and_sha256_verified_count": matched_entries,
        "asset_sha256_verified_count": hash_verified_entries,
        "missing_assets": missing,
        "size_mismatches": size_mismatches,
        "hash_mismatches": hash_mismatches,
        "catalog_items_without_sizes": invalid_size_catalog,
        "catalog_items_without_hashes": invalid_hash_catalog,
        "required_sqlite_header_checks": sqlite_checks,
        "random_access_storage_checks": stored_asset_checks,
        "problem_count": len(problems),
        "problems": problems,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", required=True, type=Path)
    parser.add_argument("--catalog", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    report = audit(args.apk, args.catalog)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "status": "pass" if report["ok"] else "fail",
        "apk": report["apk"],
        "apk_size_bytes": report["apk_size_bytes"],
        "catalog_declared_asset_count": report["catalog_declared_asset_count"],
        "apk_zip_entry_count": report["apk_zip_entry_count"],
        "assets_size_and_sha256_verified_count": report["assets_size_and_sha256_verified_count"],
        "asset_sha256_verified_count": report["asset_sha256_verified_count"],
        "missing_asset_count": len(report["missing_assets"]),
        "size_mismatch_count": len(report["size_mismatches"]),
        "hash_mismatch_count": len(report["hash_mismatches"]),
        "problem_count": report["problem_count"],
        "report": str(args.out),
    }, ensure_ascii=False, indent=2))
    return 0 if report["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
