#!/usr/bin/env python3
"""Extract user-provided app content assets for the new independent Mashxall app."""
from __future__ import annotations
import argparse
import hashlib
import json
import re
import shutil
import sqlite3
import tempfile
import zipfile
from pathlib import Path, PurePosixPath

EXPECTED = {
    "salawati-3.1.3.apks": "bba56d408b552e31be43071af6a02dcdd66608b594b1fa10ca1a63fb51a504e3",
    "khairah-1.2.apks": "36b605314a66e5809c111565e7452f91578d4b0de3e009801118d40f1def85b8",
    "noor-tafsir-1.2.2.apks": "fa9259501abd4b1c7f870109a1ad49567badb27792510cee3d5ce2ee28e11ce8",
}
AUDIO = {".mp3", ".ogg", ".wav", ".m4a", ".flac", ".aac", ".opus", ".mid", ".midi"}
DBS = {".db", ".sqlite", ".sqlite3"}
IMAGES = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".avif"}

def sha_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def sha_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(4 * 1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()

def should_keep(path: str) -> bool:
    p = PurePosixPath(path)
    ext = p.suffix.lower()
    return (
        path.startswith("assets/")
        or path.startswith("res/raw/")
        or re.match(r"^res/font(?:-[^/]+)?/", path) is not None
        or (path.startswith(("res/drawable", "res/mipmap")) and ext in IMAGES)
        or (path.startswith("res/") and ext == ".pdf")
    )

def sqlite_summary(data: bytes, ext: str) -> dict:
    with tempfile.NamedTemporaryFile(suffix=ext) as tmp:
        tmp.write(data)
        tmp.flush()
        con = sqlite3.connect(tmp.name)
        try:
            integrity = con.execute("PRAGMA integrity_check").fetchone()[0]
            tables = []
            for (name,) in con.execute(
                "SELECT name FROM sqlite_master WHERE type='table' "
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'search' ORDER BY name"
            ).fetchall():
                try:
                    columns = [r[1] for r in con.execute(f'PRAGMA table_info("{name.replace(chr(34), chr(34)*2)}")')]
                    rows = con.execute(f'SELECT COUNT(*) FROM "{name.replace(chr(34), chr(34)*2)}"').fetchone()[0]
                    tables.append({"name": name, "rows": rows, "columns": columns})
                except sqlite3.DatabaseError as exc:
                    tables.append({"name": name, "error": str(exc)})
            return {"integrity": integrity, "tables": tables}
        finally:
            con.close()

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--catalog", required=True, type=Path)
    args = parser.parse_args()
    bundle = args.bundle.resolve()
    out = args.out.resolve()
    catalog_path = args.catalog.resolve()
    if out.exists():
        shutil.rmtree(out)
    out.mkdir(parents=True, exist_ok=True)
    if not zipfile.is_zipfile(bundle):
        raise SystemExit("Source bundle is not a valid ZIP archive")
    inventory = {"bundle_sha256": sha_file(bundle), "sources": [], "files": [], "databases": [], "audio": [], "fonts": [], "documents": []}
    with zipfile.ZipFile(bundle) as wrapper:
        bad = wrapper.testzip()
        if bad:
            raise SystemExit("Source bundle ZIP integrity failed at " + bad)
        names = set(wrapper.namelist())
        for filename, expected in EXPECTED.items():
            member = "input/" + filename
            if member not in names:
                raise SystemExit("Missing source archive: " + member)
            data = wrapper.read(member)
            actual = sha_bytes(data)
            if actual != expected:
                raise SystemExit(f"Source SHA-256 mismatch for {member}: {actual}")
            source_id = filename.replace(".apks", "").replace("-", "_")
            source = {"id": source_id, "filename": filename, "sha256": actual, "size_bytes": len(data), "splits": []}
            # Work from the exact original APK-set bytes that were validated above.
            with tempfile.NamedTemporaryFile(suffix=".apks") as temp_set:
                temp_set.write(data)
                temp_set.flush()
                with zipfile.ZipFile(temp_set.name) as apkset:
                    if apkset.testzip():
                        raise SystemExit("APK set container CRC failed for " + filename)
                    apk_names = [n for n in apkset.namelist() if n.endswith(".apk")]
                    if not apk_names or "base.apk" not in apk_names:
                        raise SystemExit("Unexpected split set in " + filename)
                    for apk_name in apk_names:
                        apk_bytes = apkset.read(apk_name)
                        source["splits"].append({"name": apk_name, "sha256": sha_bytes(apk_bytes), "size_bytes": len(apk_bytes)})
                        stem = Path(apk_name).stem
                        with tempfile.NamedTemporaryFile(suffix=".apk") as temp_apk:
                            temp_apk.write(apk_bytes)
                            temp_apk.flush()
                            with zipfile.ZipFile(temp_apk.name) as apk:
                                bad_apk = apk.testzip()
                                if bad_apk:
                                    raise SystemExit(f"APK ZIP CRC failed: {filename}/{apk_name}/{bad_apk}")
                                for info in apk.infolist():
                                    if info.is_dir() or not should_keep(info.filename):
                                        continue
                                    payload = apk.read(info.filename)
                                    relative = Path("content") / source_id / stem / Path(info.filename)
                                    target = out / relative
                                    target.parent.mkdir(parents=True, exist_ok=True)
                                    target.write_bytes(payload)
                                    suffix = target.suffix.lower()
                                    entry = {
                                        "source_id": source_id,
                                        "split": apk_name,
                                        "source_path": info.filename,
                                        "asset_path": relative.as_posix(),
                                        "size_bytes": len(payload),
                                        "sha256": sha_bytes(payload),
                                        "extension": suffix,
                                    }
                                    inventory["files"].append(entry)
                                    if suffix in DBS:
                                        summary = sqlite_summary(payload, suffix)
                                        dbrow = {**entry, **summary}
                                        inventory["databases"].append(dbrow)
                                        if summary["integrity"] != "ok":
                                            raise SystemExit(f"SQLite integrity failed: {relative}: {summary['integrity']}")
                                    elif suffix in AUDIO:
                                        inventory["audio"].append(entry)
                                    elif suffix in {".ttf", ".otf"}:
                                        inventory["fonts"].append(entry)
                                    elif suffix == ".pdf":
                                        inventory["documents"].append(entry)
                                    elif suffix in IMAGES:
                                        pass
            inventory["sources"].append(source)
    inventory["counts"] = {
        "source_apps": len(inventory["sources"]),
        "files": len(inventory["files"]),
        "databases": len(inventory["databases"]),
        "audio_files": len(inventory["audio"]),
        "font_files": len(inventory["fonts"]),
        "pdf_documents": len(inventory["documents"]),
        "total_content_bytes": sum(x["size_bytes"] for x in inventory["files"]),
    }
    catalog_path.parent.mkdir(parents=True, exist_ok=True)
    catalog_path.write_text(json.dumps(inventory, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(inventory["counts"], ensure_ascii=False, indent=2))
    print("catalog:", catalog_path)
    return 0

if __name__ == "__main__":
    main()
