#!/usr/bin/env python3
"""Prepare a minimal app rebrand while preserving package identity, manifest semantics, and DEX bytes."""
from __future__ import annotations
import argparse
import hashlib
import json
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
TARGET_PACKAGE = "com.kosratdahmad.mashxall"
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
    source_parts, decoded, rebuilt_dir, unsigned_dir = (work / "source-parts", work / "decoded-base", work / "rebuilt", work / "unsigned-splits")
    for path in (source_parts, rebuilt_dir, unsigned_dir):
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
    run(apktool + ["d", "--no-src", "--force", str(original_base), "-o", str(decoded)])
    manifest_before = manifest_semantics(decoded / "AndroidManifest.xml")
    label = update_app_label(decoded)
    package_identity = rewrite_package_identity(decoded)
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
    run(apktool + ["d", "--no-src", "--force", str(rebuilt_base), "-o", str(redecoded)])
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
    source_dex = {n: sha(b) for n, b in source_entries.items() if re.fullmatch(r"classes\d*\.dex", n)}
    rebuilt_dex = {n: sha(b) for n, b in rebuilt_entries.items() if re.fullmatch(r"classes\d*\.dex", n)}
    if not source_dex or source_dex != rebuilt_dex:
        raise ValueError("APKTool changed or lost DEX bytes; refusing to continue")
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
    for name in sorted(EXPECTED_FILES):
        target = unsigned_dir / name
        if name == "base.apk":
            shutil.copy2(rebuilt_base, target)
        else:
            strip_signature_entries(source_parts / name, target)
    report = {
        "status": "PREPARED_UNSIGNED_SPLITS",
        "source_sha256": sha(args.input.read_bytes()),
        "source_package_id": ORIGINAL_PACKAGE,
        "package_id_target": TARGET_PACKAGE,
        "package_identity": package_identity,
        "manifest_semantics_preserved_except_explicit_identity_remap": True,
        "manifest_normalization_note": "Only Apktool-added android:isSplitRequired=true is ignored in the post-build comparison; the output is verified as the complete three-split set. All other manifest attributes and components remain strict.",
        "dex_files_byte_identical": True,
        "offline_database_byte_identical": True,
        "apktool_version": run(apktool + ["--version"]).strip(),
        "app_label": APP_NAME,
        "label_update": label,
        "launcher_pngs_recolored": [str(p.relative_to(decoded)) for p in icons],
        "vector_color_literals_changed": vector_changes,
        "prepared_files": [{"file": n, "bytes": (unsigned_dir/n).stat().st_size, "sha256": sha((unsigned_dir/n).read_bytes())} for n in sorted(EXPECTED_FILES)],
        "runtime_launch_test": "NOT PERFORMED: must be tested on an Android 11 phone",
        "install_note": "The app ID is com.kosratdahmad.mashxall, separate from the official com.kosratdahmad.myprayers app, so the official app can remain installed. This test build still uses a temporary signing certificate unless a stable signing key is configured; replacing a previously installed test build signed with a different certificate can require removing only that test build."
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
