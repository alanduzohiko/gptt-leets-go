import sys
import tempfile
import unittest
import struct
from pathlib import Path
import xml.etree.ElementTree as ET

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from tools.prepare_split_set import (
    APP_NAME, ORIGINAL_PACKAGE, TARGET_PACKAGE, find_label_key,
    manifest_semantics, rewrite_package_identity, patch_binary_xml_package_id,
    patch_resources_table_package_name, update_app_label, patch_runtime_package_literals
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
                'android:requiredSplitTypes="base__abi,base__density" android:splitTypes="" '
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
            ns = "{" + "http://schemas.android.com/apk/res/android" + "}"
            self.assertNotIn(ns + "requiredSplitTypes", after_root.attrib)
            self.assertNotIn(ns + "splitTypes", after_root.attrib)
            self.assertEqual(len(report["removed_split_only_manifest_attributes"]), 2)
            provider = after_root.find("application/provider")
            activity = after_root.find("application/activity")
            permission = after_root.find("uses-permission")
            self.assertEqual(provider.attrib[ns + "authorities"], TARGET_PACKAGE + ".firebaseinitprovider")
            self.assertEqual(activity.attrib[ns + "name"], ORIGINAL_PACKAGE + ".screens.MainActivity")
            self.assertEqual(permission.attrib[ns + "name"], TARGET_PACKAGE + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
            self.assertEqual(manifest_semantics(manifest), before)
            self.assertEqual(report["target_package_id"], TARGET_PACKAGE)
            self.assertTrue(report["component_class_names_preserved"])

    def test_binary_xml_package_patch_preserves_split_and_updates_long_package(self):
        def length8(value):
            if value <= 0x7F:
                return bytes([value])
            return bytes([(value >> 8) | 0x80, value & 0xFF])

        values = ["manifest", "package", ORIGINAL_PACKAGE, "split", "config.arm64_v8a"]
        string_data = bytearray()
        offsets = []
        for value in values:
            offsets.append(len(string_data))
            raw = value.encode("utf-8")
            utf16_length = len(value.encode("utf-16le")) // 2
            string_data.extend(length8(utf16_length) + length8(len(raw)) + raw + b"\0")
        while len(string_data) % 4:
            string_data.append(0)
        strings_start = 28 + len(values) * 4
        pool_size = strings_start + len(string_data)
        pool = (
            struct.pack("<HHIIIIII", 0x0001, 28, pool_size, len(values), 0, 0x100, strings_start, 0)
            + b"".join(struct.pack("<I", offset) for offset in offsets)
            + bytes(string_data)
        )

        def attr(name_index, value_index):
            return struct.pack("<IIIHBBI", 0xFFFFFFFF, name_index, value_index, 8, 0, 3, value_index)

        attr_bytes = attr(1, 2) + attr(3, 4)
        start_size = 16 + 20 + len(attr_bytes)
        start = (
            struct.pack("<HHIII", 0x0102, 16, start_size, 1, 0xFFFFFFFF)
            + struct.pack("<IIHHHHHH", 0xFFFFFFFF, 0, 20, 20, 2, 0, 0, 0)
            + attr_bytes
        )
        end = struct.pack("<HHIII", 0x0103, 16, 24, 1, 0xFFFFFFFF) + struct.pack("<II", 0xFFFFFFFF, 0)
        total_size = 8 + len(pool) + len(start) + len(end)
        original_axml = struct.pack("<HHI", 0x0003, 8, total_size) + pool + start + end

        patched, report = patch_binary_xml_package_id(original_axml, ORIGINAL_PACKAGE, TARGET_PACKAGE)
        self.assertEqual(struct.unpack_from("<I", patched, 4)[0], len(patched))
        self.assertGreater(len(patched), len(original_axml))
        self.assertEqual(report["source_package_id"], ORIGINAL_PACKAGE)
        self.assertEqual(report["target_package_id"], TARGET_PACKAGE)
        self.assertEqual(report["split_name"], "config.arm64_v8a")
        self.assertTrue(report["binary_manifest_only_changed"])
        restored, restored_report = patch_binary_xml_package_id(patched, TARGET_PACKAGE, ORIGINAL_PACKAGE)
        self.assertEqual(restored_report["split_name"], "config.arm64_v8a")
        self.assertEqual(struct.unpack_from("<I", restored, 4)[0], len(restored))

    def test_resources_table_patch_only_changes_fixed_package_name_field(self):
        old_name = ORIGINAL_PACKAGE.encode("utf-16le")
        package_name_field = old_name + b"\0" * (256 - len(old_name))
        package_chunk = (
            struct.pack("<HHII", 0x0200, 288, 288, 0x7F)
            + package_name_field
            + b"\0" * 20
        )
        table_size = 12 + len(package_chunk)
        table = struct.pack("<HHII", 0x0002, 12, table_size, 1) + package_chunk

        patched, report = patch_resources_table_package_name(table, ORIGINAL_PACKAGE, TARGET_PACKAGE)
        self.assertEqual(len(patched), len(table))
        self.assertEqual(patched[:12], table[:12])
        self.assertEqual(patched[12:24], table[12:24])
        self.assertEqual(patched[24 + 256:], table[24 + 256:])
        self.assertEqual(patched[24:24 + 256].decode("utf-16le").split("\0", 1)[0], TARGET_PACKAGE)
        self.assertEqual(report["package_chunks_updated"], 1)
        self.assertTrue(report["resource_ids_and_entries_not_rebuilt"])

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
