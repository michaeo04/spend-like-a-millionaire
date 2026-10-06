"""Shared loader/validator for tools/catalog-source.tsv (used by build_catalog.py and fetch_images.py)."""
import re
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "tools" / "catalog-source.tsv"
CATEGORIES = {"food", "shopping", "tech", "transport", "home", "travel", "fun", "sports_music", "mega"}
ESTIMATE_CATEGORIES = {"sports_music", "mega"}


def default_query(name_en: str) -> str:
    """Item name without parenthesised notes, e.g. 'Kite' or 'Bulthaup designer kitchen'."""
    return re.sub(r"\s*\([^)]*\)", "", name_en).strip()


def load_rows():
    """Returns (rows, errors). Each row: id, category, cents, name_en, name_vi, icon, estimate, query."""
    rows, seen, errors = [], set(), []
    for number, raw in enumerate(SOURCE.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split("|")
        if not 6 <= len(parts) <= 8:
            errors.append(f"line {number}: expected 6-8 fields, got {len(parts)}")
            continue
        item_id, category, price, name_en, name_vi, icon = parts[:6]
        flag = parts[6] if len(parts) > 6 else ""
        query = parts[7].strip() if len(parts) > 7 else ""
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
        rows.append({
            "id": item_id, "category": category, "cents": cents, "name_en": name_en, "name_vi": name_vi,
            "icon": icon, "estimate": estimate, "query": query or default_query(name_en),
        })
    return rows, errors
