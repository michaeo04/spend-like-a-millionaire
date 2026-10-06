#!/usr/bin/env python3
"""Builds labelled thumbnail sheets of the downloaded item photos for quick visual review.

Usage (repo root):  python tools/contact_sheet.py [out_dir [id1,id2,...]]   (default: .superpowers/sheets, all ids)
Writes sheet_01.png, sheet_02.png, ... with 30 photos each (6 columns x 5 rows), labelled by item id.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw

from catalog_source import ROOT, load_rows

TILE, LABEL, COLS, ROWS = 190, 34, 6, 5
IMAGES = ROOT / "app" / "src" / "main" / "assets" / "images"


def main() -> int:
    out_dir = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / ".superpowers" / "sheets"
    out_dir.mkdir(parents=True, exist_ok=True)
    for old in out_dir.glob("sheet_*.png"):
        old.unlink()
    rows, _ = load_rows()
    ids = [r["id"] for r in rows if (IMAGES / f"{r['id']}.webp").exists()]
    if len(sys.argv) > 2:  # optional: only these ids (comma separated)
        wanted = set(sys.argv[2].split(","))
        ids = [i for i in ids if i in wanted]
    per_sheet = COLS * ROWS
    for sheet_index in range(0, len(ids), per_sheet):
        chunk = ids[sheet_index:sheet_index + per_sheet]
        sheet = Image.new("RGB", (COLS * TILE, ROWS * (TILE + LABEL)), "white")
        draw = ImageDraw.Draw(sheet)
        for position, item_id in enumerate(chunk):
            col, row = position % COLS, position // COLS
            x, y = col * TILE, row * (TILE + LABEL)
            tile = Image.open(IMAGES / f"{item_id}.webp").convert("RGB").resize((TILE - 6, TILE - 6))
            sheet.paste(tile, (x + 3, y + 3))
            draw.text((x + 4, y + TILE), item_id[:28], fill="black")
        path = out_dir / f"sheet_{sheet_index // per_sheet + 1:02d}.png"
        sheet.save(path)
        print(path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
