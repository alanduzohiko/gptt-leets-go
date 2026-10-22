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
    split_base_dex = {n: b for n, b in split_base_entries.items() if re.fullmatch(r"classes\\d*\\.dex", n)}
    if set(split_base_dex) != set(source_dex):
        raise ValueError("Split-install base APK changed the original multidex file set")
    split_base_db = next((n for n in split_base_entries if n.endswith("muslim_db_v2.5.1.db")), None)
    if not split_base_db or sha(split_base_entries[split_base_db]) != sha(source_entries[dbname]):
        raise ValueError("Split-install base APK changed or lost the original offline database")

    # Rebuild both config APK manifests to use the same package ID as base.apk.
    # Reusing the original config manifests with a renamed base is not installable.
    config_split_reports = {}
    rebuilt_config_splits = {}
    for name in sorted(EXPECTED_FILES - {"base.apk"}):
        source_config = source_parts / name
        config_decoded = work / ("decoded-" + name.removesuffix(".apk"))
        run(apktool + ["d", "--force", str(source_config), "-o", str(config_decoded)])
        config_manifest = config_decoded / "AndroidManifest.xml"
        config_manifest_before = manifest_semantics(config_manifest)
        identity_report = rewrite_split_config_manifest(config_manifest)
        rebuilt_config = rebuilt_dir / name
        run(apktool + ["b", "--use-aapt2", "--no-crunch", str(config_decoded), "-o", str(rebuilt_config)])
        config_redecoded = work / ("redecoded-" + name.removesuffix(".apk"))
        run(apktool + ["d", "--force", str(rebuilt_config), "-o", str(config_redecoded)])
        config_root = ET.parse(config_redecoded / "AndroidManifest.xml").getroot()
        config_split_name = config_root.attrib.get("split") or config_root.attrib.get("{" + ANDROID_NS + "}split")
        if config_root.attrib.get("package") != TARGET_PACKAGE:
            raise ValueError("Rebuilt config split has the wrong package ID: " + name)
        if config_split_name != identity_report["split_name"]:
            raise ValueError("Rebuilt config split lost its split identity: " + name)
        if manifest_semantics(config_redecoded / "AndroidManifest.xml") != config_manifest_before:
            raise ValueError("Rebuilt config split changed manifest semantics beyond package ID: " + name)
        source_config_entries = zip_entries(source_config)
        rebuilt_config_entries = zip_entries(rebuilt_config)
        source_config_dex = {n: b for n, b in source_config_entries.items() if re.fullmatch(r"classes\\d*\\.dex", n)}
        rebuilt_config_dex = {n: b for n, b in rebuilt_config_entries.items() if re.fullmatch(r"classes\\d*\\.dex", n)}
        if {n: sha(b) for n, b in source_config_dex.items()} != {n: sha(b) for n, b in rebuilt_config_dex.items()}:
            raise ValueError("Config split DEX unexpectedly changed: " + name)
        source_config_libs = {n: sha(b) for n, b in source_config_entries.items() if n.startswith("lib/") and n.endswith(".so")}
        rebuilt_config_libs = {n: sha(b) for n, b in rebuilt_config_entries.items() if n.startswith("lib/") and n.endswith(".so")}
        if source_config_libs != rebuilt_config_libs:
            raise ValueError("Config split native libraries changed: " + name)
        config_split_reports[name] = {
            **identity_report,
            "source_bytes": source_config.stat().st_size,
            "rebuilt_bytes": rebuilt_config.stat().st_size,
            "dex_file_set_preserved": set(source_config_dex) == set(rebuilt_config_dex),
            "native_libraries_preserved": len(source_config_libs),
        }
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
        "manifest_normalization_note": "Only the standalone merge input removes split-only root metadata. The SAI-ready base retains the original split requirements, while both config split manifests are rebuilt to share the target package ID.",
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
