import sys
import tempfile
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from tools.prepare_split_set import APP_NAME, ORIGINAL_PACKAGE, find_label_key, manifest_semantics, update_app_label

class PrepareSplitSetTests(unittest.TestCase):
    def test_label_key_rejects_wrong_package(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "AndroidManifest.xml"
            path.write_text(
                '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                f'package="{ORIGINAL_PACKAGE}"><application android:label="@string/app_name" /></manifest>',
                encoding="utf-8",
            )
            self.assertEqual(find_label_key(path), "app_name")
            path.write_text(
                '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                'package="com.example.wrong"><application android:label="@string/app_name" /></manifest>',
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                find_label_key(path)

    def test_updates_only_named_label_across_locales(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "res/values").mkdir(parents=True)
            (root / "res/values-ar").mkdir(parents=True)
            (root / "AndroidManifest.xml").write_text(
                '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                f'package="{ORIGINAL_PACKAGE}"><application android:label="@string/app_name" /></manifest>',
                encoding="utf-8",
            )
            (root / "res/values/strings.xml").write_text(
                '<resources><string name="app_name">My Prayers</string>'
                '<string name="other">My Prayers for Android</string></resources>',
                encoding="utf-8",
            )
            (root / "res/values-ar/strings.xml").write_text(
                '<resources><string name="app_name">صلواتي</string></resources>', encoding="utf-8"
            )
            report = update_app_label(root)
            self.assertEqual(report["string_resource"], "app_name")
            self.assertEqual(len(report["files_modified"]), 2)
            en = ET.parse(root / "res/values/strings.xml").getroot()
            ar = ET.parse(root / "res/values-ar/strings.xml").getroot()
            self.assertEqual(en.find("string[@name='app_name']").text, APP_NAME)
            self.assertEqual(ar.find("string[@name='app_name']").text, APP_NAME)
            self.assertEqual(en.find("string[@name='other']").text, "My Prayers for Android")


    def test_manifest_semantics_ignores_only_tool_build_metadata(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            original = root / "original.xml"
            rebuilt = root / "rebuilt.xml"
            base = (
                '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                f'package="{ORIGINAL_PACKAGE}" android:versionCode="68" '
            )
            original.write_text(
                base + 'platformBuildVersionCode="36" platformBuildVersionName="16">'
                '<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="36"/>'
                '<application android:label="@string/app_name"><activity android:name=".MainActivity" /></application>'
                '</manifest>', encoding="utf-8"
            )
            rebuilt.write_text(
                base + 'platformBuildVersionCode="35" platformBuildVersionName="15">'
                '<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="36"/>'
                '<application android:label="@string/app_name"><activity android:name=".MainActivity" /></application>'
                '</manifest>', encoding="utf-8"
            )
            self.assertEqual(manifest_semantics(original), manifest_semantics(rebuilt))
            rebuilt.write_text(
                base + 'platformBuildVersionCode="35" platformBuildVersionName="15">'
                '<uses-sdk android:minSdkVersion="24" android:targetSdkVersion="36"/>'
                '<application android:label="@string/app_name"><activity android:name=".OtherActivity" /></application>'
                '</manifest>', encoding="utf-8"
            )
            self.assertNotEqual(manifest_semantics(original), manifest_semantics(rebuilt))


if __name__ == "__main__":
    unittest.main()
