#!/usr/bin/env python3
"""Downloads openly licensed item photos from Wikimedia Commons.

Usage (repo root):
  python tools/fetch_images.py                 # fetch every item that has no photo yet
  python tools/fetch_images.py --only a,b,c    # (re)fetch just these ids
  python tools/fetch_images.py --refresh       # refetch everything

Only images licensed CC0 / public domain / CC BY / CC BY-SA are accepted (never NC/ND or non-free),
and images flagged as showing identifiable people ("personality" restriction) are rejected.
Each photo is centre-cropped to a square, resized to 360x360 and stored as WebP in
app/src/main/assets/images/. Author, license and page URL go to tools/images-manifest.json, from which
build_catalog.py generates the in-app credits list.

Per-item overrides live in tools/image-overrides.json:
  {"item_id": {"query": "better search text"}}   # search again with other words
  {"item_id": {"file": "File:Exact name.jpg"}}   # force one specific Commons file
  {"item_id": {"skip": true}}                    # keep the emoji, no photo
"""
import argparse
import html
import io
import json
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

from PIL import Image

from catalog_source import ROOT, load_rows

API = "https://commons.wikimedia.org/w/api.php"
UA = "SpendLikeAMillionaire/0.1 (https://github.com/michaeo04/spend-like-a-millionaire; open-source app)"
OUT_DIR = ROOT / "app" / "src" / "main" / "assets" / "images"
MANIFEST = ROOT / "tools" / "images-manifest.json"
OVERRIDES = ROOT / "tools" / "image-overrides.json"
SIZE = 360
SKIP_WORDS = ("logo", "icon", "diagram", "flag of", "coat of arms", "screenshot", "poster", "stamp", "sketch", "drawing")
ALLOWED_LICENSE = re.compile(r"^(CC0|CC BY(-SA)? \d|CC-BY|Public domain|PD)", re.I)
REJECTED_LICENSE = re.compile(r"(NC|ND|non-?free|fair use)", re.I)
META = "LicenseShortName|Artist|Credit|Restrictions|NonFree"
last_call = 0.0


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
    return (info.get("extmetadata", {}).get(key) or {}).get("value", "") or ""


def acceptable(page: dict):
    info = (page.get("imageinfo") or [None])[0]
    if not info or info.get("mime") not in ("image/jpeg", "image/png"):
        return None
    if info.get("width", 0) < 640 or info.get("height", 0) < 400 or not info.get("thumburl"):
        return None
    title = page.get("title", "").lower()
    if any(word in title for word in SKIP_WORDS):
        return None
    license_name = meta_value(info, "LicenseShortName")
    if not ALLOWED_LICENSE.search(license_name) or REJECTED_LICENSE.search(license_name):
        return None
    if meta_value(info, "NonFree").lower() in ("true", "1", "yes"):
        return None
    if "personality" in meta_value(info, "Restrictions").lower():
        return None
    author = re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", meta_value(info, "Artist")))).strip()
    if not author:
        author = re.sub(r"\s+", " ", html.unescape(re.sub(r"<[^>]+>", "", meta_value(info, "Credit")))).strip()
    return {
        "title": page["title"].removeprefix("File:"),
        "page": info.get("descriptionurl", ""),
        "author": author[:120] or "Unknown",
        "license": license_name,
        "thumb": info["thumburl"],
    }


IMAGEINFO = {"prop": "imageinfo", "iiprop": "url|size|mime|extmetadata", "iiurlwidth": 640, "iiextmetadatafilter": META}


def search(query: str):
    data = api({"action": "query", "format": "json", "generator": "search", "gsrnamespace": 6,
                "gsrsearch": f"{query} filetype:bitmap", "gsrlimit": 20, **IMAGEINFO})
    pages = sorted((data.get("query") or {}).get("pages", {}).values(), key=lambda p: p.get("index", 0))
    for page in pages:
        candidate = acceptable(page)
        if candidate:
            return candidate
    return None


def by_title(title: str):
    data = api({"action": "query", "format": "json", "titles": title, **IMAGEINFO})
    for page in (data.get("query") or {}).get("pages", {}).values():
        return acceptable(page)
    return None


def save_square(raw: bytes, path):
    image = Image.open(io.BytesIO(raw)).convert("RGB")
    side = min(image.size)
    left, top = (image.width - side) // 2, (image.height - side) // 2
    image = image.crop((left, top, left + side, top + side)).resize((SIZE, SIZE), Image.LANCZOS)
    image.save(path, "WEBP", quality=72, method=6)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--only", help="comma separated item ids")
    parser.add_argument("--refresh", action="store_true", help="refetch even items that already have a photo")
    args = parser.parse_args()

    rows, errors = load_rows()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8")) if MANIFEST.exists() else {}
    overrides = json.loads(OVERRIDES.read_text(encoding="utf-8-sig")) if OVERRIDES.exists() else {}
    only = set(args.only.split(",")) if args.only else None
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    missing = []
    for row in rows:
        item_id = row["id"]
        if only is not None and item_id not in only:
            continue
        if only is None and not args.refresh and item_id in manifest and (OUT_DIR / f"{item_id}.webp").exists():
            continue
        override = overrides.get(item_id, {})
        if override.get("skip"):
            manifest.pop(item_id, None)
            (OUT_DIR / f"{item_id}.webp").unlink(missing_ok=True)
            print(f"skip   {item_id}")
            continue
        try:
            if "file" in override:
                found = by_title(override["file"])
            else:
                found = search(override.get("query", row["query"]))
                if not found and "query" not in override:
                    found = search(row["name_en"])
            if not found:
                missing.append(item_id)
                print(f"MISSING {item_id}  ({row['query']})")
                continue
            save_square(get(found.pop("thumb")), OUT_DIR / f"{item_id}.webp")
            manifest[item_id] = found
            print(f"ok     {item_id}  <- {found['title']} [{found['license']}]")
        except Exception as e:  # keep going; the item simply stays without a photo
            missing.append(item_id)
            print(f"ERROR  {item_id}: {e}")
        MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=1, sort_keys=True), encoding="utf-8")

    MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=1, sort_keys=True), encoding="utf-8")
    print(f"\n{len(manifest)} photos in manifest; {len(missing)} missing: {', '.join(missing)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
