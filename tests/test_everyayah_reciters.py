import json
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "unified-app" / "catalogs" / "everyayah_verse_reciters.json"


class EveryAyahReciterCatalogueTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.data = json.loads(CATALOG.read_text(encoding="utf-8"))
        cls.records = cls.data["records"]

    def test_catalogue_contains_ten_verified_complete_ayah_sources(self):
        self.assertEqual(10, self.data["record_count"])
        self.assertEqual(10, len(self.records))
        self.assertTrue(all(record["audio_type"] == 0 for record in self.records))
        self.assertTrue(all(record["surah_total"] == 114 for record in self.records))

    def test_every_source_has_full_canonical_ayah_coverage(self):
        for record in self.records:
            with self.subTest(reciter=record["name"]):
                self.assertEqual(6236, record["verified_canonical_ayahs"])
                self.assertEqual(0, record["missing_canonical_ayahs"])
                self.assertEqual(
                    ["001001.mp3", "114006.mp3"],
                    [sample["file"] for sample in record["sample_checks"]],
                )
                self.assertTrue(all(sample["http_status"] == 200 for sample in record["sample_checks"]))
                self.assertTrue(all(sample["mime"] == "audio/mpeg" for sample in record["sample_checks"]))

    def test_url_template_uses_three_digit_surah_and_ayah_paths(self):
        for record in self.records:
            with self.subTest(reciter=record["name"]):
                self.assertEqual(
                    f'https://everyayah.com/data/{record["folder"]}/{{surah3}}{{ayah3}}.mp3',
                    record["url_template"],
                )

    def test_catalogue_does_not_claim_audio_redistribution_rights(self):
        self.assertEqual("unknown", self.data["audio_license_status"])
        self.assertIn("not bundled", self.data["source_note"].lower())
        self.assertIn("no blanket audio redistribution licence", self.data["source_note"].lower())


if __name__ == "__main__":
    unittest.main()
