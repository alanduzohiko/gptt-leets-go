import json
import tempfile
import unittest
import zipfile
from pathlib import Path

from tools.audit_apk_assets import REQUIRED_OTHER, REQUIRED_SQLITE, SQLITE_HEADER, audit


class AuditApkAssetsTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.apk = self.root / "app.apk"
        self.catalog_path = self.root / "catalog.json"

    def create_fixture(self, omit=None, corrupt_size=None, corrupt_sqlite=None):
        omit = set(omit or ())
        corrupt_size = corrupt_size or {}
        corrupt_sqlite = set(corrupt_sqlite or ())
        declared = []
        assets = {}
        for path in REQUIRED_SQLITE:
            payload = SQLITE_HEADER + b"unit-test-db:" + path.encode("utf-8")
            if path in corrupt_sqlite:
                payload = b"not-sqlite-data:" + path.encode("utf-8")
            assets[path] = payload
            declared.append({"asset_path": path, "size_bytes": len(payload)})
        for path in REQUIRED_OTHER:
            if path == "assets/content_catalog.json":
                continue
            if path == "mushaf_svg_index.json":
                payload = json.dumps({
                    "page_count": 604,
                    "pages": [{"page": i} for i in range(1, 605)]
                }).encode("utf-8")
            else:
                payload = b"uncompressed-test-asset-data"
            assets[path] = payload
            declared.append({"asset_path": path, "size_bytes": len(payload)})
        catalog = {"bundle_sha256": "source-bundle-fixture", "counts": {"source_apps": 3}, "files": declared, "generated_assets": []}
        self.catalog_path.write_text(json.dumps(catalog), encoding="utf-8")
        with zipfile.ZipFile(self.apk, "w") as archive:
            embedded = json.dumps(catalog).encode("utf-8")
            archive.writestr("assets/content_catalog.json", embedded)
            for path, payload in assets.items():
                if path in omit:
                    continue
                if path in corrupt_size:
                    # Deliberately mismatch the catalog's expected uncompressed size.
                    archive.writestr("assets/" + path, payload + b"size-mismatch")
                else:
                    stored = path.endswith(".bin")
                    archive.writestr(
                        "assets/" + path,
                        payload,
                        compress_type=zipfile.ZIP_STORED if stored else zipfile.ZIP_DEFLATED,
                    )
        return catalog

    def test_valid_apk_with_full_core_assets_passes(self):
        self.create_fixture()
        report = audit(self.apk, self.catalog_path)
        self.assertTrue(report["ok"], report["problems"])
        self.assertEqual([], report["missing_assets"])
        self.assertEqual([], report["size_mismatches"])
        self.assertEqual(len(REQUIRED_SQLITE), len(report["required_sqlite_header_checks"]))
        self.assertTrue(all(check["passed"] for check in report["required_sqlite_header_checks"]))
        self.assertTrue(all(check["passed"] for check in report["random_access_storage_checks"]))

    def test_missing_packaged_quran_database_fails(self):
        self.create_fixture(omit={REQUIRED_SQLITE[0]})
        report = audit(self.apk, self.catalog_path)
        self.assertFalse(report["ok"])
        self.assertIn("assets/" + REQUIRED_SQLITE[0], report["missing_assets"])

    def test_incorrect_asset_size_fails(self):
        self.create_fixture(corrupt_size={REQUIRED_SQLITE[0]: True})
        report = audit(self.apk, self.catalog_path)
        self.assertFalse(report["ok"])
        self.assertTrue(any(item["kind"] == "asset_size_mismatch" and item["asset_path"] == REQUIRED_SQLITE[0] for item in report["problems"]))

    def test_invalid_sqlite_header_fails(self):
        self.create_fixture(corrupt_sqlite={REQUIRED_SQLITE[0]})
        report = audit(self.apk, self.catalog_path)
        self.assertFalse(report["ok"])
        self.assertTrue(any(item["kind"] == "invalid_sqlite_header" and item["asset_path"] == REQUIRED_SQLITE[0] for item in report["problems"]))

    def test_missing_catalog_required_asset_fails(self):
        catalog = self.create_fixture()
        catalog["files"] = [item for item in catalog["files"] if item["asset_path"] != REQUIRED_SQLITE[0]]
        self.catalog_path.write_text(json.dumps(catalog), encoding="utf-8")
        report = audit(self.apk, self.catalog_path)
        self.assertFalse(report["ok"])
        self.assertTrue(any(item["kind"] == "required_asset_not_in_catalog" and item["asset_path"] == REQUIRED_SQLITE[0] for item in report["problems"]))


if __name__ == "__main__":
    unittest.main()
