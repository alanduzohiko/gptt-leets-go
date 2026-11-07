import sqlite3
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from tools.audit_hadith_decoder import audit


class AuditHadithDecoderTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.database = Path(self.temp.name) / "hadith.db"
        with sqlite3.connect(self.database) as connection:
            connection.execute("""
                CREATE TABLE hadith_on_ayah (
                    id INTEGER PRIMARY KEY,
                    collection TEXT NOT NULL,
                    arabic TEXT NOT NULL,
                    english TEXT NOT NULL,
                    reference TEXT NOT NULL,
                    verse_reference TEXT NOT NULL
                )
            """)
            connection.executemany(
                "INSERT INTO hadith_on_ayah VALUES (?, ?, ?, ?, ?, ?)",
                [
                    (i, "cipher-collection", "cipher-arabic", "cipher-english", "cipher-reference", f"[1:{i}]")
                    for i in range(1, 1000)
                ] + [
                    (1000, "cipher-collection", "cipher-valid-empty-arabic", "cipher-english", "cipher-reference", "[2:1]")
                ],
            )

    def decode_fixture(self, value, _openssl):
        return {
            "cipher-collection": "Bukhari",
            "cipher-arabic": "نص حديث عربي صحيح",
            "cipher-valid-empty-arabic": "",
            "cipher-english": "A source-linked English translation",
            "cipher-reference": "Sahih al-Bukhari 1:1",
        }.get(value)

    def test_audit_examines_every_row_and_accepts_a_valid_empty_source_arabic_field(self):
        with patch("tools.audit_hadith_decoder.shutil.which", return_value="openssl"), patch(
            "tools.audit_hadith_decoder.decrypt_field", side_effect=self.decode_fixture
        ):
            report = audit(self.database)

        self.assertEqual("pass", report["status"])
        self.assertEqual(1000, report["source_rows"])
        self.assertEqual(1000, report["rows_examined"])
        self.assertEqual(1000, report["valid_decrypted_rows"])
        self.assertEqual(999, report["rows_with_arabic_text"])
        self.assertEqual([1000], report["valid_empty_arabic_source_row_ids"])
        self.assertEqual(8, len(report["sample_rows"]))
        self.assertTrue(report["checks"]["every_source_row_examined"])

    def test_invalid_required_ciphertext_fails_instead_of_being_hidden_by_early_samples(self):
        with sqlite3.connect(self.database) as connection:
            connection.execute(
                "INSERT INTO hadith_on_ayah VALUES (?, ?, ?, ?, ?, ?)",
                (1001, "cipher-collection", "cipher-arabic", "cipher-invalid-english", "cipher-reference", "[2:2]"),
            )

        with patch("tools.audit_hadith_decoder.shutil.which", return_value="openssl"), patch(
            "tools.audit_hadith_decoder.decrypt_field", side_effect=self.decode_fixture
        ):
            report = audit(self.database)

        self.assertEqual("fail", report["status"])
        self.assertEqual(1001, report["rows_examined"])
        self.assertEqual(1, report["field_decode_failures"]["english"])
        self.assertEqual([1001], report["field_failure_row_ids"]["english"])

    def test_unknown_collection_code_fails_the_full_source_audit(self):
        with sqlite3.connect(self.database) as connection:
            connection.execute("UPDATE hadith_on_ayah SET collection='cipher-unknown' WHERE id=1000")

        def decode_with_unknown(value, openssl):
            if value == "cipher-unknown":
                return "UnknownCollection"
            return self.decode_fixture(value, openssl)

        with patch("tools.audit_hadith_decoder.shutil.which", return_value="openssl"), patch(
            "tools.audit_hadith_decoder.decrypt_field", side_effect=decode_with_unknown
        ):
            report = audit(self.database)

        self.assertEqual("fail", report["status"])
        self.assertEqual(["UnknownCollection"], report["unknown_decrypted_collection_codes"])


if __name__ == "__main__":
    unittest.main()
