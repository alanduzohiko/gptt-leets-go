import tempfile
import unittest
from pathlib import Path

from tools.audit_content_wiring import audit


class AuditContentWiringTests(unittest.TestCase):
    def setUp(self):
        self.tempdir = tempfile.TemporaryDirectory()
        self.addCleanup(self.tempdir.cleanup)

    def source(self, text: str) -> Path:
        path = Path(self.tempdir.name) / "Sample.kt"
        path.write_text(text, encoding="utf-8")
        return path

    def test_existing_references_pass(self):
        catalog = {
            "counts": {"files": 2},
            "files": [
                {"asset_path": "content/app/base/assets/quran.db"},
                {"asset_path": "content/app/base/assets/fonts/arabic.ttf"},
            ],
            "generated_assets": [{"asset_path": "mushaf_svg_index.json"}],
        }
        source = self.source(
            'const val DB = "content/app/base/assets/quran.db"\n'
            'val font = "content/app/base/assets/fonts/arabic.ttf"\n'
        )
        report = audit(catalog, [source])
        self.assertTrue(report["ok"])
        self.assertEqual([], report["missing_references"])

    def test_source_specific_contracts_are_not_applied_to_synthetic_fixture(self):
        catalog = {
            "files": [{"asset_path": "content/app/base/assets/quran.db"}],
            "generated_assets": [{"asset_path": "mushaf_svg_index.json"}],
        }
        source = self.source('val db = "content/app/base/assets/quran.db"')
        report = audit(catalog, [source])
        self.assertTrue(report["ok"])
        self.assertFalse(report["runtime_prayer_source_contract"]["applicable"])
        self.assertFalse(report["quran_font_source_integrity_contract"]["applicable"])

    def test_missing_literal_is_reported(self):
        catalog = {"files": [], "generated_assets": [{"asset_path": "mushaf_svg_index.json"}]}
        source = self.source('val missing = "content/noor/base/assets/missing.db"')
        report = audit(catalog, [source])
        self.assertFalse(report["ok"])
        self.assertEqual(
            "content/noor/base/assets/missing.db",
            report["missing_references"][0]["path"],
        )

    def test_missing_derived_khairah_binary_is_reported(self):
        db = "content/khairah_1.2/base/assets/tafsir/ibn-kathir.sqlite"
        catalog = {
            "files": [{"asset_path": db}],
            "generated_assets": [{"asset_path": "mushaf_svg_index.json"}],
        }
        source = self.source(f'val db = "{db}"')
        report = audit(catalog, [source])
        self.assertFalse(report["ok"])
        self.assertIn(
            "content/khairah_1.2/base/assets/tafsir/ibn-kathir.bin",
            [item["path"] for item in report["missing_references"]],
        )


if __name__ == "__main__":
    unittest.main()
