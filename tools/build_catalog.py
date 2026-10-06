#!/usr/bin/env python3
"""Builds app/src/main/assets/catalog.json (and image_credits.json) from tools/catalog-source.tsv.

Usage (from the repo root):  python tools/build_catalog.py
Fails loudly on duplicate ids, unknown categories, bad prices, or estimate flags outside
sports_music/mega, so the app's data validation test never sees broken content.
Item photos come from tools/images-manifest.json (written by tools/fetch_images.py).
"""
import json
import sys
from pathlib import Path

from catalog_source import ROOT, load_rows

ASSETS = ROOT / "app" / "src" / "main" / "assets"
MANIFEST = ROOT / "tools" / "images-manifest.json"


def main() -> int:
    rows, errors = load_rows()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1

    manifest = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {}
    items, credits = [], []
    for row in rows:
        entry = {
            "id": row["id"],
            "category": row["category"],
            "priceCents": row["cents"],
            "name": {"en": row["name_en"], "vi": row["name_vi"]},
            "icon": row["icon"],
        }
        if row["estimate"]:
            entry["estimate"] = True
        info = manifest.get(row["id"])
        image_path = ASSETS / "images" / f"{row['id']}.webp"
        if info and image_path.exists():
            entry["image"] = f"images/{row['id']}.webp"
            credits.append({
                "id": row["id"],
                "title": info["title"],
                "author": info["author"],
                "license": info["license"],
                "url": info["page"],
            })
        items.append(entry)

    (ASSETS / "catalog.json").write_text(
        "[\n  " + ",\n  ".join(json.dumps(e, ensure_ascii=False, separators=(",", ":")) for e in items) + "\n]\n",
        encoding="utf-8",
    )
    (ASSETS / "image_credits.json").write_text(
        "[\n  " + ",\n  ".join(json.dumps(c, ensure_ascii=False, separators=(",", ":")) for c in credits) + "\n]\n",
        encoding="utf-8",
    )
    print(f"wrote {len(items)} items ({len(credits)} with photos)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
