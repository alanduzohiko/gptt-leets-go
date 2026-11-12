import json
import unittest
from pathlib import Path


REPOSITORY_ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = REPOSITORY_ROOT / "unified-app" / "catalogs" / "muezzin_catalog.json"


class MuezzinCatalogTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))
        cls.records = cls.catalog["records"]

    def test_catalog_is_a_real_separate_muezzin_catalogue(self):
        self.assertEqual("Mashxall curated muezzin voices", self.catalog["collection_name"])
        self.assertEqual(self.catalog["record_count"], len(self.records))
        self.assertGreaterEqual(len(self.records), 60)
        self.assertTrue(all(row["category"] == "muezzin" for row in self.records))

    def test_each_record_has_a_unique_https_mp3_source(self):
        ids = [row["id"] for row in self.records]
        self.assertEqual(len(ids), len(set(ids)))
        for row in self.records:
            self.assertTrue(row["id"])
            self.assertTrue(row["title_ar"] or row["title_en"])
            self.assertTrue(row["url"].startswith("https://"))
            self.assertTrue(row["url"].endswith(".mp3"))
            self.assertGreater(row["size_bytes"], 0)

    def test_requested_famous_and_kurdish_muezzins_are_present(self):
        all_names = " ".join(
            f'{row["title_ar"]} {row["title_en"]} {row["source_filename"]}'
            for row in self.records
        ).lower()
        for name in (
            "منصور السالمي",
            "إسلام صبحي",
            "احمد النفيس",
            "ناصر القطامي",
            "ياسر الدوسري",
            "أحمد الكوردي",
        ):
            self.assertIn(name.lower(), all_names)

    def test_non_haram_location_recordings_and_mosque_categories_are_excluded(self):
        forbidden = ("الأقصى", "القدس", "دبي", "مصر", "aqsa", "dubai", "egypt", "masjed_qeba")
        for row in self.records:
            self.assertFalse(any(word in f'{row["title_ar"]} {row["title_en"]} {row["source_filename"]}'.lower()
                                 for word in forbidden))
            self.assertEqual("muezzin", row["category"])

    def test_recording_rights_are_not_claimed_without_a_source_license(self):
        note = self.catalog["source_note"].lower()
        self.assertIn("no declared audio licence", note)
        self.assertIn("do not bundle or redistribute", note)


if __name__ == "__main__":
    unittest.main()
