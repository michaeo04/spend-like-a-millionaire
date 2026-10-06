#!/usr/bin/env python3
"""Builds app/src/main/assets/people.json from tools/people-source.tsv.

Usage (repo root):  python tools/build_people.py
Photos come from tools/fetch_images.py (app/src/main/assets/images/people/<id>.webp); a person without
a photo simply keeps the initial-letter avatar.
"""
import json
import sys

from catalog_source import ROOT, load_people_rows

ASSETS = ROOT / "app" / "src" / "main" / "assets"


def main() -> int:
    rows, errors = load_people_rows()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    people = []
    for row in rows:
        entry = {
            "id": row["id"],
            "group": row["group"],
            "name": {"en": row["name_en"], "vi": row["name_vi"]},
            "netWorthUsd": row["net_worth"],
            "source": row["source"],
            "asOf": row["as_of"],
            "avatar": {"color": row["color"]},
        }
        if (ASSETS / "images" / "people" / f"{row['id']}.webp").exists():
            entry["image"] = f"images/people/{row['id']}.webp"
        people.append(entry)
    (ASSETS / "people.json").write_text(
        "[\n  " + ",\n  ".join(json.dumps(p, ensure_ascii=False, separators=(",", ":")) for p in people) + "\n]\n",
        encoding="utf-8",
    )
    with_photo = sum(1 for p in people if "image" in p)
    print(f"wrote {len(people)} people ({with_photo} with photos)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
