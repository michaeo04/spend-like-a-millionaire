#!/usr/bin/env python3
"""Downloads openly licensed photos (items and people) from Wikimedia Commons.

Usage (repo root):
  python tools/fetch_images.py                 # fetch everything that has no photo yet
  python tools/fetch_images.py --only a,b,c    # (re)fetch just these ids (item ids or p_... person ids)
  python tools/fetch_images.py --refresh       # refetch everything

Only images licensed CC0 / public domain / CC BY / CC BY-SA are accepted (never NC/ND or non-free).
Items: images flagged as showing identifiable people ("personality" restriction) are rejected.
People: portraits are expected to show a person, so that flag is accepted but recorded in the manifest
("personality": true); the file title must contain the person's name and group shots are skipped.
Each photo is cropped to a square (people: biased towards the top so faces stay in frame), resized to
360x360 and stored as WebP in app/src/main/assets/images/ (people in images/people/). Author, license and
page URL go to tools/images-manifest.json, from which build_catalog.py generates the in-app credits list.

Per-item overrides live in tools/image-overrides.json:
  {"id": {"query": "better search text"}}   # search again with other words
  {"id": {"file": "File:Exact name.jpg"}}    # force one specific Commons file
  {"id": {"skip": true}}                     # keep the emoji / initial, no photo
"""
import argparse
import html
import io
import json
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request

from PIL import Image

from catalog_source import ROOT, load_people_rows, load_rows

API = "https://commons.wikimedia.org/w/api.php"
UA = "SpendLikeAMillionaire/0.1 (https://github.com/michaeo04/spend-like-a-millionaire; open-source app)"
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "images"
PEOPLE_DIR = OUT_DIR / "people"
MANIFEST = ROOT / "tools" / "images-manifest.json"
OVERRIDES = ROOT / "tools" / "image-overrides.json"
SIZE = 360
SKIP_WORDS = ("logo", "icon", "diagram", "flag of", "coat of arms", "screenshot", "poster", "stamp", "sketch", "drawing")
GROUP_WORDS = (" and ", " with ", " meets ", " & ", " vs ", " family", " wax", "statue", "caricature", "cartoon")
ALLOWED_LICENSE = re.compile(r"^(CC0|CC BY(-SA)? \d|CC-BY|Public domain|PD)", re.I)
REJECTED_LICENSE = re.compile(r"(NC|ND|non-?free|fair use)", re.I)
META = "LicenseShortName|Artist|Credit|Restrictions|NonFree"
last_call = 0.0


def fold(text: str) -> str:
    """Lowercase and strip accents/punctuation so 'Vượng' matches 'Vuong'."""
    stripped = "".join(c for c in unicodedata.normalize("NFD", text.lower().replace("đ", "d")) if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9 ]+", " ", stripped)


def get(url: str) -> bytes:
    global last_call
    for attempt in range(5):
        wait = 0.6 - (time.time() - last_call)
        if wait > 0:
            time.sleep(wait)
        last_call = time.time()
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": UA}), timeout=40) as r:
                return r.read()
        except urllib.error.HTTPError as e:
            if e.code in (429, 503):
                time.sleep(5 * (attempt + 1))
                continue
            raise
        except (urllib.error.URLError, TimeoutError):
            time.sleep(3 * (attempt + 1))
    raise RuntimeError(f"giving up on {url}")


def api(params: dict) -> dict:
    return json.loads(get(API + "?" + urllib.parse.urlencode(params)))


def meta_value(info: dict, key: str) -> str:
    return (info.get("extmetadata", {}).get(key) or {}).get(key if False else "value", "") or ""


def acceptable(page: dict, person_token=None):
    info = (page.get("imageinfo") or [None])[0]
    if not info or info.get("mime") not in ("image/jpeg", "image/png"):
        return None
    width, height = info.get("width", 0), info.get("height", 0)
    min_width = 400 if person_token is not None else 640  # portraits are shown at 360 px
    if width < min_width or height < 400 or not info.get("thumburl"):
        return None
    title = page.get("title", "")
    lowered = title.lower()
    if any(word in lowered for word in SKIP_WORDS):
        return None
    license_name = meta_value(info, "LicenseShortName")
    if not ALLOWED_LICENSE.search(license_name) or REJECTED_LICENSE.search(license_name):
        return None
    if meta_value(info, "NonFree").lower() in ("true", "1", "yes"):
        return None
    personality = "personality" in meta_value(info, "Restrictions").lower()
    if person_token is not None:
        if person_token not in fold(title):
            return None
        if any(word in f" {lowered} " for word in GROUP_WORDS):
            return None
        if not 0.6 <= width / height <= 1.45:
            return None
    elif personality:
        return None
    author = re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", meta_value(info, "Artist")))).strip()
    if not author:
        author = re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", meta_value(info, "Credit")))).strip()
    result = {
        "title": title.removeprefix("File:"),
        "page": info.get("descriptionurl", ""),
        "author": author[:120] or "Unknown",
        "license": license_name,
        "thumb": info["thumburl"],
    }
    if person_token is not None and personality:
        result["personality"] = True
    return result


IMAGEINFO = {"prop": "imageinfo", "iiprop": "url|size|mime|extmetadata", "iiurlwidth": 640, "iiextmetadatafilter": META}


def search(query: str, person_token=None):
    data = api({"action": "query", "format": "json", "generator": "search", "gsrnamespace": 6,
                "gsrsearch": f"{query} filetype:bitmap", "gsrlimit": 30, **IMAGEINFO})
    pages = sorted((data.get("query") or {}).get("pages", {}).values(), key=lambda p: p.get("index", 0))
    candidates = [c for c in (acceptable(page, person_token) for page in pages) if c]
    if person_token is not None:  # prefer images without a personality-rights note
        candidates.sort(key=lambda c: bool(c.get("personality")))
    return candidates[0] if candidates else None


def by_title(title: str, person_token=None):
    data = api({"action": "query", "format": "json", "titles": title, **IMAGEINFO})
    for page in (data.get("query") or {}).get("pages", {}).values():
        return acceptable(page, person_token)
    return None


def save_square(raw: bytes, path, top_bias: float = 0.5):
    image = Image.open(io.BytesIO(raw)).convert("RGB")
    side = min(image.size)
    left = (image.width - side) // 2
    top = int((image.height - side) * top_bias)
    image = image.crop((left, top, left + side, top + side)).resize((SIZE, SIZE), Image.LANCZOS)
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, "WEBP", quality=72, method=6)


def targets():
    """Items and people as uniform dicts: id, query, name, token (people only), path, top_bias."""
    rows, errors = load_rows()
    people, people_errors = load_people_rows()
    if errors or people_errors:
        print("\n".join(errors + people_errors), file=sys.stderr)
        raise SystemExit(1)
    out = [{"id": r["id"], "query": r["query"], "name": r["name_en"], "token": None,
            "path": OUT_DIR / f"{r['id']}.webp", "top_bias": 0.5} for r in rows]
    for p in people:
        last = fold(p["query"]).split()[-1]
        out.append({"id": p["id"], "query": p["query"], "name": p["name_en"], "token": last,
                    "path": PEOPLE_DIR / f"{p['id']}.webp", "top_bias": 0.15})
    return out


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--only", help="comma separated ids")
    parser.add_argument("--refresh", action="store_true", help="refetch even targets that already have a photo")
    args = parser.parse_args()

    manifest = json.loads(MANIFEST.read_text(encoding="utf-8-sig")) if MANIFEST.exists() else {}
    overrides = json.loads(OVERRIDES.read_text(encoding="utf-8-sig")) if OVERRIDES.exists() else {}
    only = set(args.only.split(",")) if args.only else None

    missing = []
    for target in targets():
        item_id, path = target["id"], target["path"]
        if only is not None and item_id not in only:
            continue
        if only is None and not args.refresh and item_id in manifest and path.exists():
            continue
        override = overrides.get(item_id, {})
        if override.get("skip"):
            manifest.pop(item_id, None)
            path.unlink(missing_ok=True)
            print(f"skip   {item_id}")
            continue
        try:
            if "file" in override:
                found = by_title(override["file"], target["token"])
            else:
                found = search(override.get("query", target["query"]), target["token"])
                if not found and "query" not in override and target["token"] is None:
                    found = search(target["name"])
            if not found:
                missing.append(item_id)
                print(f"MISSING {item_id}  ({target['query']})")
                continue
            save_square(get(found.pop("thumb")), path, target["top_bias"])
            manifest[item_id] = found
            flag = " [personality-rights note]" if found.get("personality") else ""
            print(f"ok     {item_id}  <- {found['title']} [{found['license']}]{flag}")
        except Exception as e:  # keep going; the target simply stays without a photo
            missing.append(item_id)
            print(f"ERROR  {item_id}: {e}")
        MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=1, sort_keys=True), encoding="utf-8")

    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=1, sort_keys=True), encoding="utf-8")
    print(f"\n{len(manifest)} photos in manifest; {len(missing)} missing: {', '.join(missing)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
