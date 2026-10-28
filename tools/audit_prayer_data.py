#!/usr/bin/env python3
"""Audit the source prayer databases used for GPS, city and dependent-city schedules.

This is a read-only structural/data check. It never edits an original source database.
It checks coordinate bounds, time/date formats, location references, and the source
city-correction table before the database is bundled into the Android APK.
"""
from __future__ import annotations

import argparse
import json
import re
import sqlite3
from pathlib import Path
from typing import Any

CLOCK = re.compile(r"^(?:[01]\d|2[0-3]):[0-5]\d$")
MONTH_DAY = re.compile(r"^(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\d|3[01])$")

PRAYERS = ("fajr", "sunrise", "dhuhr", "asr", "maghrib", "isha")


def open_readonly(path: Path) -> sqlite3.Connection:
    if not path.is_file():
        raise FileNotFoundError(f"Required prayer database is missing: {path}")
    uri = path.resolve().as_uri() + "?mode=ro"
    con = sqlite3.connect(uri, uri=True, timeout=15)
    con.row_factory = sqlite3.Row
    return con


def table_names(con: sqlite3.Connection) -> set[str]:
    return {
        row[0]
        for row in con.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"
        )
    }


def columns(con: sqlite3.Connection, table: str) -> set[str]:
    return {row["name"] for row in con.execute(f'PRAGMA table_info("{table}")')}


def require_columns(
    checks: list[dict[str, Any]],
    con: sqlite3.Connection,
    table: str,
    required: set[str],
    prefix: str,
) -> bool:
    existing_tables = table_names(con)
    present = table in existing_tables
    actual = columns(con, table) if present else set()
    missing = sorted(required - actual)
    checks.append({
        "name": f"{prefix}.{table}.required_columns",
        "passed": present and not missing,
        "actual": sorted(actual),
        "expected": sorted(required),
        **({"missing": missing} if missing else {}),
    })
    return present and not missing


def check(
    checks: list[dict[str, Any]],
    name: str,
    passed: bool,
    actual: Any,
    expected: Any,
    detail: str = "",
) -> None:
    checks.append({
        "name": name,
        "passed": bool(passed),
        "actual": actual,
        "expected": expected,
        **({"detail": detail} if detail else {}),
    })


def clock_stats(con: sqlite3.Connection, table: str, date_column: str, location_column: str) -> dict[str, Any]:
    required = set(PRAYERS)
    available = columns(con, table)
    if not required <= available:
        return {"rows": 0, "complete_rows": 0, "invalid_times": {}, "invalid_dates": None}
    rows = con.execute(
        f'SELECT "{date_column}",{",".join(chr(34) + p + chr(34) for p in PRAYERS)} FROM "{table}"'
    )
    total = 0
    complete_rows = 0
    invalid_times = {name: 0 for name in PRAYERS}
    invalid_dates = 0
    for row in rows:
        total += 1
        date_value = str(row[0] or "").strip()
        if not MONTH_DAY.fullmatch(date_value):
            invalid_dates += 1
        all_valid = True
        for index, prayer in enumerate(PRAYERS, start=1):
            value = str(row[index] or "").strip()
            if not CLOCK.fullmatch(value):
                invalid_times[prayer] += 1
                all_valid = False
        if all_valid and MONTH_DAY.fullmatch(date_value):
            complete_rows += 1
    return {
        "rows": total,
        "complete_rows": complete_rows,
        "invalid_times": invalid_times,
        "invalid_dates": invalid_dates,
    }


def audit(noor_path: Path, salawati_path: Path) -> dict[str, Any]:
    checks: list[dict[str, Any]] = []
    warnings: list[dict[str, Any]] = []
    stats: dict[str, Any] = {}

    with open_readonly(noor_path) as noor, open_readonly(salawati_path) as salawati:
        noor_integrity = noor.execute("PRAGMA integrity_check").fetchone()[0]
        salawati_integrity = salawati.execute("PRAGMA integrity_check").fetchone()[0]
        check(checks, "noor.sqlite_integrity", noor_integrity == "ok", noor_integrity, "ok")
        check(checks, "salawati.sqlite_integrity", salawati_integrity == "ok", salawati_integrity, "ok")

        noor_requirements = {
            "cities": {"city", "lat", "lon"},
            "staticprayertimes": {"city", "date", *PRAYERS},
            "extra": {"city", "name", "c_base", "correction", "c_order"},
        }
        salawati_requirements = {
            "country": {"_id", "name"},
            "location": {
                "_id", "name", "country_id", "latitude", "longitude",
                "has_fixed_prayer_time", "prayer_dependent_id",
            },
            "prayer_time": {"location_id", "date", *PRAYERS},
        }
        noor_ok = all(
            require_columns(checks, noor, table, cols, "noor")
            for table, cols in noor_requirements.items()
        )
        salawati_ok = all(
            require_columns(checks, salawati, table, cols, "salawati")
            for table, cols in salawati_requirements.items()
        )

        if noor_ok:
            city_count = noor.execute("SELECT COUNT(*) FROM cities").fetchone()[0]
            static_count = noor.execute("SELECT COUNT(*) FROM staticprayertimes").fetchone()[0]
            correction_count = noor.execute("SELECT COUNT(*) FROM extra").fetchone()[0]
            noor_clock = clock_stats(noor, "staticprayertimes", "date", "city")
            coords = noor.execute("""
                SELECT
                    SUM(CASE WHEN lat IS NULL OR lon IS NULL THEN 1 ELSE 0 END),
                    SUM(CASE WHEN lat NOT BETWEEN -90 AND 90 OR lon NOT BETWEEN -180 AND 180 THEN 1 ELSE 0 END)
                FROM cities
            """).fetchone()
            duplicate_timetable_keys = noor.execute("""
                SELECT COUNT(*) FROM (
                    SELECT lower(city),date FROM staticprayertimes
                    GROUP BY lower(city),date HAVING COUNT(*) > 1
                )
            """).fetchone()[0]
            stats["noor"] = {
                "cities": city_count,
                "static_prayer_rows": static_count,
                "city_correction_rows": correction_count,
                "timetable_quality": noor_clock,
                "cities_missing_coordinates": coords[0] or 0,
                "cities_with_out_of_range_coordinates": coords[1] or 0,
                "duplicate_city_date_keys": duplicate_timetable_keys,
            }
            check(checks, "noor.city_count", city_count >= 170, city_count, ">=170")
            check(checks, "noor.static_timetable_coverage", static_count >= 28000, static_count, ">=28000 rows")
            check(checks, "noor.city_correction_records", correction_count >= 180, correction_count, ">=180")
            check(
                checks, "noor.city_coordinate_bounds",
                (coords[1] or 0) == 0, coords[1] or 0, 0,
            )
            check(
                checks, "noor.timetable_rows_have_valid_dates_and_clock_values",
                noor_clock["rows"] == 0 or noor_clock["complete_rows"] / noor_clock["rows"] >= 0.98,
                noor_clock["complete_rows"], f">=98% of {noor_clock['rows']} rows",
                "Rows with source-provided empty/invalid clock values are counted per field in the report.",
            )
            if duplicate_timetable_keys:
                warnings.append({
                    "name": "noor.duplicate_city_date_keys",
                    "count": duplicate_timetable_keys,
                    "detail": "These keys may be harmless source aliases, but direct timetable lookup selects the first match.",
                })

        if salawati_ok:
            location_count = salawati.execute("SELECT COUNT(*) FROM location").fetchone()[0]
            country_count = salawati.execute("SELECT COUNT(*) FROM country").fetchone()[0]
            prayer_count = salawati.execute("SELECT COUNT(*) FROM prayer_time").fetchone()[0]
            dependent_count = salawati.execute("""
                SELECT COUNT(*) FROM location
                WHERE COALESCE(prayer_dependent_id,0) > 0
            """).fetchone()[0]
            orphan_time_rows = salawati.execute("""
                SELECT COUNT(*) FROM prayer_time pt
                LEFT JOIN location l ON l._id=pt.location_id
                WHERE l._id IS NULL
            """).fetchone()[0]
            dangling_dependent_rows = salawati.execute("""
                SELECT COUNT(*) FROM location dep
                LEFT JOIN location base ON base._id=dep.prayer_dependent_id
                WHERE COALESCE(dep.prayer_dependent_id,0) > 0 AND base._id IS NULL
            """).fetchone()[0]
            duplicate_time_keys = salawati.execute("""
                SELECT COUNT(*) FROM (
                    SELECT location_id,date FROM prayer_time
                    GROUP BY location_id,date HAVING COUNT(*) > 1
                )
            """).fetchone()[0]
            coordinate_rows = salawati.execute("""
                SELECT
                    COUNT(*) AS total,
                    SUM(CASE WHEN latitude IS NULL OR longitude IS NULL THEN 1 ELSE 0 END) AS missing,
                    SUM(CASE WHEN latitude NOT BETWEEN -90 AND 90 OR longitude NOT BETWEEN -180 AND 180 THEN 1 ELSE 0 END) AS invalid
                FROM location
            """).fetchone()
            country_ref_errors = salawati.execute("""
                SELECT COUNT(*) FROM location l
                LEFT JOIN country c ON c._id=l.country_id
                WHERE l.country_id IS NOT NULL AND c._id IS NULL
            """).fetchone()[0]
            salawati_clock = clock_stats(salawati, "prayer_time", "date", "location_id")
            stats["salawati"] = {
                "countries": country_count,
                "locations": location_count,
                "prayer_rows": prayer_count,
                "dependent_locations": dependent_count,
                "orphan_prayer_rows": orphan_time_rows,
                "dangling_dependent_locations": dangling_dependent_rows,
                "location_rows_missing_coordinates": coordinate_rows["missing"] or 0,
                "location_rows_with_out_of_range_coordinates": coordinate_rows["invalid"] or 0,
                "dangling_country_references": country_ref_errors,
                "duplicate_location_date_keys": duplicate_time_keys,
                "timetable_quality": salawati_clock,
            }
            check(checks, "salawati.country_count", country_count >= 250, country_count, ">=250")
            check(checks, "salawati.location_count", location_count >= 160000, location_count, ">=160000")
            check(checks, "salawati.prayer_timetable_rows", prayer_count >= 45000, prayer_count, ">=45000")
            check(checks, "salawati.prayer_rows_reference_existing_locations", orphan_time_rows == 0, orphan_time_rows, 0)
            check(checks, "salawati.dependent_location_references", dangling_dependent_rows == 0, dangling_dependent_rows, 0)
            check(checks, "salawati.location_coordinate_bounds", (coordinate_rows["invalid"] or 0) == 0, coordinate_rows["invalid"] or 0, 0)
            check(checks, "salawati.timetable_rows_have_valid_dates_and_clock_values",
                  salawati_clock["rows"] == 0 or salawati_clock["complete_rows"] / salawati_clock["rows"] >= 0.98,
                  salawati_clock["complete_rows"], f">=98% of {salawati_clock['rows']} rows")
            if country_ref_errors:
                warnings.append({
                    "name": "salawati.dangling_country_references",
                    "count": country_ref_errors,
                    "detail": "Some source locations refer to a country ID absent from the country table.",
                })
            if duplicate_time_keys:
                warnings.append({
                    "name": "salawati.duplicate_location_date_keys",
                    "count": duplicate_time_keys,
                    "detail": "Duplicates may cause multiple source timetable matches for the same location and date.",
                })

    failures = [entry for entry in checks if not entry["passed"]]
    return {
        "schema_version": 1,
        "status": "pass" if not failures else "fail",
        "passed_checks": len(checks) - len(failures),
        "failed_checks": len(failures),
        "checks": checks,
        "statistics": stats,
        "warnings": warnings,
        "source_paths": {"noor": str(noor_path), "salawati": str(salawati_path)},
        "notes": [
            "All source SQLite connections are opened read-only.",
            "GPS timetable matching uses bounding-box candidates followed by haversine distance ordering.",
            "Dependent Salawati locations use the source-linked base location when the original database marks them as dependent.",
            "Prayer-time resolution returns unavailable rather than calculating replacement times when no valid nearby Salawati timetable exists.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--noor", required=True, type=Path)
    parser.add_argument("--salawati", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    report = audit(args.noor, args.salawati)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "status": report["status"],
        "passed_checks": report["passed_checks"],
        "failed_checks": report["failed_checks"],
        "statistics": report["statistics"],
        "warnings": report["warnings"],
        "failures": [x for x in report["checks"] if not x["passed"]],
        "report": str(args.out),
    }, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
