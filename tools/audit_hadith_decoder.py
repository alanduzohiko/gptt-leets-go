#!/usr/bin/env python3
"""Check that real Noor verse-linked Hadith ciphertext decrypts to readable data."""
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


def decrypt_field(value: Any, openssl: str) -> str:
    if not isinstance(value, str) or not value.strip():
        return ""
    compact = "".join(value.split())
    try:
        ciphertext = base64.b64decode(compact, validate=True)
    except (ValueError, binascii.Error):
        return ""
    if not ciphertext or len(ciphertext) % 16 != 0:
        return ""
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
        return ""
    if result.returncode != 0:
        return ""
    try:
        plain = result.stdout.decode("utf-8", errors="strict").strip()
    except UnicodeDecodeError:
        return ""
    if any(unicodedata.category(ch) == "Cc" and ch not in "\n\r\t" for ch in plain):
        return ""
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
    decoded_rows = 0
    rows_examined = 0
    with sqlite3.connect(f"file:{database.resolve()}?mode=ro", uri=True) as connection:
        connection.row_factory = sqlite3.Row
        integrity = connection.execute("PRAGMA integrity_check").fetchone()[0]
        if integrity != "ok":
            raise RuntimeError(f"SQLite integrity check failed: {integrity}")
        total = connection.execute("SELECT COUNT(*) FROM hadith_on_ayah").fetchone()[0]
        rows = connection.execute(
            """
            SELECT id,collection,arabic,english,reference,verse_reference
            FROM hadith_on_ayah ORDER BY id LIMIT 100
            """
        ).fetchall()

    for row in rows:
        rows_examined += 1
        code = decrypt_field(row["collection"], openssl)
        arabic = decrypt_field(row["arabic"], openssl)
        english = decrypt_field(row["english"], openssl)
        reference = decrypt_field(row["reference"], openssl)
        if code and code not in COLLECTIONS:
            unknown_codes.add(code)
        if code in COLLECTIONS and has_arabic(arabic):
            decoded_rows += 1
            samples.append({
                "id": int(row["id"]),
                "collection_code": code,
                "arabic_readable": True,
                "english_decoded": bool(english),
                "reference_decoded": bool(reference),
                "linked_verse_count": len([
                    part for part in str(row["verse_reference"] or "").split(",") if part.strip()
                ]),
                "decoded_text_sha256": hashlib.sha256(
                    (code + "\n" + arabic + "\n" + english + "\n" + reference).encode("utf-8")
                ).hexdigest(),
            })
        if len(samples) >= 8:
            break

    passed = total >= 1000 and decoded_rows >= 5
    return {
        "schema_version": 1,
        "status": "pass" if passed else "fail",
        "database": database.name,
        "sqlite_integrity": integrity,
        "source_rows": total,
        "rows_examined": rows_examined,
        "valid_decrypted_rows": decoded_rows,
        "known_source_collection_codes": sorted({row["collection_code"] for row in samples}),
        "unknown_decrypted_collection_codes": sorted(unknown_codes),
        "source_aes_format": "AES-256-ECB with PKCS5/PKCS7 padding; Base64 payload",
        "checks": {
            "minimum_source_rows": total >= 1000,
            "minimum_real_rows_successfully_decrypted": decoded_rows >= 5,
            "collection_codes_map_to_source_names": bool(samples) and all(row["collection_code"] in COLLECTIONS for row in samples),
            "arabic_fields_are_readable_unicode": bool(samples) and all(row["arabic_readable"] for row in samples),
        },
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
        "known_source_collection_codes": report["known_source_collection_codes"],
        "unknown_decrypted_collection_codes": report["unknown_decrypted_collection_codes"],
        "checks": report["checks"],
        "report": str(args.out),
    }, ensure_ascii=False, indent=2))
    return 0 if report["status"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
