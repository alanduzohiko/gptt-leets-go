#!/usr/bin/env python3
"""Fail builds when Kotlin points at content assets absent from the extracted APKs."""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

CONTENT_LITERAL = re.compile(r'"(content/[^"\\]+)"')
TAFSIR_DB = re.compile(r'^(content/khairah_1\.2/.*/tafsir/[^/]+)\.sqlite$')


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
        source_text[source.name] = source_text.get(source.name, "") + "\\n" + text
        for match in CONTENT_LITERAL.finditer(text):
            referenced.setdefault(match.group(1), set()).add(str(source))

    missing: list[dict] = []
    # Prayer schedule resolution is strictly Salawati-only; Noor's other worship content remains available.
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
