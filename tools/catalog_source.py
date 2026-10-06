"""Shared loader/validator for tools/catalog-source.tsv (used by build_catalog.py and fetch_images.py)."""
import re
from decimal import Decimal
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "tools" / "catalog-source.tsv"
CATEGORIES = {"food", "shopping", "tech", "transport", "home", "travel", "fun", "sports_music", "mega"}


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
        rows.append({
            "id": item_id, "category": category, "cents": cents, "name_en": name_en, "name_vi": name_vi,
            "icon": icon, "estimate": estimate, "query": query or default_query(name_en),
        })
    return rows, errors


PEOPLE_SOURCE = ROOT / "tools" / "people-source.tsv"
GROUPS = {"billionaire", "celebrity"}


def load_people_rows():
    """Returns (rows, errors). Each row: id, group, name_en, name_vi, net_worth, source, as_of, color, query."""
    rows, seen, errors = [], set(), []
    for number, raw in enumerate(PEOPLE_SOURCE.read_text(encoding="utf-8").splitlines(), start=1):
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split("|")
        if len(parts) != 9:
            errors.append(f"people line {number}: expected 9 fields, got {len(parts)}")
            continue
        pid, group, name_en, name_vi, worth, source, as_of, color, query = [p.strip() for p in parts]
        if not pid.startswith("p_") or pid in seen:
            errors.append(f"people line {number}: bad or duplicate id {pid}")
        seen.add(pid)
        if group not in GROUPS:
            errors.append(f"people line {number}: unknown group {group}")
        if not worth.isdigit() or int(worth) <= 0:
            errors.append(f"people line {number}: bad net worth {worth}")
            continue
        if not re.fullmatch(r"\d{4}-\d{2}", as_of):
            errors.append(f"people line {number}: as_of must be YYYY-MM")
        if not re.fullmatch(r"#[0-9A-Fa-f]{6}", color):
            errors.append(f"people line {number}: bad color {color}")
        rows.append({"id": pid, "group": group, "name_en": name_en, "name_vi": name_vi, "net_worth": int(worth),
                     "source": source, "as_of": as_of, "color": color, "query": query or name_en})
    return rows, errors
