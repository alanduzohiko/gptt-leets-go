#!/usr/bin/env python3
"""Fail builds when Kotlin points at content assets absent from the extracted APKs."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

CONTENT_LITERAL = re.compile(r'"(content/[^"\\]+)"')
TAFSIR_DB = re.compile(r'^(content/khairah_1\.2/.*/tafsir/[^/]+)\.sqlite$')
QURAN_FONT_OPTION = re.compile(
    r'QuranTextFontOption\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*,\s*(\d+)L\s*,\s*"([0-9a-f]{64})"\s*,'
)



def audit(catalog: dict, sources: list[Path]) -> dict:
    available = {
        item.get("asset_path")
        for item in catalog.get("files", [])
        if isinstance(item, dict) and item.get("asset_path")
    }
    available.update(
        item.get("asset_path")
        for item in catalog.get("generated_assets", [])
        if isinstance(item, dict) and item.get("asset_path")
    )
    referenced: dict[str, set[str]] = {}
    source_text: dict[str, str] = {}
    for source in sources:
        text = source.read_text(encoding="utf-8", errors="replace")
        source_text[source.name] = source_text.get(source.name, "") + "\n" + text
        for match in CONTENT_LITERAL.finditer(text):
            referenced.setdefault(match.group(1), set()).add(str(source))

    missing: list[dict] = []
    # App-specific contract checks run in the real unified source tree. Small unit-test
    # fixtures that contain only a sample Kotlin file should test literal path behavior alone.
    has_prayer_sources = {"ContentRepository.kt", "MainActivity.kt"}.issubset(source_text)
    if has_prayer_sources:
        repository_text = source_text.get("ContentRepository.kt", "")
        activity_text = source_text.get("MainActivity.kt", "")
        runtime_prayer_contract = {
            "salawati_city_timetable_loader": "loadSalawatiPrayerTimesForCity" in repository_text,
            "salawati_gps_timetable_lookup": "loadNearbyTimetableRows" in repository_text and "SALAWATI_DB" in repository_text,
            "rejects_non_salawati_rows": "salawatiOnlyPrayerTimes" in repository_text,
            "no_noor_prayer_correction_loader": "loadNoorPrayerTimesWithCorrection" not in repository_text,
            "no_noor_correction_result": "noor-correction:" not in repository_text,
            "no_noor_source_label": 'normalized.startsWith("noor")' not in activity_text,
            "no_astronomical_replacement_source": "calculated from coordinates" not in activity_text.lower(),
        }
        failed_prayer_contract = [name for name, passed in runtime_prayer_contract.items() if not passed]
        if failed_prayer_contract:
            missing.append({
                "path": "SALAWATI_ONLY_PRAYER_SOURCE_CONTRACT",
                "sources": [str(source) for source in sources if source.name in {"ContentRepository.kt", "MainActivity.kt"}],
                "failed_checks": failed_prayer_contract,
            })
    else:
        runtime_prayer_contract = {"applicable": False, "reason": "unified app source files are not present in this fixture"}

    # Verify each font is an original extracted asset with matching size and SHA-256.
    has_app_screens = "AppScreens.kt" in source_text
    font_matches = list(QURAN_FONT_OPTION.finditer(source_text.get("AppScreens.kt", ""))) if has_app_screens else []
    font_issues: list[dict] = []
    seen_font_ids: set[str] = set()
    seen_font_paths: set[str] = set()
    catalog_by_path = {
        item.get("asset_path"): item
        for item in catalog.get("files", [])
        if isinstance(item, dict) and item.get("asset_path")
    }
    for match in font_matches:
        font_id, asset_path, size_text, source_hash = match.groups()
        declared_size = int(size_text)
        row = catalog_by_path.get(asset_path)
        if font_id in seen_font_ids:
            font_issues.append({"id": font_id, "asset_path": asset_path, "issue": "duplicate font id"})
        if asset_path in seen_font_paths:
            font_issues.append({"id": font_id, "asset_path": asset_path, "issue": "duplicate font asset path"})
        seen_font_ids.add(font_id)
        seen_font_paths.add(asset_path)
        if row is None:
            font_issues.append({"id": font_id, "asset_path": asset_path, "issue": "not present in extracted source catalogue"})
            continue
        if declared_size != int(row.get("size_bytes", 0)):
            font_issues.append({
                "id": font_id, "asset_path": asset_path, "issue": "size differs from source catalogue",
                "declared_size_bytes": declared_size, "source_size_bytes": row.get("size_bytes"),
            })
        if source_hash != str(row.get("sha256", "")):
            font_issues.append({
                "id": font_id, "asset_path": asset_path, "issue": "SHA-256 differs from source catalogue",
                "declared_sha256": source_hash, "source_sha256": row.get("sha256"),
            })
    quran_font_contract = {
        "applicable": has_app_screens,
        "option_count": len(font_matches),
        "unique_ids": len(seen_font_ids),
        "verified_source_fonts": len(font_matches) - len(font_issues),
        "issues": font_issues,
    }
    if has_app_screens and (len(font_matches) != 6 or len(seen_font_ids) != len(font_matches) or font_issues):
        missing.append({
            "path": "QURAN_FONT_SOURCE_INTEGRITY_CONTRACT",
            "sources": [str(source) for source in sources if source.name == "AppScreens.kt"],
            **quran_font_contract,
        })

    # Quran reading contract: tapping a verse in text mode must open the same
    # verse-action panel used by the printed Mushaf; never make this search-only.
    app_screens_text = source_text.get("AppScreens.kt", "")
    quran_reader_contract = {
        "text_verse_card_opens_action_panel": (
            re.search(
                r"modifier\s*=\s*Modifier(?:\s*\.\s*staggeredEntrance\s*\([^)]*\))?\s*\.\s*(?:premiumClickable|clickable)\s*\{\s*selected\s*=\s*verse\.surah",
                app_screens_text,
            ) is not None
            and "selectedAyahForSheet = verse.surah to verse.number" in app_screens_text
            and "modifier = if (query.isNotBlank()) Modifier.clickable" not in app_screens_text
        ),
        "chosen_quran_font_is_applied_in_verse_panel": (
            "fontFamily = quranTextFontFamily" in app_screens_text
            and "text = sheetVerse!!.arabic" in app_screens_text
        ),
    }
    failed_quran_reader_contract = [
        name for name, passed in quran_reader_contract.items() if not passed
    ]
    if has_app_screens and failed_quran_reader_contract:
        missing.append({
            "path": "QURAN_READER_INTERACTION_CONTRACT",
            "sources": [str(source) for source in sources if source.name == "AppScreens.kt"],
            "failed_checks": failed_quran_reader_contract,
        })

    for path, owners in sorted(referenced.items()):
        if path not in available:
            missing.append({"path": path, "sources": sorted(owners)})
        # Khairah tafsir loader computes the companion .bin path from the
        # .sqlite database path, so check it even when the DB itself exists.
        derived = TAFSIR_DB.match(path)
        if derived:
            companion = derived.group(1) + ".bin"
            if companion not in available:
                missing.append({
                    "path": companion,
                    "sources": sorted(owners),
                    "derived_from": path,
                })

    if "mushaf_svg_index.json" not in available:
        missing.append({
            "path": "mushaf_svg_index.json",
            "sources": ["tools/extract_unified_content.py"],
        })
    return {
        "ok": not missing,
        "catalog_counts": catalog.get("counts", {}),
        "asset_count": len(available),
        "source_file_count": len(sources),
        "literal_reference_count": len(referenced),
        "missing_references": missing,
        "runtime_prayer_source_contract": runtime_prayer_contract,
        "quran_font_source_integrity_contract": quran_font_contract,
        "quran_reader_interaction_contract": quran_reader_contract if has_app_screens else {"applicable": False},
        "checked_sources": [str(source) for source in sources],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", required=True, type=Path)
    parser.add_argument(
        "--source-root",
        type=Path,
        default=Path("unified-app/app/src/main/java/com/mashxall/unified"),
    )
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    if not args.catalog.is_file():
        parser.error(f"catalog does not exist: {args.catalog}")
    if not args.source_root.is_dir():
        parser.error(f"source root does not exist: {args.source_root}")
    catalog = json.loads(args.catalog.read_text(encoding="utf-8"))
    sources = sorted(args.source_root.rglob("*.kt"))
    if not sources:
        parser.error(f"no Kotlin source files found under {args.source_root}")
    report = audit(catalog, sources)
    rendered = json.dumps(report, ensure_ascii=False, indent=2)
    print(rendered)
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(rendered + "\n", encoding="utf-8")
    return 0 if report["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
