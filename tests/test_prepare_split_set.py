import sys
import tempfile
import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from tools.prepare_split_set import (
    APP_NAME, ORIGINAL_PACKAGE, TARGET_PACKAGE, find_label_key,
    manifest_semantics, rewrite_package_identity, update_app_label
)

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


    def test_package_identity_changes_for_side_by_side_install_without_renaming_classes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            manifest = root / "AndroidManifest.xml"
            manifest.write_text(
                '<manifest xmlns:android="http://schemas.android.com/apk/res/android" '
                f'package="{ORIGINAL_PACKAGE}">'
                f'<uses-permission android:name="{ORIGINAL_PACKAGE}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" />'
                '<application>'
                f'<provider android:name="com.google.firebase.provider.FirebaseInitProvider" '
                f'android:authorities="{ORIGINAL_PACKAGE}.firebaseinitprovider" />'
                f'<activity android:name="{ORIGINAL_PACKAGE}.screens.MainActivity" />'
                '</application></manifest>',
                encoding="utf-8",
            )
            before = manifest_semantics(manifest)
            report = rewrite_package_identity(root)
            after_root = ET.parse(manifest).getroot()
            self.assertEqual(after_root.attrib["package"], TARGET_PACKAGE)
            provider = after_root.find("application/provider")
            activity = after_root.find("application/activity")
            permission = after_root.find("uses-permission")
            ns = "{" + "http://schemas.android.com/apk/res/android" + "}"
            self.assertEqual(provider.attrib[ns + "authorities"], TARGET_PACKAGE + ".firebaseinitprovider")
            self.assertEqual(activity.attrib[ns + "name"], ORIGINAL_PACKAGE + ".screens.MainActivity")
            self.assertEqual(permission.attrib[ns + "name"], TARGET_PACKAGE + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
            self.assertEqual(manifest_semantics(manifest), before)
            self.assertEqual(report["target_package_id"], TARGET_PACKAGE)
            self.assertTrue(report["component_class_names_preserved"])

    def test_rewrites_only_runtime_application_id_and_audio_uris(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            smali = root / "smali/com/kosratdahmad/myprayers/BuildConfig.smali"
            smali.parent.mkdir(parents=True)
            lines = [
                '.class public Lcom/kosratdahmad/myprayers/BuildConfig;\n',
                '.super Ljava/lang/Object;\n',
                '    const-string v0, "com.kosratdahmad.myprayers"\n',
                '    const-string v1, "Lcom/kosratdahmad/myprayers/screens/MainActivity;"\n',
            ]
            lines.extend(
                f'    const-string v2, "android.resource://{ORIGINAL_PACKAGE}/raw/tone{i}"\n'
                for i in range(12)
            )
            lines.append('    const-string v3, "com.kosratdahmad.myprayers.ACTION_WIDGET_UPDATED"\n')
            smali.write_text("".join(lines), encoding="utf-8")
            report = patch_runtime_package_literals(root)
            text = smali.read_text(encoding="utf-8")
            self.assertEqual(report["exact_application_id_literals_rewritten"], 1)
            self.assertEqual(report["audio_resource_uri_literals_rewritten"], 12)
            self.assertIn('const-string v0, "' + TARGET_PACKAGE + '"', text)
            self.assertIn('Lcom/kosratdahmad/myprayers/screens/MainActivity;', text)
            self.assertNotIn('android.resource://' + ORIGINAL_PACKAGE + '/raw/', text)
            self.assertIn('const-string v3, "com.kosratdahmad.myprayers.ACTION_WIDGET_UPDATED"', text)

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
