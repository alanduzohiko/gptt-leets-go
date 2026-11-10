#!/usr/bin/env python3
"""Audit every verse-linked Hadith row in Noor's encrypted source database."""
from __future__ import annotations

import argparse
import base64
import binascii
import hashlib
import json
import shutil
import sqlite3
import subprocess
import unicodedata
from pathlib import Path
from typing import Any

KEY = "c7fwxtzWmtNko9CdG8IlP9phi24ONllU"
COLLECTIONS = {
    "AbuDawud": "Sunan Abu Dawud",
    "Adab": "Al-Adab Al-Mufrad",
    "Bukhari": "Sahih al-Bukhari",
    "IbnMajah": "Sunan Ibn Majah",
    "Malik": "Muwatta Malik",
    "Maram": "Bulugh al-Maram",
    "Muslim": "Sahih Muslim",
    "Nasai": "Sunan al-Nasa'i",
    "Saliheen": "Riyad as-Salihin",
    "Tirmidhi": "Jami at-Tirmidhi",
}


def decrypt_field(value: Any, openssl: str) -> str | None:
    """Return decoded text, empty text for a valid encrypted empty field, or None on failure."""
    if not isinstance(value, str) or not value.strip():
        return None
    compact = "".join(value.split())
    try:
        ciphertext = base64.b64decode(compact, validate=True)
    except (ValueError, binascii.Error):
        return None
    if not ciphertext or len(ciphertext) % 16 != 0:
        return None
    try:
        result = subprocess.run(
            [openssl, "enc", "-d", "-aes-256-ecb", "-nosalt", "-K", KEY.encode("utf-8").hex()],
            input=ciphertext,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
            timeout=3,
        )
    except (OSError, subprocess.TimeoutExpired):
        return None
    if result.returncode != 0:
        return None
    try:
        plain = result.stdout.decode("utf-8", errors="strict").strip()
    except UnicodeDecodeError:
        return None
    if any(unicodedata.category(ch) == "Cc" and ch not in "\n\r\t" for ch in plain):
        return None
    return plain


def has_arabic(text: str) -> bool:
    return any("\u0600" <= ch <= "\u06ff" for ch in text)


def audit(database: Path) -> dict[str, Any]:
    openssl = shutil.which("openssl")
    if not openssl:
        raise RuntimeError("OpenSSL is required for this source-format audit")
    if not database.is_file() or database.stat().st_size == 0:
        raise RuntimeError(f"Source Hadith database is missing or empty: {database}")

    samples: list[dict[str, Any]] = []
    unknown_codes: set[str] = set()
    known_codes: set[str] = set()
    source_empty_arabic_ids: list[int] = []
    field_failures = {name: 0 for name in ("collection", "arabic", "english", "reference", "verse_reference")}
    field_failure_ids: dict[str, list[int]] = {name: [] for name in field_failures}
    valid_rows = 0
    rows_with_arabic_text = 0
    rows_examined = 0

    # Collection ciphertexts repeat for most rows; caching avoids repeatedly
    # spawning OpenSSL for the same small set of collection identifiers.
    decoded_cache: dict[str, str | None] = {}

    def decode(value: Any) -> str | None:
        if not isinstance(value, str):
            return None
        if value not in decoded_cache:
            decoded_cache[value] = decrypt_field(value, openssl)
        return decoded_cache[value]

    connection = sqlite3.connect(f"file:{database.resolve()}?mode=ro", uri=True)
    try:
        connection.row_factory = sqlite3.Row
        integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            raise RuntimeError(f"SQLite integrity check failed: {integrity}")
        total = int(connection.execute("SELECT COUNT(*) FROM hadith_on_ayah").fetchone()[0])
        rows = connection.execute(
            """
            SELECT id,collection,arabic,english,reference,verse_reference
            FROM hadith_on_ayah ORDER BY id
            """
        ).fetchall()
    finally:
        connection.close()

    for row in rows:
        rows_examined += 1
        row_id = int(row["id"])
        decoded = {
            "collection": decode(row["collection"]),
            "arabic": decode(row["arabic"]),
            "english": decode(row["english"]),
            "reference": decode(row["reference"]),
        }
        row_valid = True

        code = decoded["collection"]
        if not code or code not in COLLECTIONS:
            field_failures["collection"] += 1
            field_failure_ids["collection"].append(row_id)
            if code:
                unknown_codes.add(code)
            row_valid = False
        else:
            known_codes.add(code)

        arabic = decoded["arabic"]
        if arabic is None:
            field_failures["arabic"] += 1
            field_failure_ids["arabic"].append(row_id)
            row_valid = False
        elif arabic:
            if not has_arabic(arabic):
                field_failures["arabic"] += 1
                field_failure_ids["arabic"].append(row_id)
                row_valid = False
            else:
                rows_with_arabic_text += 1
        else:
            # A valid ciphertext may intentionally represent an empty Arabic
            # field in the original source; it is not a decryption failure.
            source_empty_arabic_ids.append(row_id)

        for field in ("english", "reference"):
            value = decoded[field]
            if value is None or not value.strip():
                field_failures[field] += 1
                field_failure_ids[field].append(row_id)
                row_valid = False

        verse_reference = str(row["verse_reference"] or "").strip()
        if not verse_reference:
            field_failures["verse_reference"] += 1
            field_failure_ids["verse_reference"].append(row_id)
            row_valid = False

        if row_valid:
            valid_rows += 1
            if arabic and len(samples) < 8:
                samples.append({
                    "id": row_id,
                    "collection_code": code,
                    "arabic_readable": True,
                    "english_decoded": True,
                    "reference_decoded": True,
                    "verse_reference": verse_reference,
                    "decoded_text_sha256": hashlib.sha256(
                        (code + "\n" + arabic + "\n" + decoded["english"] + "\n" + decoded["reference"]).encode("utf-8")
                    ).hexdigest(),
                })

    all_rows_examined = rows_examined == total
    required_fields_valid = all(field_failures[name] == 0 for name in ("collection", "english", "reference", "verse_reference"))
    arabic_field_valid = field_failures["arabic"] == 0
    codes_mapped = bool(known_codes) and not unknown_codes and field_failures["collection"] == 0
    passed = (
        total >= 1000
        and all_rows_examined
        and valid_rows == total
        and required_fields_valid
        and arabic_field_valid
        and codes_mapped
    )
    checks = {
        "minimum_source_rows": total >= 1000,
        "every_source_row_examined": all_rows_examined,
        "all_required_fields_decrypt": required_fields_valid,
        "arabic_fields_decrypt_or_are_source_empty": arabic_field_valid,
        "collection_codes_map_to_source_names": codes_mapped,
        "verse_references_present": field_failures["verse_reference"] == 0,
    }
    return {
        "schema_version": 2,
        "status": "pass" if passed else "fail",
        "database": database.name,
        "sqlite_integrity": integrity,
        "source_rows": total,
        "rows_examined": rows_examined,
        "valid_decrypted_rows": valid_rows,
        "rows_with_arabic_text": rows_with_arabic_text,
        "rows_with_valid_empty_arabic_source": len(source_empty_arabic_ids),
        "valid_empty_arabic_source_row_ids": source_empty_arabic_ids,
        "field_decode_failures": field_failures,
        "field_failure_row_ids": field_failure_ids,
        "known_source_collection_codes": sorted(known_codes),
        "unknown_decrypted_collection_codes": sorted(unknown_codes),
        "source_aes_format": "AES-256-ECB with PKCS5/PKCS7 padding; Base64 payload",
        "checks": checks,
        "sample_rows": samples,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--database", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()
    report = audit(args.database)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({
        "status": report["status"],
        "source_rows": report["source_rows"],
        "rows_examined": report["rows_examined"],
        "valid_decrypted_rows": report["valid_decrypted_rows"],
        "rows_with_arabic_text": report["rows_with_arabic_text"],
        "rows_with_valid_empty_arabic_source": report["rows_with_valid_empty_arabic_source"],
        "field_decode_failures": report["field_decode_failures"],
        "known_source_collection_codes": report["known_source_collection_codes"],
        "unknown_decrypted_collection_codes": report["unknown_decrypted_collection_codes"],
        "checks": report["checks"],
        "report": str(args.out),
    }, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
