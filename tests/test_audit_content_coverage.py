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
