#!/usr/bin/env python3
"""Builds app/src/main/assets/catalog.json from tools/catalog-source.tsv.

Usage (from the repo root):  python tools/build_catalog.py
Fails loudly on duplicate ids, unknown categories, bad prices, or estimate flags outside
sports_music/mega, so the app's data validation test never sees broken content.
"""
import json
import sys
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "tools" / "catalog-source.tsv"
TARGET = ROOT / "app" / "src" / "main" / "assets" / "catalog.json"
CATEGORIES = {"food", "shopping", "tech", "transport", "home", "travel", "fun", "sports_music", "mega"}
ESTIMATE_CATEGORIES = {"sports_music", "mega"}


def main() -> int:
    items, seen, errors = [], set(), []
    for number, raw in enumerate(SOURCE.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split("|")
        if len(parts) not in (6, 7):
            errors.append(f"line {number}: expected 6 or 7 fields, got {len(parts)}")
            continue
        item_id, category, price, name_en, name_vi, icon = parts[:6]
        flag = parts[6] if len(parts) == 7 else ""
        if item_id in seen:
            errors.append(f"line {number}: duplicate id {item_id}")
        seen.add(item_id)
        if category not in CATEGORIES:
            errors.append(f"line {number}: unknown category {category}")
        try:
            cents = int((Decimal(price) * 100).to_integral_value())
        except Exception:
            errors.append(f"line {number}: bad price {price}")
            continue
        if cents <= 0:
            errors.append(f"line {number}: price must be positive")
        estimate = flag == "e"
        if estimate and category not in ESTIMATE_CATEGORIES:
            errors.append(f"line {number}: estimate flag only allowed in {sorted(ESTIMATE_CATEGORIES)}")
        entry = {
            "id": item_id,
            "category": category,
            "priceCents": cents,
            "name": {"en": name_en, "vi": name_vi},
            "icon": icon,
        }
        if estimate:
            entry["estimate"] = True
        items.append(entry)

    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1

    lines = [json.dumps(entry, ensure_ascii=False, separators=(",", ":")) for entry in items]
    TARGET.write_text("[\n  " + ",\n  ".join(lines) + "\n]\n", encoding="utf-8")
    print(f"wrote {len(items)} items to {TARGET.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
