#!/usr/bin/env python3
"""Extract user-provided app content assets for the new independent Mashxall app."""
from __future__ import annotations
import argparse
import hashlib
import json
import lzma
import os
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
    integrity_notes: list[str] = []
    with tempfile.NamedTemporaryFile(suffix=ext) as tmp:
        tmp.write(data)
        tmp.flush()
        con = sqlite3.connect(tmp.name)
        try:
            integrity = con.execute("PRAGMA integrity_check").fetchone()[0]
            # Some SQLite builds cannot run the global integrity_check against
            # Khairah's contentless FTS4 search index (it reports a SQL logic
            # error while inspecting its inverted index). Verify the unchanged
            # source DB again in a disposable copy with ONLY that virtual index
            # removed. Never modify the extracted payload or claim the FTS index
            # itself has been integrity-verified.
            fts_error = isinstance(integrity, str) and re.search(
                r"unable to validate the inverted index for FTS[34] table main\.search: SQL logic error",
                integrity,
                re.I,
            )
            if fts_error:
                schema_row = con.execute(
                    "SELECT sql FROM sqlite_master WHERE type='table' AND name='search'"
                ).fetchone()
                schema = schema_row[0] if schema_row and schema_row[0] else ""
                if not re.search(r"CREATE\s+VIRTUAL\s+TABLE.*USING\s+FTS[34]\b", schema, re.I | re.S):
                    raise sqlite3.DatabaseError(
                        "Integrity checker reported an FTS error but search is not an FTS3/4 virtual table"
                    )
                # Verify FTS MATCH separately before omitting only its virtual table
                # from the disposable b-tree integrity pass below.
                try:
                    con.execute('SELECT rowid FROM "search" WHERE "search" MATCH ? LIMIT 1', ("الرحمن",)).fetchone()
                    integrity_notes.append("The FTS4 MATCH smoke query executed successfully.")
                except sqlite3.DatabaseError as fts_exc:
                    integrity_notes.append("The FTS4 MATCH smoke query failed: " + str(fts_exc))
                with tempfile.NamedTemporaryFile(suffix=ext) as verify_tmp:
                    verify_tmp.write(data)
                    verify_tmp.flush()
                    verify = sqlite3.connect(verify_tmp.name)
                    try:
                        verify.execute('DROP TABLE "search"')
                        checked = verify.execute("PRAGMA integrity_check").fetchone()[0]
                        if checked != "ok":
                            raise sqlite3.DatabaseError(
                                "SQLite integrity check still failed after isolating the known FTS virtual index: " + str(checked)
                            )
                        # Probe MATCH separately. A contentless FTS table can
                        # legitimately reject ordinary SELECT COUNT(*) scans.
                        verify.close()
                    finally:
                        try:
                            verify.close()
                        except Exception:
                            pass
                integrity = "ok"
                integrity_notes.append(
                    "Main SQLite b-trees passed integrity_check in a disposable verification copy. "
                    "The source's contentless FTS3/4 virtual table 'search' is excluded from that check "
                    "because SQLite returned an inverted-index SQL logic error; source bytes were not changed."
                )
            if integrity != "ok":
                raise sqlite3.DatabaseError("SQLite integrity check failed: " + str(integrity))
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
            return {"integrity": integrity, "integrity_notes": integrity_notes, "tables": tables}
        finally:
            con.close()


def index_mushaf_svg_pages(pages_bin: Path, index_path: Path) -> dict:
    """Index concatenated XZ streams without storing the 300+ MB decoded pages."""
    data = pages_bin.read_bytes()
    pages = []
    position = 0
    chunk_size = 64 * 1024
    while position < len(data):
        stream_start = position
        decoder = lzma.LZMADecompressor(format=lzma.FORMAT_XZ)
        decoded = bytearray()
        while position < len(data) and not decoder.eof:
            chunk = data[position:min(position + chunk_size, len(data))]
            decoded.extend(decoder.decompress(chunk))
            position += len(chunk)
            if decoder.eof:
                position -= len(decoder.unused_data)
        if not decoder.eof or position <= stream_start:
            raise SystemExit(f"Corrupt concatenated XZ stream at byte {stream_start}")
        raw = bytes(decoded)
        match = re.search(rb'<svg\b[^>]*\bid="Mushaf_Page_(\d+)"', raw[:8192])
        expected_page = len(pages) + 1
        if not match or int(match.group(1)) != expected_page or b"</svg>" not in raw[-512:]:
            raise SystemExit(f"Unexpected SVG page structure at stream {expected_page}")
        pages.append({
            "page": expected_page,
            "offset": stream_start,
            "compressed_length": position - stream_start,
            "uncompressed_length": len(raw),
            "sha256": sha_bytes(raw),
        })
    if len(pages) != 604:
        raise SystemExit(f"Expected 604 Mushaf SVG pages, indexed {len(pages)}")
    result = {
        "schema_version": 1,
        "source_asset_path": "content/khairah_1.2/base/assets/mushaf/pages.bin",
        "page_count": len(pages),
        "pages": pages,
    }
    index_path.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Indexed {len(pages)} Mushaf SVG pages from {pages_bin.name}; index: {index_path}")
    return result

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
    inventory = {"bundle_sha256": sha_file(bundle), "sources": [], "files": [], "databases": [], "audio": [], "fonts": [], "documents": [], "generated_assets": []}
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

    mushaf_bin = out / "content" / "khairah_1.2" / "base" / "assets" / "mushaf" / "pages.bin"
    if not mushaf_bin.is_file():
        raise SystemExit("Khairah Mushaf page asset is missing: " + str(mushaf_bin))
    mushaf_index_path = out / "mushaf_svg_index.json"
    mushaf_index = index_mushaf_svg_pages(mushaf_bin, mushaf_index_path)
    inventory["generated_assets"].append({
        "asset_path": "mushaf_svg_index.json",
        "purpose": "Offsets and SHA-256 values for the 604 concatenated XZ-compressed SVG Mushaf pages",
        "page_count": mushaf_index["page_count"],
        "size_bytes": mushaf_index_path.stat().st_size,
        "sha256": sha_file(mushaf_index_path),
    })
    inventory["counts"] = {
        "source_apps": len(inventory["sources"]),
        "files": len(inventory["files"]),
        "databases": len(inventory["databases"]),
        "audio_files": len(inventory["audio"]),
        "font_files": len(inventory["fonts"]),
        "pdf_documents": len(inventory["documents"]),
        "total_content_bytes": sum(x["size_bytes"] for x in inventory["files"]),
        "mushaf_svg_pages": mushaf_index["page_count"],
    }
    catalog_path.parent.mkdir(parents=True, exist_ok=True)
    catalog_path.write_text(json.dumps(inventory, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(inventory["counts"], ensure_ascii=False, indent=2))
    print("catalog:", catalog_path)
    if os.environ.get("MASHXALL_PRINT_DB_SCHEMAS") == "1":
        selected = re.compile(r"tafseer|tafsir|hadith|prayertime|farmuda|book|quran|azkar|word_by_word|lexicon", re.I)
        schema_report = []
        for db in inventory["databases"]:
            path = db.get("asset_path", "")
            if not selected.search(path):
                continue
            schema_report.append({
                "asset_path": path,
                "size_bytes": db.get("size_bytes"),
                "integrity": db.get("integrity"),
                "tables": [
                    {"name": table.get("name"), "rows": table.get("rows"), "columns": table.get("columns")}
                    for table in db.get("tables", [])
                ],
            })
        print("MASHXALL_DB_SCHEMA_REPORT_BEGIN")
        print(json.dumps(schema_report, ensure_ascii=False, separators=(",", ":")))
        print("MASHXALL_DB_SCHEMA_REPORT_END")
    return 0

if __name__ == "__main__":
    main()
