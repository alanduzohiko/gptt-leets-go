import tempfile
import unittest
import zipfile
from pathlib import Path

from tools.normalize_standalone_apk import normalize_apk


class NormalizeStandaloneApkTests(unittest.TestCase):
    def test_dex_native_libraries_and_resource_table_are_stored(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "fixture.apk"
            payloads = {
                "AndroidManifest.xml": b"manifest fixture",
                "resources.arsc": b"resource table fixture",
                "classes.dex": b"dex fixture",
                "classes2.dex": b"dex2 fixture",
                "lib/arm64-v8a/libdemo.so": b"native library fixture",
                "assets/data.json": b'{"message":"ordinary asset"}',
            }
            with zipfile.ZipFile(apk, "w") as z:
                for name, payload in payloads.items():
                    z.writestr(name, payload, compress_type=zipfile.ZIP_DEFLATED)

            report = normalize_apk(apk)
            self.assertTrue(report["payloads_byte_identical"])
            self.assertEqual(report["dex_entries_stored_uncompressed"], 2)
            self.assertEqual(report["native_library_entries_stored_uncompressed"], 1)
            with zipfile.ZipFile(apk, "r") as z:
                for name in ("classes.dex", "classes2.dex", "lib/arm64-v8a/libdemo.so", "resources.arsc"):
                    self.assertEqual(z.getinfo(name).compress_type, zipfile.ZIP_STORED)
                    self.assertEqual(z.read(name), payloads[name])
                self.assertEqual(z.getinfo("assets/data.json").compress_type, zipfile.ZIP_DEFLATED)
                self.assertEqual(z.read("assets/data.json"), payloads["assets/data.json"])
    
    def test_rejects_duplicate_entry_names(self):
        with tempfile.TemporaryDirectory() as tmp:
            apk = Path(tmp) / "duplicate.apk"
            with zipfile.ZipFile(apk, "w") as z:
                z.writestr("AndroidManifest.xml", b"m")
                z.writestr("resources.arsc", b"r")
                z.writestr("classes.dex", b"d1")
                z.writestr("classes.dex", b"d2")
            with self.assertRaisesRegex(ValueError, "duplicate"):
                normalize_apk(apk)


if __name__ == "__main__":
    unittest.main()
