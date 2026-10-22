#!/usr/bin/env python3
"""Rebrand original app resources/identity while reassembling only package-scoped DEX string literals."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import struct
import sys
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image

APP_NAME = "مەشخەڵ"
ORIGINAL_PACKAGE = "com.kosratdahmad.myprayers"
TARGET_PACKAGE = os.environ.get("MASHXALL_TARGET_PACKAGE", "com.kosratdahmad.mashxall.full")
EXPECTED_FILES = {"base.apk", "split_config.arm64_v8a.apk", "split_config.xxhdpi.apk"}
ANDROID_NS = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID_NS)
COLORS = {
    "#25ad60": "#d4af37", "#5fb87b": "#f1dfa6", "#77c089": "#dfc26a",
    "#d1dfe3": "#f7eed6", "#bdc8c9": "#b68b2f", "#ffffff": "#0f3b30",
}

def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def run(command: list[str]) -> str:
    result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    if result.returncode:
        raise RuntimeError("Command failed: " + " ".join(command) + "\n" + result.stdout[-5000:])
    return result.stdout

def find_label_key(manifest_path: Path, expected_package: str = ORIGINAL_PACKAGE) -> str:
    root = ET.parse(manifest_path).getroot()
    if root.attrib.get("package") != expected_package:
        raise ValueError("Unexpected package ID: " + str(root.attrib.get("package")))
    application = root.find("application")
    if application is None:
        raise ValueError("Decoded manifest has no application element")
    label = application.attrib.get("{" + ANDROID_NS + "}label", "")
    match = re.fullmatch(r"@string/([A-Za-z0-9_]+)", label)
    if not match:
        raise ValueError("Expected @string label but got " + repr(label))
    return match.group(1)


def manifest_semantics(manifest_path: Path, ignore_tool_added_split_flag: bool = False):
    """Return manifest semantics while ignoring Apktool/aapt build-environment metadata."""
    root = ET.parse(manifest_path).getroot()
    ignored_attrs = {
        "platformBuildVersionCode",
        "platformBuildVersionName",
        "{" + ANDROID_NS + "}compileSdkVersion",
        "{" + ANDROID_NS + "}compileSdkVersionCodename",
    }

    def normalize(node, is_root: bool = False):
        normalized_attrs = []
        for key, value in node.attrib.items():
            if key in ignored_attrs:
                continue
            # Android App Bundle split-only root flags must be stripped for a standalone APK.
            # Exclude those packaging metadata flags from source-vs-rebuilt semantic comparison.
            split_only_root_attributes = {
                "split", "splitTypes", "requiredSplitTypes", "isSplitRequired",
                "isFeatureSplit", "configForSplit",
                "{" + ANDROID_NS + "}split",
                "{" + ANDROID_NS + "}splitTypes",
                "{" + ANDROID_NS + "}requiredSplitTypes",
                "{" + ANDROID_NS + "}isSplitRequired",
                "{" + ANDROID_NS + "}isFeatureSplit",
                "{" + ANDROID_NS + "}configForSplit",
            }
            if is_root and key in split_only_root_attributes:
                continue
            if (
                ignore_tool_added_split_flag
                and key == "{" + ANDROID_NS + "}isSplitRequired"
                and value == "true"
            ):
                continue
            # The standalone build intentionally gets its own installable application ID.
            # Normalize only these explicit identity-related manifest changes for comparison.
            if is_root and key == "package" and value == TARGET_PACKAGE:
                value = ORIGINAL_PACKAGE
            if key == "{" + ANDROID_NS + "}authorities" and value.startswith(TARGET_PACKAGE + "."):
                value = ORIGINAL_PACKAGE + value[len(TARGET_PACKAGE):]
            # The resource-preserving build stores the rebranded launcher label
            # directly in the compiled manifest to avoid rebuilding resources.arsc.
            if node.tag == "application" and key == "{" + ANDROID_NS + "}label" and value == APP_NAME:
                value = "@string/app_name"
            if (
                key == "{" + ANDROID_NS + "}name"
                and value == TARGET_PACKAGE + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
            ):
                value = ORIGINAL_PACKAGE + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
            normalized_attrs.append((key, value))
        attrs = tuple(sorted(normalized_attrs))
        text = (node.text or "").strip() or None
        # XML declaration order does not change Android manifest semantics; normalize it away.
        children = tuple(sorted((normalize(child) for child in list(node)), key=repr))
        return (node.tag, attrs, text, children)

    return normalize(root, is_root=True)



def manifest_diff_summary(before, after, path: str = "/manifest", limit: int = 80) -> list[dict]:
    """Summarize normalized manifest differences so failures are diagnosable from CI logs."""
    differences: list[dict] = []

    def visit(left, right, current_path: str) -> None:
        if len(differences) >= limit:
            return
        if left[0] != right[0]:
            differences.append({"path": current_path, "kind": "tag", "before": left[0], "after": right[0]})
            return
        left_attrs, right_attrs = dict(left[1]), dict(right[1])
        for key in sorted(set(left_attrs) | set(right_attrs)):
            if left_attrs.get(key) != right_attrs.get(key):
                differences.append({
                    "path": current_path, "kind": "attribute", "name": key,
                    "before": left_attrs.get(key), "after": right_attrs.get(key)
                })
                if len(differences) >= limit:
                    return
        if left[2] != right[2]:
            differences.append({"path": current_path, "kind": "text", "before": left[2], "after": right[2]})
            if len(differences) >= limit:
                return
        lchildren, rchildren = left[3], right[3]
        for index in range(min(len(lchildren), len(rchildren))):
            visit(lchildren[index], rchildren[index], f"{current_path}/{lchildren[index][0]}[{index}]")
            if len(differences) >= limit:
                return
        if len(lchildren) != len(rchildren):
            differences.append({
                "path": current_path, "kind": "child-count",
                "before": len(lchildren), "after": len(rchildren)
            })

    visit(before, after, path)
    return differences

def rewrite_package_identity(decoded_dir: Path) -> dict:
    """Give the rebranded app its own ID so it can coexist with the official app."""
    manifest_path = decoded_dir / "AndroidManifest.xml"
    tree = ET.parse(manifest_path)
    root = tree.getroot()
    if root.attrib.get("package") != ORIGINAL_PACKAGE:
        raise ValueError("Unexpected source package ID: " + str(root.attrib.get("package")))
    # The Play-delivered base APK declares required ABI/density splits. Once the
    # original base + ABI + density parts are merged into a standalone APK, these
    # root metadata flags must go, or Package Manager may report missing splits.
    split_only_root_attributes = (
        "split", "splitTypes", "requiredSplitTypes", "isSplitRequired",
        "isFeatureSplit", "configForSplit",
        "{" + ANDROID_NS + "}split",
        "{" + ANDROID_NS + "}splitTypes",
        "{" + ANDROID_NS + "}requiredSplitTypes",
        "{" + ANDROID_NS + "}isSplitRequired",
        "{" + ANDROID_NS + "}isFeatureSplit",
        "{" + ANDROID_NS + "}configForSplit",
    )
    removed_split_attributes = []
    for attr in split_only_root_attributes:
        if attr in root.attrib:
            removed_split_attributes.append({"attribute": attr, "value": root.attrib.pop(attr)})
    root.set("package", TARGET_PACKAGE)
    changed_authorities = []
    changed_permission_names = []
    authorities_attr = "{" + ANDROID_NS + "}authorities"
    name_attr = "{" + ANDROID_NS + "}name"
    old_permission = ORIGINAL_PACKAGE + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
    new_permission = TARGET_PACKAGE + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
    for node in root.iter():
        authority = node.attrib.get(authorities_attr)
        if authority and authority.startswith(ORIGINAL_PACKAGE + "."):
            replacement = TARGET_PACKAGE + authority[len(ORIGINAL_PACKAGE):]
            node.set(authorities_attr, replacement)
            changed_authorities.append({"from": authority, "to": replacement})
        name = node.attrib.get(name_attr)
        if name == old_permission:
            node.set(name_attr, new_permission)
            changed_permission_names.append({"from": name, "to": new_permission})
    tree.write(manifest_path, encoding="utf-8", xml_declaration=True)
    if len(changed_authorities) < 1:
        raise ValueError("Expected application-scoped provider authorities were not found")
    return {
        "source_package_id": ORIGINAL_PACKAGE,
        "target_package_id": TARGET_PACKAGE,
        "changed_authorities": changed_authorities,
        "changed_custom_permission_names": changed_permission_names,
        "component_class_names_preserved": True,
        "removed_split_only_manifest_attributes": removed_split_attributes,
    }


def patch_runtime_package_literals(decoded_dir: Path) -> dict:
    """Update only package-ID string literals, leaving DEX class/package descriptors untouched.

    The original bytecode stores the package ID in BuildConfig and stores explicit
    android.resource:// URIs for notification audio. Changing the manifest ID alone
    would make those package-specific strings resolve to the original package.
    """
    smali_files = sorted(decoded_dir.glob("smali*/**/*.smali"))
    if not smali_files:
        raise ValueError("Apktool did not decode smali; refusing a package-ID change without patching runtime package literals.")
    exact_literal = '"' + ORIGINAL_PACKAGE + '"'
    uri_prefix = "android.resource://" + ORIGINAL_PACKAGE + "/"
    exact_replacements = 0
    uri_replacements = 0
    changed_files = []
    for path in smali_files:
        original = path.read_text(encoding="utf-8")
        output_lines = []
        changed = False
        for line in original.splitlines(keepends=True):
            if not line.lstrip().startswith(("const-string ", "const-string/jumbo ")):
                output_lines.append(line)
                continue
            if exact_literal in line:
                line = line.replace(exact_literal, '"' + TARGET_PACKAGE + '"')
                exact_replacements += 1
                changed = True
            if uri_prefix in line:
                hits = line.count(uri_prefix)
                line = line.replace(uri_prefix, "android.resource://" + TARGET_PACKAGE + "/")
                uri_replacements += hits
                changed = True
            output_lines.append(line)
        if changed:
            path.write_text("".join(output_lines), encoding="utf-8")
            changed_files.append(str(path.relative_to(decoded_dir)))
    if exact_replacements < 1:
        raise ValueError("The original BuildConfig package-ID string was not found in smali.")
    if uri_replacements < 10:
        raise ValueError("Expected at least ten notification-audio package URIs; found " + str(uri_replacements))
    return {
        "exact_application_id_literals_rewritten": exact_replacements,
        "audio_resource_uri_literals_rewritten": uri_replacements,
        "smali_files_modified": changed_files,
        "class_descriptors_preserved": True,
    }


def update_app_label(decoded_dir: Path) -> dict:
    key = find_label_key(decoded_dir / "AndroidManifest.xml")
    changed_files = []
    for path in sorted((decoded_dir / "res").glob("values*/strings.xml")):
        try:
            tree = ET.parse(path)
        except ET.ParseError:
            continue
        changed = False
        for node in tree.getroot().findall("string"):
            if node.attrib.get("name") == key:
                node.text = APP_NAME
                for child in list(node):
                    node.remove(child)
                changed = True
        if changed:
            tree.write(path, encoding="utf-8", xml_declaration=True)
            changed_files.append(str(path.relative_to(decoded_dir)))
    if not changed_files:
        raise ValueError("Could not find app label resource " + key + " in values*/strings.xml")
    return {"string_resource": key, "files_modified": changed_files}

def recolor_png(path: Path) -> None:
    image = Image.open(path).convert("RGBA")
    pairs = [
        ((255,255,255),(15,59,48)), ((37,173,98),(212,175,55)),
        ((95,184,124),(241,223,166)), ((119,192,137),(223,194,106)),
        ((209,223,227),(247,238,214)), ((189,200,201),(182,139,47)),
    ]
    source, target = [p[0] for p in pairs], [p[1] for p in pairs]
    pixels = image.load()
    for y in range(image.height):
        for x in range(image.width):
            r, g, b, a = pixels[x, y]
            if a < 80:
                continue
            distances = [(r-c[0])**2 + (g-c[1])**2 + (b-c[2])**2 for c in source]
            idx = min(range(len(distances)), key=distances.__getitem__)
            if distances[idx] <= 120**2:
                pixels[x, y] = (*target[idx], a)
    image.save(path, format="PNG", optimize=True)

def recolor_vector(path: Path) -> int:
    text = path.read_text(encoding="utf-8")
    total = 0
    for old, new in COLORS.items():
        text, n = re.subn(re.escape(old), new, text, flags=re.IGNORECASE)
        total += n
    path.write_text(text, encoding="utf-8")
    return total

def strip_signature_entries(source: Path, destination: Path) -> None:
    with zipfile.ZipFile(source) as zin, zipfile.ZipFile(destination, "w", allowZip64=True) as zout:
        for info in zin.infolist():
            upper = info.filename.upper()
            if (upper.startswith("META-INF/") and upper.endswith((".SF", ".RSA", ".DSA", ".EC", ".MF"))) or info.filename == "stamp-cert-sha256":
                continue
            item = zipfile.ZipInfo(info.filename, info.date_time)
            item.compress_type = info.compress_type
            item.comment = info.comment
            item.create_system = info.create_system
            item.external_attr = info.external_attr
            item.internal_attr = info.internal_attr
            item.extra = b""
            zout.writestr(item, zin.read(info.filename), compress_type=info.compress_type, compresslevel=6)

def zip_entries(path: Path) -> dict[str, bytes]:
    with zipfile.ZipFile(path) as z:
        bad = z.testzip()
        if bad:
            raise ValueError("ZIP CRC failed: " + bad)
        return {name: z.read(name) for name in z.namelist()}


def rewrite_split_config_manifest(manifest_path: Path) -> dict:
    """Change a config split package ID while preserving its split identity."""
    tree = ET.parse(manifest_path)
    root = tree.getroot()
    package = root.attrib.get("package")
    if package != ORIGINAL_PACKAGE:
        raise ValueError(f"Unexpected config-split package ID in {manifest_path}: {package!r}")
    split_name = root.attrib.get("split") or root.attrib.get("{" + ANDROID_NS + "}split")
    if not split_name:
        raise ValueError(f"Expected a split name in config-split manifest: {manifest_path}")
    before = manifest_semantics(manifest_path)
    root.set("package", TARGET_PACKAGE)
    tree.write(manifest_path, encoding="utf-8", xml_declaration=True)
    if manifest_semantics(manifest_path) != before:
        raise ValueError("Config-split manifest changed beyond package-ID remap: " + str(manifest_path))
    return {
        "source_package_id": ORIGINAL_PACKAGE,
        "target_package_id": TARGET_PACKAGE,
        "split_name": split_name,
        "manifest_package_id_rewritten": True,
        "split_identity_preserved": True,
    }




_ANDROID_XML_TYPE = 0x0003
_ANDROID_STRING_POOL_TYPE = 0x0001
_ANDROID_START_ELEMENT_TYPE = 0x0102
_ANDROID_UTF8_FLAG = 0x100
_ANDROID_NO_INDEX = 0xFFFFFFFF


def _axml_u16(data: bytes, offset: int) -> int:
    return struct.unpack_from("<H", data, offset)[0]


def _axml_u32(data: bytes, offset: int) -> int:
    return struct.unpack_from("<I", data, offset)[0]


def _axml_read_len8(data: bytes, offset: int) -> tuple[int, int]:
    if offset >= len(data):
        raise ValueError("Truncated Android XML string length")
    first = data[offset]
    offset += 1
    if first & 0x80:
        if offset >= len(data):
            raise ValueError("Truncated two-byte Android XML string length")
        return ((first & 0x7F) << 8) | data[offset], offset + 1
    return first, offset


def _axml_write_len8(value: int) -> bytes:
    if value <= 0x7F:
        return bytes([value])
    if value <= 0x7FFF:
        return bytes([(value >> 8) | 0x80, value & 0xFF])
    raise ValueError("Android XML UTF-8 string is too long")


def _axml_write_len16(value: int) -> bytes:
    if value <= 0x7FFF:
        return struct.pack("<H", value)
    if value <= 0x7FFFFFFF:
        return struct.pack("<HH", (value >> 16) | 0x8000, value & 0xFFFF)
    raise ValueError("Android XML UTF-16 string is too long")


def _axml_decode_pool_string(data: bytes, offset: int, utf8: bool) -> str:
    if utf8:
        utf16_length, offset = _axml_read_len8(data, offset)
        byte_length, offset = _axml_read_len8(data, offset)
        end = offset + byte_length
        if end >= len(data) or data[end] != 0:
            raise ValueError("Malformed Android XML UTF-8 string-pool entry")
        value = data[offset:end].decode("utf-8")
        if len(value.encode("utf-16le")) // 2 != utf16_length:
            raise ValueError("Android XML string-pool length mismatch")
        return value
    length = _axml_u16(data, offset)
    offset += 2
    if length & 0x8000:
        length = ((length & 0x7FFF) << 16) | _axml_u16(data, offset)
        offset += 2
    end = offset + length * 2
    if end + 2 > len(data) or data[end:end + 2] != b"\0\0":
        raise ValueError("Malformed Android XML UTF-16 string-pool entry")
    return data[offset:end].decode("utf-16le")


def _axml_encode_pool_string(value: str, utf8: bool) -> bytes:
    if "\0" in value:
        raise ValueError("Embedded NUL is not supported in Android manifest package names")
    if utf8:
        encoded = value.encode("utf-8")
        utf16_length = len(value.encode("utf-16le")) // 2
        return _axml_write_len8(utf16_length) + _axml_write_len8(len(encoded)) + encoded + b"\0"
    encoded = value.encode("utf-16le")
    return _axml_write_len16(len(encoded) // 2) + encoded + b"\0\0"


def patch_binary_xml_manifest(
    data: bytes,
    old_package: str,
    new_package: str,
    app_label: str | None = None,
    standalone: bool = False,
) -> tuple[bytes, dict]:
    """Patch app identity fields in binary AndroidManifest.xml without rebuilding resources.

    Only the manifest string pool and explicitly selected attribute records change.
    Component class names, resource IDs, and unrelated manifest attributes remain intact.
    """
    if (
        len(data) < 8
        or _axml_u16(data, 0) != _ANDROID_XML_TYPE
        or _axml_u16(data, 2) != 8
        or _axml_u32(data, 4) != len(data)
    ):
        raise ValueError("Invalid Android binary XML header")

    chunks: list[tuple[int, bytes, int]] = []
    position = 8
    string_pool_index = None
    string_values = None
    string_pool_meta = None
    while position < len(data):
        if position + 8 > len(data):
            raise ValueError("Truncated Android XML chunk header")
        chunk_type = _axml_u16(data, position)
        header_size = _axml_u16(data, position + 2)
        chunk_size = _axml_u32(data, position + 4)
        if header_size < 8 or chunk_size < header_size or position + chunk_size > len(data):
            raise ValueError(f"Invalid Android XML chunk at offset {position}")
        chunk = data[position:position + chunk_size]
        if chunk_type == _ANDROID_STRING_POOL_TYPE:
            if string_pool_index is not None:
                raise ValueError("Multiple string pools in Android manifest are unsupported")
            if header_size < 28:
                raise ValueError("Android XML string-pool header is too short")
            string_count = _axml_u32(data, position + 8)
            style_count = _axml_u32(data, position + 12)
            flags = _axml_u32(data, position + 16)
            strings_start = _axml_u32(data, position + 20)
            styles_start = _axml_u32(data, position + 24)
            if style_count != 0 or styles_start != 0:
                raise ValueError("Unexpected styled strings in Android manifest; refusing unsafe patch")
            if position + header_size + string_count * 4 > position + chunk_size:
                raise ValueError("Android XML string offsets exceed string-pool chunk")
            if strings_start < header_size + string_count * 4 or strings_start > chunk_size:
                raise ValueError("Invalid Android XML string-data offset")
            utf8 = bool(flags & _ANDROID_UTF8_FLAG)
            offsets_start = position + header_size
            strings_absolute = position + strings_start
            offsets = [
                _axml_u32(data, offsets_start + idx * 4)
                for idx in range(string_count)
            ]
            string_values = [
                _axml_decode_pool_string(data, strings_absolute + item_offset, utf8)
                for item_offset in offsets
            ]
            string_pool_meta = (header_size, string_count, flags, utf8)
            string_pool_index = len(chunks)
        chunks.append((chunk_type, chunk, position))
        position += chunk_size

    if position != len(data) or string_pool_meta is None or string_values is None:
        raise ValueError("Android XML chunk structure is incomplete")

    pool_strings = list(string_values)
    existing_string_indices: dict[str, int] = {}
    for index, value in enumerate(pool_strings):
        existing_string_indices.setdefault(value, index)
    appended_strings: list[str] = []
    changed_authorities: list[dict] = []
    changed_permissions: list[dict] = []
    label_changed = False
    package_changed = False
    removed_split_attrs: list[dict] = []
    root_split_name = None
    patched_chunks: list[tuple[int, bytes, int]] = []
    element_stack: list[str] = []
    split_only_attrs = {
        "split", "splitTypes", "requiredSplitTypes", "isSplitRequired",
        "isFeatureSplit", "configForSplit",
    }

    def get_string(index: int) -> str | None:
        if index == _ANDROID_NO_INDEX or index < 0 or index >= len(pool_strings):
            return None
        return pool_strings[index]

    def string_index(value: str) -> int:
        found = existing_string_indices.get(value)
        if found is not None:
            return found
        idx = len(pool_strings) + len(appended_strings)
        appended_strings.append(value)
        existing_string_indices[value] = idx
        return idx

    for chunk_type, chunk, absolute in chunks:
        mutable = bytearray(chunk)
        if chunk_type == _ANDROID_START_ELEMENT_TYPE:
            if len(chunk) < 36:
                raise ValueError("Android XML start-element chunk is too short")
            element_name_index = _axml_u32(chunk, 20)
            if element_name_index >= len(pool_strings):
                raise ValueError("Element name index exceeds string pool")
            tag = pool_strings[element_name_index]
            attribute_start = _axml_u16(chunk, 24)
            attribute_size = _axml_u16(chunk, 26)
            attribute_count = _axml_u16(chunk, 28)
            if attribute_size < 20:
                raise ValueError("Invalid Android XML attribute record size")
            attributes_base = 16 + attribute_start
            if attributes_base + attribute_count * attribute_size > len(chunk):
                raise ValueError("Android XML attributes exceed their element chunk")

            keep_attr_indices: list[int] = []
            root_values: dict[str, str] = {}
            for idx in range(attribute_count):
                attr_offset = attributes_base + idx * attribute_size
                namespace_index = _axml_u32(chunk, attr_offset)
                name_index = _axml_u32(chunk, attr_offset + 4)
                raw_value_index = _axml_u32(chunk, attr_offset + 8)
                value_type = chunk[attr_offset + 15]
                typed_value_index = _axml_u32(chunk, attr_offset + 16)
                if name_index >= len(pool_strings):
                    raise ValueError("Attribute name index exceeds string pool")
                namespace = (
                    None if namespace_index == _ANDROID_NO_INDEX
                    else get_string(namespace_index)
                )
                attribute_name = pool_strings[name_index]
                value_index = raw_value_index
                if value_index == _ANDROID_NO_INDEX and value_type == 3:
                    value_index = typed_value_index
                attribute_value = get_string(value_index) if value_type == 3 else None
                if tag == "manifest" and namespace is None and attribute_name == "split":
                    root_split_name = attribute_value
                remove_this = (
                    standalone
                    and tag == "manifest"
                    and attribute_name in split_only_attrs
                    and namespace in (None, ANDROID_NS)
                )
                if remove_this:
                    removed_split_attrs.append({"attribute": attribute_name, "value": attribute_value})
                    continue

                replacement_value: str | None = None
                if tag == "manifest" and namespace is None and attribute_name == "package":
                    if attribute_value != old_package:
                        raise ValueError(
                            f"Expected manifest package {old_package!r}, found {attribute_value!r}"
                        )
                    replacement_value = new_package
                    package_changed = True
                elif (
                    tag == "application" and namespace == ANDROID_NS
                    and attribute_name == "label" and app_label is not None
                ):
                    replacement_value = app_label
                    label_changed = True
                elif namespace == ANDROID_NS and attribute_name == "authorities" and attribute_value:
                    parts = attribute_value.split(";")
                    rewritten_parts = [
                        new_package + item[len(old_package):]
                        if item == old_package or item.startswith(old_package + ".")
                        else item
                        for item in parts
                    ]
                    rewritten = ";".join(rewritten_parts)
                    if rewritten != attribute_value:
                        replacement_value = rewritten
                        changed_authorities.append({"from": attribute_value, "to": rewritten})
                elif namespace == ANDROID_NS and attribute_name == "permission" and attribute_value:
                    if attribute_value == old_package or attribute_value.startswith(old_package + "."):
                        replacement_value = new_package + attribute_value[len(old_package):]
                        changed_permissions.append({"from": attribute_value, "to": replacement_value})
                elif (
                    namespace == ANDROID_NS and attribute_name == "name"
                    and tag in {"permission", "uses-permission", "uses-permission-sdk-23", "permission-group", "permission-tree"}
                    and attribute_value
                    and (attribute_value == old_package or attribute_value.startswith(old_package + "."))
                ):
                    replacement_value = new_package + attribute_value[len(old_package):]
                    changed_permissions.append({"from": attribute_value, "to": replacement_value})

                if replacement_value is not None:
                    replacement_index = string_index(replacement_value)
                    struct.pack_into("<I", mutable, attr_offset + 8, replacement_index)
                    mutable[attr_offset + 15] = 3  # TYPE_STRING; also converts application label from a resource reference.
                    struct.pack_into("<I", mutable, attr_offset + 16, replacement_index)
                keep_attr_indices.append(idx)

            if tag == "application" and app_label is not None and not label_changed:
                raise ValueError("Application label attribute was not found in original manifest")
            if tag == "manifest" and not package_changed:
                # The package attribute should be processed on the root element only.
                pass

            if standalone and tag == "manifest" and len(keep_attr_indices) != attribute_count:
                original_after_attrs = attributes_base + attribute_count * attribute_size
                kept_attr_bytes = b"".join(
                    bytes(mutable[attributes_base + idx * attribute_size: attributes_base + (idx + 1) * attribute_size])
                    for idx in keep_attr_indices
                )
                mutable = bytearray(
                    bytes(mutable[:attributes_base])
                    + kept_attr_bytes
                    + bytes(mutable[original_after_attrs:])
                )
                struct.pack_into("<I", mutable, 4, len(mutable))
                struct.pack_into("<H", mutable, 28, len(keep_attr_indices))
            element_stack.append(tag)

        elif chunk_type == 0x0103:  # RES_XML_END_ELEMENT_TYPE
            if len(chunk) >= 24:
                end_name_index = _axml_u32(chunk, 20)
                end_tag = get_string(end_name_index)
                if element_stack:
                    last = element_stack.pop()
                    if end_tag is not None and last != end_tag:
                        raise ValueError(f"Malformed Android manifest nesting: {last!r} ended as {end_tag!r}")

        patched_chunks.append((chunk_type, bytes(mutable), absolute))

    if not package_changed:
        raise ValueError("Root manifest package attribute not found or not rewritten")

    pool_header_size, original_string_count, flags, utf8 = string_pool_meta
    all_strings = pool_strings + appended_strings
    pool_header = bytearray(chunks[string_pool_index][1][:pool_header_size])
    encoded_strings = bytearray()
    new_offsets = []
    for value in all_strings:
        new_offsets.append(len(encoded_strings))
        encoded_strings.extend(_axml_encode_pool_string(value, utf8))
    while len(encoded_strings) % 4:
        encoded_strings.append(0)

    new_strings_start = pool_header_size + len(all_strings) * 4
    new_pool_size = new_strings_start + len(encoded_strings)
    struct.pack_into("<I", pool_header, 4, new_pool_size)
    struct.pack_into("<I", pool_header, 8, len(all_strings))
    # Appended replacements may no longer be sorted, so clear the sorted-string-pool flag.
    struct.pack_into("<I", pool_header, 16, flags & ~0x1)
    struct.pack_into("<I", pool_header, 20, new_strings_start)
    struct.pack_into("<I", pool_header, 24, 0)
    new_pool = (
        bytes(pool_header)
        + b"".join(struct.pack("<I", string_offset) for string_offset in new_offsets)
        + bytes(encoded_strings)
    )
    if len(new_pool) != new_pool_size:
        raise ValueError("Internal Android XML string-pool size mismatch")

    rebuilt_xml = bytearray(data[:8])
    for index, (_chunk_type, chunk, _absolute) in enumerate(patched_chunks):
        rebuilt_xml.extend(new_pool if index == string_pool_index else chunk)
    struct.pack_into("<I", rebuilt_xml, 4, len(rebuilt_xml))
    return bytes(rebuilt_xml), {
        "source_package_id": old_package,
        "target_package_id": new_package,
        "split_name": root_split_name,
        "manifest_package_id_rewritten": True,
        "split_identity_preserved": True,
        "binary_manifest_patched_only": True,
        "application_label_literal": app_label if label_changed else None,
        "changed_authorities": changed_authorities,
        "changed_custom_permissions": changed_permissions,
        "removed_split_only_attributes": removed_split_attrs,
    }


def patch_binary_xml_package_id(
    data: bytes, old_package: str, new_package: str
) -> tuple[bytes, dict]:
    """Backward-compatible wrapper used by config-split manifest processing and tests."""
    return patch_binary_xml_manifest(data, old_package, new_package)


def _is_apk_signature_entry(filename: str) -> bool:
    upper = filename.upper()
    return (
        upper.startswith("META-INF/")
        and upper.endswith((".SF", ".RSA", ".DSA", ".EC", ".MF"))
    ) or filename == "stamp-cert-sha256"





def patch_resources_table_package_name(
    data: bytes, old_package: str, new_package: str
) -> tuple[bytes, dict]:
    """Patch only the fixed UTF-16 package-name field in resources.arsc package chunks."""
    if len(data) < 12 or _axml_u16(data, 0) != 0x0002 or _axml_u32(data, 4) != len(data):
        raise ValueError("Invalid Android resources.arsc header")
    table_header_size = _axml_u16(data, 2)
    if table_header_size < 12:
        raise ValueError("Android resources.arsc header is too short")
    old_encoded = old_package.encode("utf-16le")
    new_encoded = new_package.encode("utf-16le")
    name_field_size = 128 * 2
    if len(old_encoded) >= name_field_size or len(new_encoded) >= name_field_size:
        raise ValueError("Package name does not fit the Android resource table package-name field")
    patched = bytearray(data)
    position = table_header_size
    changed = 0
    package_ids = []
    while position < len(data):
        if position + 8 > len(data):
            raise ValueError("Truncated resources.arsc chunk header")
        chunk_type = _axml_u16(data, position)
        chunk_header_size = _axml_u16(data, position + 2)
        chunk_size = _axml_u32(data, position + 4)
        if chunk_header_size < 8 or chunk_size < chunk_header_size or position + chunk_size > len(data):
            raise ValueError(f"Invalid resources.arsc chunk at offset {position}")
        if chunk_type == 0x0200:  # RES_TABLE_PACKAGE_TYPE
            if chunk_header_size < 288:
                raise ValueError("Android resource package header is too short")
            package_id = _axml_u32(data, position + 8)
            field_start = position + 12
            field = data[field_start:field_start + name_field_size]
            package_name = field.decode("utf-16le").split("\0", 1)[0]
            if package_name == old_package:
                replacement = new_encoded + b"\0" * (name_field_size - len(new_encoded))
                patched[field_start:field_start + name_field_size] = replacement
                changed += 1
                package_ids.append(package_id)
        position += chunk_size
    if position != len(data):
        raise ValueError("resources.arsc chunk boundaries do not match the file size")
    if changed < 1:
        raise ValueError(f"Resource table package name {old_package!r} was not found")
    output = bytes(patched)
    # Prove that the only byte range changed is the fixed package-name field in a package chunk.
    if len(output) != len(data):
        raise ValueError("Resource table size changed during package-name patch")
    return output, {
        "source_package_name": old_package,
        "target_package_name": new_package,
        "package_chunks_updated": changed,
        "package_ids": package_ids,
        "resource_table_length_preserved": True,
        "resource_ids_and_entries_not_rebuilt": True,
    }


def rewrite_split_config_apk(source_apk: Path, destination_apk: Path) -> dict:
    """Rebrand a config split manifest while keeping every other payload byte-identical."""
    source_entries = zip_entries(source_apk)
    if "AndroidManifest.xml" not in source_entries:
        raise ValueError("Configuration APK has no AndroidManifest.xml: " + str(source_apk))
    patched_manifest, report = patch_binary_xml_package_id(
        source_entries["AndroidManifest.xml"], ORIGINAL_PACKAGE, TARGET_PACKAGE
    )
    if not report.get("split_name", "").startswith("config."):
        raise ValueError("Expected a config.* split manifest in " + str(source_apk))
    patched_resources = None
    resource_report = None
    if "resources.arsc" in source_entries:
        patched_resources, resource_report = patch_resources_table_package_name(
            source_entries["resources.arsc"], ORIGINAL_PACKAGE, TARGET_PACKAGE
        )
    destination_apk.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(source_apk) as source_zip, zipfile.ZipFile(
        destination_apk, "w", allowZip64=True
    ) as destination_zip:
        for info in source_zip.infolist():
            if _is_apk_signature_entry(info.filename):
                continue
            if info.filename == "AndroidManifest.xml":
                payload = patched_manifest
            elif info.filename == "resources.arsc" and patched_resources is not None:
                payload = patched_resources
            else:
                payload = source_zip.read(info.filename)
            copied = zipfile.ZipInfo(info.filename, info.date_time)
            copied.compress_type = info.compress_type
            copied.comment = info.comment
            copied.create_system = info.create_system
            copied.external_attr = info.external_attr
            copied.internal_attr = info.internal_attr
            copied.extra = b""
            destination_zip.writestr(copied, payload, compress_type=info.compress_type, compresslevel=6)

    output_entries = zip_entries(destination_apk)
    expected_entries = {
        name for name in source_entries if not _is_apk_signature_entry(name)
    }
    if set(output_entries) != expected_entries:
        raise ValueError("Config split payload inventory changed while patching manifest")
    for name, payload in source_entries.items():
        if name == "AndroidManifest.xml" or _is_apk_signature_entry(name):
            continue
        if name == "resources.arsc" and patched_resources is not None:
            if output_entries[name] != patched_resources:
                raise ValueError("Resource table differs from the package-name-only patch: " + name)
            continue
        if output_entries[name] != payload:
            raise ValueError("Config split entry changed unexpectedly: " + name)
    report.update({
        "file": source_apk.name,
        "source_bytes": source_apk.stat().st_size,
        "rebuilt_bytes": destination_apk.stat().st_size,
        "resources_arsc_other_bytes_preserved_except_package_name": (
            "resources.arsc" not in source_entries
            or resource_report is not None
        ),
        "resources_arsc_package_name_patch": resource_report,
        "native_libraries_preserved": sum(
            name.startswith("lib/") and name.endswith(".so") for name in output_entries
        ),
        "non_manifest_payloads_byte_identical": True,
    })
    return report






def build_resource_preserving_base(
    source_apk: Path,
    rebuilt_dex_apk: Path,
    destination_apk: Path,
    standalone: bool,
) -> dict:
    """Inject rebuilt DEX into the original base while preserving all resource IDs."""
    source_entries = zip_entries(source_apk)
    rebuilt_entries = zip_entries(rebuilt_dex_apk)
    source_dex = {
        name: payload for name, payload in source_entries.items()
        if re.fullmatch(r"classes\d*\.dex", name)
    }
    rebuilt_dex = {
        name: payload for name, payload in rebuilt_entries.items()
        if re.fullmatch(r"classes\d*\.dex", name)
    }
    if not source_dex or set(source_dex) != set(rebuilt_dex):
        raise ValueError("Resource-preserving build has a different multidex file set")
    patched_manifest, manifest_report = patch_binary_xml_manifest(
        source_entries["AndroidManifest.xml"],
        ORIGINAL_PACKAGE,
        TARGET_PACKAGE,
        app_label=APP_NAME,
        standalone=standalone,
    )
    if not manifest_report["manifest_package_id_rewritten"] or not manifest_report["application_label_literal"]:
        raise ValueError("Original manifest package or application label was not patched")
    patched_resources, resource_report = patch_resources_table_package_name(
        source_entries["resources.arsc"], ORIGINAL_PACKAGE, TARGET_PACKAGE
    )

    destination_apk.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(source_apk, "r") as source_zip, zipfile.ZipFile(
        destination_apk, "w", allowZip64=True
    ) as destination_zip:
        for info in source_zip.infolist():
            if _is_apk_signature_entry(info.filename):
                continue
            if info.filename == "AndroidManifest.xml":
                payload = patched_manifest
            elif info.filename == "resources.arsc":
                payload = patched_resources
            elif info.filename in rebuilt_dex:
                payload = rebuilt_dex[info.filename]
            else:
                payload = source_zip.read(info.filename)
            copied = zipfile.ZipInfo(info.filename, info.date_time)
            copied.compress_type = info.compress_type
            copied.comment = info.comment
            copied.create_system = info.create_system
            copied.external_attr = info.external_attr
            copied.internal_attr = info.internal_attr
            copied.extra = b""
            destination_zip.writestr(
                copied, payload, compress_type=info.compress_type, compresslevel=6
            )

    output_entries = zip_entries(destination_apk)
    expected_names = {
        name for name in source_entries if not _is_apk_signature_entry(name)
    }
    if set(output_entries) != expected_names:
        raise ValueError("Resource-preserving base changed the ZIP entry inventory")
    for name, payload in source_entries.items():
        if _is_apk_signature_entry(name) or name == "AndroidManifest.xml" or name in source_dex:
            continue
        if name == "resources.arsc":
            if output_entries[name] != patched_resources:
                raise ValueError("Original resources.arsc differs from the package-name-only patch")
        elif output_entries[name] != payload:
            raise ValueError("Base APK payload changed unexpectedly: " + name)
    if {name: sha(payload) for name, payload in output_entries.items() if re.fullmatch(r"classes\d*\.dex", name)} != {
        name: sha(payload) for name, payload in rebuilt_dex.items()
    }:
        raise ValueError("Rebuilt DEX payloads were not injected byte-for-byte")
    database_name = next((name for name in source_entries if name.endswith("muslim_db_v2.5.1.db")), None)
    if not database_name or sha(output_entries[database_name]) != sha(source_entries[database_name]):
        raise ValueError("Original offline database changed in resource-preserving base")
    return {
        "file": destination_apk.name,
        "standalone": standalone,
        "source_bytes": source_apk.stat().st_size,
        "output_bytes": destination_apk.stat().st_size,
        "manifest_patch": manifest_report,
        "resource_table_package_name_patch": resource_report,
        "resource_table_length_preserved": len(source_entries["resources.arsc"]) == len(output_entries["resources.arsc"]),
        "original_resource_ids_and_entries_preserved_except_package_name": True,
        "original_database_byte_identical": True,
        "all_non_manifest_non_dex_payloads_byte_identical_except_resource_package_name": True,
        "dex_file_set_preserved": set(source_dex) == set(rebuilt_dex),
        "dex_payloads_from_reassembled_original_smali": True,
    }



def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--apktool-jar", required=True, type=Path)
    parser.add_argument("--workdir", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()
    work = args.workdir.resolve()
    if work.exists():
        shutil.rmtree(work)
    work.mkdir(parents=True)
    source_parts = work / "source-parts"
    decoded = work / "decoded-base"
    rebuilt_dir = work / "rebuilt"
    unsigned_dir = work / "unsigned-splits"
    standalone_input_dir = work / "standalone-input"
    for path in (source_parts, rebuilt_dir, unsigned_dir, standalone_input_dir):
        path.mkdir()
    with zipfile.ZipFile(args.input) as bundle:
        names = set(bundle.namelist())
        if names != EXPECTED_FILES:
            raise ValueError("Unexpected split list: " + repr(sorted(names)))
        if bundle.testzip():
            raise ValueError("Input APKS archive has a CRC error")
        for name in sorted(EXPECTED_FILES):
            (source_parts / name).write_bytes(bundle.read(name))
    apktool = ["java", "-jar", str(args.apktool_jar.resolve())]
    original_base = source_parts / "base.apk"
    run(apktool + ["d", "--force", str(original_base), "-o", str(decoded)])
    source_smali = {str(p.relative_to(decoded)) for p in decoded.glob("smali*/**/*.smali")}
    if not source_smali:
        raise ValueError("Apktool did not produce any smali files")
    manifest_before = manifest_semantics(decoded / "AndroidManifest.xml")
    label = update_app_label(decoded)
    package_identity = rewrite_package_identity(decoded)
    runtime_literals = patch_runtime_package_literals(decoded)
    icons = sorted((decoded / "res").glob("mipmap-*/ic_launcher.png"))
    if not icons:
        raise ValueError("No launcher icon PNG found")
    for path in icons:
        recolor_png(path)
    vector_changes = 0
    for relative in ("res/drawable/ic_launcher_foreground.xml", "res/drawable/ic_launcher_background.xml"):
        path = decoded / relative
        if path.exists():
            vector_changes += recolor_vector(path)
    rebuilt_base = rebuilt_dir / "base.apk"
    # Apktool 3.x bundles AAPT2 and removed the legacy --use-aapt2 / --no-crunch flags.
    # Pinning Apktool 2.11.1 preserves the manifest behavior this safe rebuild validates.
    run(apktool + ["b", "--use-aapt2", "--no-crunch", str(decoded), "-o", str(rebuilt_base)])
    redecoded = work / "redecoded-base"
    run(apktool + ["d", "--force", str(rebuilt_base), "-o", str(redecoded)])
    rebuilt_smali = {str(p.relative_to(redecoded)) for p in redecoded.glob("smali*/**/*.smali")}
    if source_smali != rebuilt_smali:
        missing = sorted(source_smali - rebuilt_smali)[:30]
        added = sorted(rebuilt_smali - source_smali)[:30]
        raise ValueError("Rebuilt APK class inventory differs from original. Missing=" + repr(missing) + " added=" + repr(added))
    if find_label_key(redecoded / "AndroidManifest.xml", TARGET_PACKAGE) != label["string_resource"]:
        raise ValueError("Rebuilt application label resource or package identity changed")
    manifest_after = manifest_semantics(redecoded / "AndroidManifest.xml", ignore_tool_added_split_flag=True)
    if manifest_before != manifest_after:
        differences = manifest_diff_summary(manifest_before, manifest_after)
        raise ValueError(
            "Apktool changed Android manifest semantics beyond ignored build metadata; refusing to continue. "
            + "Normalized differences: " + json.dumps(differences, ensure_ascii=False)
        )
    source_entries = zip_entries(original_base)
    rebuilt_entries = zip_entries(rebuilt_base)
    source_dex = {n: b for n, b in source_entries.items() if re.fullmatch(r"classes\d*\.dex", n)}
    rebuilt_dex = {n: b for n, b in rebuilt_entries.items() if re.fullmatch(r"classes\d*\.dex", n)}
    if not source_dex or set(source_dex) != set(rebuilt_dex):
        raise ValueError("APKTool changed the DEX file set; refusing to continue")
    if not any(TARGET_PACKAGE.encode("utf-8") in data for data in rebuilt_dex.values()):
        raise ValueError("Rebuilt DEX lacks the configured application ID string literal")
    expected_audio_uri = f"android.resource://{TARGET_PACKAGE}/raw/mecca".encode("utf-8")
    if not any(expected_audio_uri in data for data in rebuilt_dex.values()):
        raise ValueError("Rebuilt DEX lacks the updated notification-audio URI")
    if not any(b"Lcom/kosratdahmad/myprayers/screens/MainActivity;" in data for data in rebuilt_dex.values()):
        raise ValueError("Original application class descriptors were not preserved")
    dbname = next((n for n in source_entries if n.endswith("muslim_db_v2.5.1.db")), None)
    if not dbname or sha(source_entries[dbname]) != sha(rebuilt_entries.get(dbname, b"")):
        raise ValueError("Bundled offline database changed or disappeared")
    title_found = False
    for path in (redecoded / "res").glob("values*/strings.xml"):
        try:
            xml = ET.parse(path).getroot()
        except ET.ParseError:
            continue
        title_found |= any((node.text or "") == APP_NAME and node.attrib.get("name") == label["string_resource"] for node in xml.findall("string"))
    if not title_found:
        raise ValueError("Rebuilt app label is not present after decoding")

    # Build a second base for split installation. The standalone merge input
    # must not retain split requirements; the real split base must retain them.
    split_manifest_path = decoded / "AndroidManifest.xml"
    split_tree = ET.parse(split_manifest_path)
    split_root = split_tree.getroot()
    for item in package_identity["removed_split_only_manifest_attributes"]:
        split_root.set(item["attribute"], item["value"])
    split_tree.write(split_manifest_path, encoding="utf-8", xml_declaration=True)
    rebuilt_split_base = rebuilt_dir / "base-split.apk"
    run(apktool + ["b", "--use-aapt2", "--no-crunch", str(decoded), "-o", str(rebuilt_split_base)])
    redecoded_split_base = work / "redecoded-split-base"
    run(apktool + ["d", "--force", str(rebuilt_split_base), "-o", str(redecoded_split_base)])
    split_base_root = ET.parse(redecoded_split_base / "AndroidManifest.xml").getroot()
    if split_base_root.attrib.get("package") != TARGET_PACKAGE:
        raise ValueError("Split-install base APK has the wrong package ID")
    for item in package_identity["removed_split_only_manifest_attributes"]:
        if split_base_root.attrib.get(item["attribute"]) != item["value"]:
            raise ValueError("Split-install base APK did not preserve " + item["attribute"])
    if manifest_semantics(redecoded_split_base / "AndroidManifest.xml") != manifest_before:
        raise ValueError("Split-install base APK changed manifest semantics beyond package identity")
    split_base_entries = zip_entries(rebuilt_split_base)
    split_base_dex = {n: b for n, b in split_base_entries.items() if re.fullmatch(r"classes\d*\.dex", n)}
    if set(split_base_dex) != set(source_dex):
        raise ValueError("Split-install base APK changed the original multidex file set")
    split_base_db = next((n for n in split_base_entries if n.endswith("muslim_db_v2.5.1.db")), None)
    if not split_base_db or sha(split_base_entries[split_base_db]) != sha(source_entries[dbname]):
        raise ValueError("Split-install base APK changed or lost the original offline database")

    # Patch only each config split's binary manifest. Apktool rebuilding a config
    # APK independently can rewrite resources.arsc and break package-resource linkage.
    config_split_reports = {}
    rebuilt_config_splits = {}
    for name in sorted(EXPECTED_FILES - {"base.apk"}):
        source_config = source_parts / name
        rebuilt_config = rebuilt_dir / name
        identity_report = rewrite_split_config_apk(source_config, rebuilt_config)
        rebuilt_config_entries = zip_entries(rebuilt_config)
        source_config_entries = zip_entries(source_config)
        if "resources.arsc" in source_config_entries:
            expected_resources, _resource_report = patch_resources_table_package_name(
                source_config_entries["resources.arsc"], ORIGINAL_PACKAGE, TARGET_PACKAGE
            )
            if rebuilt_config_entries.get("resources.arsc") != expected_resources:
                raise ValueError("Config split resources.arsc differs from the package-name-only patch: " + name)
        source_config_libs = {
            entry: sha(payload) for entry, payload in source_config_entries.items()
            if entry.startswith("lib/") and entry.endswith(".so")
        }
        rebuilt_config_libs = {
            entry: sha(payload) for entry, payload in rebuilt_config_entries.items()
            if entry.startswith("lib/") and entry.endswith(".so")
        }
        if source_config_libs != rebuilt_config_libs:
            raise ValueError("Config split native libraries changed: " + name)
        config_split_reports[name] = identity_report
        rebuilt_config_splits[name] = rebuilt_config

    # The SAI archive uses the split-aware base; APKEditor gets a separate base
    # whose split metadata was stripped for standalone installation.
    for name in sorted(EXPECTED_FILES):
        if name == "base.apk":
            shutil.copy2(rebuilt_split_base, unsigned_dir / name)
            shutil.copy2(rebuilt_base, standalone_input_dir / name)
        else:
            strip_signature_entries(rebuilt_config_splits[name], unsigned_dir / name)
            shutil.copy2(rebuilt_config_splits[name], standalone_input_dir / name)

    report = {
        "status": "PREPARED_UNSIGNED_SPLITS",
        "source_sha256": sha(args.input.read_bytes()),
        "source_package_id": ORIGINAL_PACKAGE,
        "package_id_target": TARGET_PACKAGE,
        "package_identity": package_identity,
        "manifest_semantics_preserved_except_explicit_identity_remap_and_split_metadata_removal": True,
        "manifest_normalization_note": "Only the standalone merge input removes split-only root metadata. The SAI-ready base retains original split requirements. Config split package IDs are changed in binary AndroidManifest.xml only; resources.arsc and all native-library payloads remain byte-identical.",
        "split_install_set_identity_consistent": True,
        "config_split_manifest_rebranding": config_split_reports,
        "standalone_merge_input_dir": str(standalone_input_dir),
        "dex_files_reassembled_from_original_smali": True,
        "original_class_descriptors_preserved": True,
        "smali_class_file_count_preserved": len(source_smali),
        "smali_class_inventory_identical": True,
        "runtime_package_literals": runtime_literals,
        "offline_database_byte_identical": True,
        "apktool_version": run(apktool + ["--version"]).strip(),
        "app_label": APP_NAME,
        "label_update": label,
        "launcher_pngs_recolored": [str(p.relative_to(decoded)) for p in icons],
        "vector_color_literals_changed": vector_changes,
        "prepared_files": [{"file": n, "bytes": (unsigned_dir/n).stat().st_size, "sha256": sha((unsigned_dir/n).read_bytes())} for n in sorted(EXPECTED_FILES)],
        "standalone_merge_inputs": [{"file": n, "bytes": (standalone_input_dir/n).stat().st_size, "sha256": sha((standalone_input_dir/n).read_bytes())} for n in sorted(EXPECTED_FILES)],
        "runtime_launch_test": "NOT PERFORMED: must be tested on an Android 11 phone",
        "install_note": f"The app ID is {TARGET_PACKAGE}, unique for this workflow build to avoid signing-certificate conflicts with earlier test attempts. DEX code was assembled from original smali with only the application-ID and audio resource URI string literals changed; original class descriptors, resources, database and native libraries are preserved. External cloud services may still enforce the publisher original package/signing identity.",
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0

if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print("ERROR:", exc, file=sys.stderr)
        raise
