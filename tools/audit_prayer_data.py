#!/usr/bin/env python3
"""Read-only quality audit for the single supported prayer-time source: Salawati.

Noor is still used for its separate Quran/worship datasets; this audit deliberately
does not treat Noor's static prayer rows or correction table as timing fallbacks.
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
        raise FileNotFoundError(f"Required Salawati database is missing: {path}")
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


def record(
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


def require_columns(
    checks: list[dict[str, Any]],
    con: sqlite3.Connection,
    table: str,
    required: set[str],
) -> bool:
    exists = table in table_names(con)
    actual = columns(con, table) if exists else set()
    missing = sorted(required - actual)
    record(
        checks,
        f"salawati.{table}.required_columns",
        exists and not missing,
        sorted(actual),
        sorted(required),
        "Missing columns: " + ", ".join(missing) if missing else "",
    )
    return exists and not missing


def clock_stats(con: sqlite3.Connection) -> dict[str, Any]:
    available = columns(con, "prayer_time")
    if not set(PRAYERS + ("date", "location_id")) <= available:
        return {"rows": 0, "complete_rows": 0, "invalid_times": {}, "invalid_dates": None}
    total = 0
    complete = 0
    invalid_times = {name: 0 for name in PRAYERS}
    invalid_dates = 0
    select = "SELECT date," + ",".join(f'"{name}"' for name in PRAYERS) + ' FROM "prayer_time"'
    for row in con.execute(select):
        total += 1
        date_ok = bool(MONTH_DAY.fullmatch(str(row[0] or "").strip()))
        if not date_ok:
            invalid_dates += 1
        all_times_valid = True
        for idx, name in enumerate(PRAYERS, start=1):
            if not CLOCK.fullmatch(str(row[idx] or "").strip()):
                invalid_times[name] += 1
                all_times_valid = False
        if date_ok and all_times_valid:
            complete += 1
    return {
        "rows": total,
        "complete_rows": complete,
        "invalid_times": invalid_times,
        "invalid_dates": invalid_dates,
    }



def location_schedule_diagnostics(con: sqlite3.Connection, month_day: str) -> dict[str, Any]:
    """Expose real source coverage around named Iraqi cities and GPS timetable providers."""
    totals = con.execute("""
        SELECT
            COUNT(DISTINCT location_id) AS scheduled_locations,
            COUNT(DISTINCT date) AS distinct_dates,
            MIN(date) AS first_date,
            MAX(date) AS last_date
        FROM prayer_time
    """).fetchone()

    patterns = [
        "sulaymaniyah", "sulaimaniyah", "sulaimaniya", "slemani", "slemany",
        "سلێمانی", "السليمانية", "السلیمانیة",
        "erbil", "hawler", "hawlêr", "هەولێر", "اربيل", "أربيل", "أربیل",
        "duhok", "dohuk", "دهوك", "دهۆک",
        "kirkuk", "كركوك", "کەرکووک",
    ]
    where = " OR ".join(["lower(l.name) LIKE ?" for _ in patterns])
    args = [month_day] + [f"%{pattern.lower()}%" for pattern in patterns]
    city_rows = con.execute(f"""
        SELECT l.name AS city,
               COALESCE(c.name,'') AS country,
               l.latitude AS latitude,
               l.longitude AS longitude,
               COALESCE(l.has_fixed_prayer_time,0) AS has_fixed,
               COALESCE(l.prayer_dependent_id,0) AS dependent_id,
               COALESCE(base.name,'') AS dependent_base,
               COUNT(pt.date) AS scheduled_days,
               MIN(pt.date) AS first_date,
               MAX(pt.date) AS last_date,
               SUM(CASE WHEN pt.date=? THEN 1 ELSE 0 END) AS has_requested_date
        FROM location l
        LEFT JOIN country c ON c._id=l.country_id
        LEFT JOIN location base ON base._id=l.prayer_dependent_id
        LEFT JOIN prayer_time pt
          ON pt.location_id = CASE
              WHEN COALESCE(l.has_fixed_prayer_time,0)=1 THEN l._id
              WHEN COALESCE(l.prayer_dependent_id,0)>0 THEN l.prayer_dependent_id
              ELSE l._id
          END
        WHERE {where}
        GROUP BY l._id
        ORDER BY
            CASE WHEN lower(COALESCE(c.name,'')) LIKE '%iraq%' OR lower(COALESCE(c.name,'')) LIKE '%العراق%' THEN 0 ELSE 1 END,
            scheduled_days DESC, l.name
        LIMIT 80
    """, args).fetchall()

    providers = con.execute("""
        SELECT l.name AS city,
               COALESCE(c.name,'') AS country,
               l.latitude AS latitude,
               l.longitude AS longitude,
               COALESCE(l.has_fixed_prayer_time,0) AS has_fixed,
               COALESCE(l.prayer_dependent_id,0) AS dependent_id,
               COALESCE(base.name,'') AS dependent_base,
               COUNT(pt.date) AS scheduled_days,
               MIN(pt.date) AS first_date,
               MAX(pt.date) AS last_date
        FROM location l
        LEFT JOIN country c ON c._id=l.country_id
        LEFT JOIN location base ON base._id=l.prayer_dependent_id
        JOIN prayer_time pt
          ON pt.location_id = CASE
              WHEN COALESCE(l.has_fixed_prayer_time,0)=1 THEN l._id
              WHEN COALESCE(l.prayer_dependent_id,0)>0 THEN l.prayer_dependent_id
              ELSE l._id
          END
        WHERE lower(COALESCE(c.name,'')) LIKE '%iraq%'
           OR lower(COALESCE(c.name,'')) LIKE '%العراق%'
        GROUP BY l._id
        ORDER BY scheduled_days DESC, l.name
        LIMIT 40
    """).fetchall()

    def row_dict(row: sqlite3.Row) -> dict[str, Any]:
        result = {
            "city": row["city"],
            "country": row["country"],
            "latitude": row["latitude"],
            "longitude": row["longitude"],
            "has_fixed_prayer_time": bool(row["has_fixed"]),
            "prayer_dependent_id": row["dependent_id"],
            "dependent_base_city": row["dependent_base"],
            "scheduled_days": row["scheduled_days"],
            "first_date": row["first_date"],
            "last_date": row["last_date"],
        }
        if "has_requested_date" in row.keys():
            result["has_requested_month_day"] = (row["has_requested_date"] or 0) > 0
        return result

    return {
        "requested_month_day": month_day,
        "scheduled_location_count": totals["scheduled_locations"],
        "distinct_schedule_date_count": totals["distinct_dates"],
        "schedule_date_range": [totals["first_date"], totals["last_date"]],
        "named_kurdistan_and_iraq_city_matches": [row_dict(row) for row in city_rows],
        "iraq_locations_with_source_schedules": [row_dict(row) for row in providers],
        "notes": [
            "A city listed in the location table may not own a prayer schedule; the original dependent-location relation or a nearby supported Salawati schedule may be needed.",
            "These diagnostics expose source rows only. They do not generate or adjust prayer times.",
        ],
    }

def audit(salawati_path: Path) -> dict[str, Any]:
    checks: list[dict[str, Any]] = []
    warnings: list[dict[str, Any]] = []
    stats: dict[str, Any] = {}

    with open_readonly(salawati_path) as salawati:
        integrity = salawati.execute("PRAGMA integrity_check").fetchone()[0]
        record(checks, "salawati.sqlite_integrity", integrity == "ok", integrity, "ok")

        requirements = {
            "country": {"_id", "name"},
            "location": {
                "_id", "name", "country_id", "latitude", "longitude",
                "has_fixed_prayer_time", "prayer_dependent_id",
            },
            "prayer_time": {"location_id", "date", *PRAYERS},
        }
        structure_ok = all(
            require_columns(checks, salawati, table, required)
            for table, required in requirements.items()
        )

        if structure_ok:
            country_count = salawati.execute("SELECT COUNT(*) FROM country").fetchone()[0]
            location_count = salawati.execute("SELECT COUNT(*) FROM location").fetchone()[0]
            prayer_count = salawati.execute("SELECT COUNT(*) FROM prayer_time").fetchone()[0]
            dependent_count = salawati.execute("""
                SELECT COUNT(*) FROM location WHERE COALESCE(prayer_dependent_id,0) > 0
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
            quality = clock_stats(salawati)
            source_date = __import__("datetime").datetime.now().strftime("%m-%d")
            stats["salawati_location_diagnostics"] = location_schedule_diagnostics(salawati, source_date)
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
                "timetable_quality": quality,
            }
            record(checks, "salawati.country_count", country_count >= 250, country_count, ">=250")
            record(checks, "salawati.location_count", location_count >= 160000, location_count, ">=160000")
            record(checks, "salawati.prayer_timetable_rows", prayer_count >= 45000, prayer_count, ">=45000")
            record(checks, "salawati.prayer_rows_reference_existing_locations", orphan_time_rows == 0, orphan_time_rows, 0)
            record(checks, "salawati.dependent_location_references", dangling_dependent_rows == 0, dangling_dependent_rows, 0)
            record(checks, "salawati.location_coordinate_bounds", (coordinate_rows["invalid"] or 0) == 0, coordinate_rows["invalid"] or 0, 0)
            record(
                checks,
                "salawati.timetable_rows_have_valid_dates_and_clock_values",
                quality["rows"] > 0 and quality["complete_rows"] / quality["rows"] >= 0.98,
                quality["complete_rows"],
                f">=98% of {quality['rows']} rows",
            )
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
                    "detail": "Duplicate source keys may return multiple schedule matches for a location/date.",
                })

    failures = [entry for entry in checks if not entry["passed"]]
    return {
        "schema_version": 2,
        "status": "pass" if not failures else "fail",
        "passed_checks": len(checks) - len(failures),
        "failed_checks": len(failures),
        "checks": checks,
        "statistics": stats,
        "warnings": warnings,
        "source_paths": {"salawati": str(salawati_path)},
        "notes": [
            "Only Salawati is accepted as the prayer-time schedule source.",
            "Dependent locations use Salawati's original prayer_dependent_id relation.",
            "Noor's prayer database is not a schedule or correction fallback; it remains in the app for separate source content such as Azkar and Allah names.",
            "GPS lookup uses Salawati locations and source schedule rows only.",
            "When no valid nearby Salawati schedule exists, the UI reports unavailable rather than inventing/calculating replacement times.",
            "The database is opened read-only; this audit never edits original source rows.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--salawati", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    report = audit(args.salawati)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({
        "status": report["status"],
        "passed_checks": report["passed_checks"],
        "failed_checks": report["failed_checks"],
        "statistics": report["statistics"],
        "source_city_diagnostics": report["statistics"].get("salawati_location_diagnostics", {}),
        "warnings": report["warnings"],
        "failures": [x for x in report["checks"] if not x["passed"]],
        "report": str(args.out),
    }, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
