#!/usr/bin/env python3
"""Verify that the Mashxall build contains the essential libraries from all three supplied source apps."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path

EXPECTED_SOURCES = {
    "noor-tafsir-1.2.2.apks": "Noor Tafsir",
    "khairah-1.2.apks": "Khairah",
    "salawati-3.1.3.apks": "Salawati",
}

REQUIRED_ASSETS = {
    "Quran and surah data": "content/noor_tafsir_1.2.2/base/assets/quran.db",
    "Prayer times and city data": "content/noor_tafsir_1.2.2/base/assets/prayertimes.db",
    "Hadith collection": "content/noor_tafsir_1.2.2/base/assets/db_hadith.db",
    "Hadith linked to Quran verses": "content/noor_tafsir_1.2.2/base/assets/db_hadith_of_ayah.db",
    "Word-by-word Quran analysis": "content/noor_tafsir_1.2.2/base/assets/db_word_by_word.db",
    "Kurdish transliteration": "content/noor_tafsir_1.2.2/base/assets/kurdish_transliteration.db",
    "Quran lexicon": "content/noor_tafsir_1.2.2/base/assets/db_words_lexicon.db",
    "Book catalogue": "content/noor_tafsir_1.2.2/base/assets/tn_books.db",
    "Mushaf verse geometry and tajweed metadata": "content/noor_tafsir_1.2.2/base/assets/ayah_info.db",
    "Farmuda source library": "content/noor_tafsir_1.2.2/base/assets/farmuda.db",
    "Roshn Kurdish tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseeri_roshn.db",
    "Hazhar Kurdish tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseeri_hazhar.db",
    "Muyassar tafsir from Noor": "content/noor_tafsir_1.2.2/base/assets/tafseer_maisar.db",
    "Sahih International translation/tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseer_sahih_international.db",
    "Noor tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseeri_noor.db",
    "Hawrami tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseeri_hawrami.db",
    "Runahi Badini tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseeri_runahi_badini.db",
    "Zhian Badini tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseeri_zhian_badini.db",
    "Saadi tafsir from Noor": "content/noor_tafsir_1.2.2/base/assets/tafseeri_saadi.db",
    "Mukhtasar tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseeri_mukhtasar.db",
    "Rebar Sorani tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseer_rebar.db",
    "Tawhid tafsir": "content/noor_tafsir_1.2.2/base/assets/tafseeri_tawhid.db",
    "Amiri Quran font": "content/noor_tafsir_1.2.2/base/assets/fonts/amiri_modded.ttf",
    "Khairah's original 604-page Mushaf": "content/khairah_1.2/base/assets/mushaf/pages.bin",
    "Ibn Kathir tafsir from Khairah": "content/khairah_1.2/base/assets/tafsir/ibn-kathir.sqlite",
    "Muyassar tafsir from Khairah": "content/khairah_1.2/base/assets/tafsir/muyassar.sqlite",
    "Saadi tafsir from Khairah": "content/khairah_1.2/base/assets/tafsir/saadi.sqlite",
    "Tabari tafsir from Khairah": "content/khairah_1.2/base/assets/tafsir/tabari.sqlite",
    "Salawati prayer and adhkar database": "content/salawati_3.1.3/base/assets/database/muslim_db_v2.5.1.db",
}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(4 * 1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", required=True, type=Path)
    parser.add_argument("--assets-root", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    assets_root = args.assets_root.resolve()

    try:
        catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    except Exception as exc:
        raise SystemExit(f"FAIL: cannot read asset catalogue: {exc}") from exc

    sources = catalog.get("sources", [])
    source_names = {row.get("filename") for row in sources}
    missing_sources = sorted(set(EXPECTED_SOURCES) - source_names)
    if missing_sources:
        raise SystemExit("FAIL: source bundle is missing apps: " + ", ".join(missing_sources))

    files = catalog.get("files", [])
    file_by_path = {row.get("asset_path"): row for row in files}
    missing = []
    corrupt_metadata = []
    extracted_file_failures = []
    for row in files:
        rel = Path(str(row.get("asset_path", "")))
        target = (assets_root / rel).resolve()
        if not rel.parts or rel.is_absolute() or assets_root not in target.parents:
            extracted_file_failures.append({"asset_path": str(rel), "issue": "unsafe asset path"})
            continue
        if not target.is_file():
            extracted_file_failures.append({"asset_path": str(rel), "issue": "extracted file is missing"})
            continue
        expected_size = int(row.get("size_bytes", 0))
        expected_hash = str(row.get("sha256", ""))
        actual_size = target.stat().st_size
        if actual_size != expected_size:
            extracted_file_failures.append({
                "asset_path": str(rel), "issue": "size mismatch",
                "expected_size_bytes": expected_size, "actual_size_bytes": actual_size,
            })
            continue
        actual_hash = sha256_file(target)
        if actual_hash != expected_hash:
            extracted_file_failures.append({
                "asset_path": str(rel), "issue": "SHA-256 mismatch",
                "expected_sha256": expected_hash, "actual_sha256": actual_hash,
            })

    for label, path in REQUIRED_ASSETS.items():
        row = file_by_path.get(path)
        if not row:
            missing.append({"feature": label, "asset_path": path})
        elif int(row.get("size_bytes", 0)) <= 0 or len(row.get("sha256", "")) != 64:
            corrupt_metadata.append({"feature": label, "asset_path": path})

    generated = catalog.get("generated_assets", [])
    mushaf_index = next((row for row in generated if row.get("asset_path") == "mushaf_svg_index.json"), None)
    generated_file_failures = []
    if not mushaf_index or int(mushaf_index.get("page_count", 0)) != 604:
        missing.append({"feature": "604-page SVG index", "asset_path": "mushaf_svg_index.json"})
    else:
        index_path = assets_root / "mushaf_svg_index.json"
        if not index_path.is_file():
            generated_file_failures.append({"asset_path": "mushaf_svg_index.json", "issue": "generated page index is missing"})
        elif index_path.stat().st_size != int(mushaf_index.get("size_bytes", 0)) or sha256_file(index_path) != mushaf_index.get("sha256"):
            generated_file_failures.append({"asset_path": "mushaf_svg_index.json", "issue": "generated page index size or SHA-256 mismatch"})

    databases = catalog.get("databases", [])
    audio = catalog.get("audio", [])
    fonts = catalog.get("fonts", [])
    documents = catalog.get("documents", [])
    if not databases:
        missing.append({"feature": "SQLite source databases", "asset_path": "(none)"})
    if not audio:
        missing.append({"feature": "bundled audio inventory", "asset_path": "(none)"})
    if not fonts:
        missing.append({"feature": "Quran and UI font files", "asset_path": "(none)"})

    if missing or corrupt_metadata or extracted_file_failures or generated_file_failures:
        report = {
            "status": "fail",
            "missing_required_assets": missing,
            "invalid_asset_metadata": corrupt_metadata,
            "extracted_file_failures": extracted_file_failures,
            "generated_file_failures": generated_file_failures,
        }
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
        raise SystemExit("FAIL: source coverage audit did not pass; see " + str(args.out) + "\n" + json.dumps(report, ensure_ascii=False))

    per_source = {}
    for filename, label in EXPECTED_SOURCES.items():
        source = next(row for row in sources if row.get("filename") == filename)
        sid = source.get("id", "")
        per_source[label] = {
            "source_sha256": source.get("sha256"),
            "source_size_bytes": source.get("size_bytes"),
            "split_apks": len(source.get("splits", [])),
            "assets": sum(1 for row in files if row.get("source_id") == sid),
            "databases": sum(1 for row in databases if row.get("source_id") == sid),
            "audio_files": sum(1 for row in audio if row.get("source_id") == sid),
            "pdf_documents": sum(1 for row in documents if row.get("source_id") == sid),
        }

    report = {
        "status": "pass",
        "source_apps": per_source,
        "required_feature_assets_verified": len(REQUIRED_ASSETS),
        "extracted_file_payloads_verified": len(files),
        "generated_file_payloads_verified": 1,
        "mushaf_pages": int(mushaf_index["page_count"]),
        "total_assets": len(files),
        "total_databases": len(databases),
        "total_audio_files": len(audio),
        "total_fonts": len(fonts),
        "total_pdf_documents": len(documents),
        "total_source_bytes": catalog.get("counts", {}).get("total_content_bytes"),
        "verified_required_features": list(REQUIRED_ASSETS.keys()) + ["604-page SVG index"],
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print("PASS: all three source-app libraries and their required feature assets are present.")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    sys.exit(main())
