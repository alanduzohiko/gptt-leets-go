import unittest

from tools.audit_content_coverage import verify


class AuditContentCoverageTests(unittest.TestCase):
    def test_missing_source_databases_fail_coverage(self):
        report = verify({"counts": {"source_apps": 3}})
        self.assertEqual("fail", report["status"])
        self.assertGreater(report["failed_checks"], 0)
        names = [check["name"] for check in report["checks"]]
        self.assertIn(
            "table:content/noor_tafsir_1.2.2/base/assets/quran.db:quran",
            names,
        )

    def test_valid_quran_table_passes_its_individual_check(self):
        quran_path = "content/noor_tafsir_1.2.2/base/assets/quran.db"
        catalog = {
            "counts": {},
            "databases": [{
                "asset_path": quran_path,
                "integrity": "ok",
                "tables": [
                    {"name": "quran", "rows": 6236, "columns": ["surah_id", "verse_id"]},
                ],
            }],
        }
        report = verify(catalog)
        quran_check = next(
            check for check in report["checks"]
            if check["name"] == f"table:{quran_path}:quran"
        )
        self.assertTrue(quran_check["passed"])
        self.assertEqual(6236, quran_check["actual"])

    def test_incomplete_quran_table_fails_even_with_valid_sqlite_integrity(self):
        quran_path = "content/noor_tafsir_1.2.2/base/assets/quran.db"
        catalog = {
            "counts": {},
            "databases": [{
                "asset_path": quran_path,
                "integrity": "ok",
                "tables": [{"name": "quran", "rows": 6235, "columns": []}],
            }],
        }
        report = verify(catalog)
        quran_check = next(
            check for check in report["checks"]
            if check["name"] == f"table:{quran_path}:quran"
        )
        self.assertFalse(quran_check["passed"])

    def test_multilingual_hadith_articles_and_salawati_name_translations_are_required(self):
        hadith_path = "content/noor_tafsir_1.2.2/base/assets/db_hadith.db"
        salawati_path = "content/salawati_3.1.3/base/assets/database/muslim_db_v2.5.1.db"
        catalog = {
            "counts": {},
            "databases": [
                {
                    "asset_path": hadith_path,
                    "integrity": "ok",
                    "tables": [
                        {"name": "articles", "rows": 11, "columns": ["description", "text", "text_ar", "text_en"]},
                        {"name": "author", "rows": 11, "columns": ["author"]},
                    ],
                },
                {
                    "asset_path": salawati_path,
                    "integrity": "ok",
                    "tables": [
                        {"name": "name_translation", "rows": 594, "columns": ["name_id", "language", "name"]},
                    ],
                },
            ],
        }
        report = verify(catalog)
        checks = {check["name"]: check for check in report["checks"]}
        for table_path, table in [
            (hadith_path, "articles"),
            (hadith_path, "author"),
            (salawati_path, "name_translation"),
        ]:
            check = checks[f"table:{table_path}:{table}"]
            self.assertTrue(check["passed"], check)
        self.assertEqual(11, report["summary"]["hadith_articles_minimum"])
        self.assertEqual(594, report["summary"]["salawati_name_translation_minimum"])

    def test_database_with_failed_integrity_does_not_pass_by_row_count_alone(self):
        quran_path = "content/noor_tafsir_1.2.2/base/assets/quran.db"
        catalog = {
            "counts": {},
            "databases": [{
                "asset_path": quran_path,
                "integrity": "corrupt",
                "tables": [{"name": "quran", "rows": 6236, "columns": []}],
            }],
        }
        report = verify(catalog)
        quran_check = next(
            check for check in report["checks"]
            if check["name"] == f"table:{quran_path}:quran"
        )
        self.assertFalse(quran_check["passed"])


if __name__ == "__main__":
    unittest.main()
