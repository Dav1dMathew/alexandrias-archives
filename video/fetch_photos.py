"""Fetch real landmark photos from Wikimedia Commons for the video.

No API key needed. Records each file's licence and author so attribution
can be published with the video - several Commons images are CC BY-SA,
which makes credit a licence condition, not a nicety.

Run from video/:  python3 fetch_photos.py
"""

import json
import os
import urllib.parse
import urllib.request

API = "https://commons.wikimedia.org/w/api.php"
USER_AGENT = "AlexandriasArchivesVideo/0.1 (educational competition project)"
OUT_DIR = os.path.join(os.path.dirname(__file__), "public", "photos")

# Licences we're willing to use. Anything else is skipped rather than
# risking an unlicensed asset in a published video.
ALLOWED = ("cc0", "cc by", "cc-by", "public domain", "pd-", "attribution")

WANTED = {
    "pisa": "Leaning Tower of Pisa tower",
    "burj": "Burj Khalifa Dubai skyscraper",
    "taj": "Taj Mahal Agra",
    "colosseum": "Colosseum Rome amphitheatre",
    "blueprint": "architectural drawing elevation historic plan",
    "archive": "old library archive books manuscript",
}


def search(query, limit=6):
    params = {
        "action": "query",
        "generator": "search",
        "gsrsearch": query,
        "gsrnamespace": 6,
        "gsrlimit": limit,
        "prop": "imageinfo",
        "iiprop": "url|extmetadata|mime|size",
        "iiurlwidth": 1920,
        "format": "json",
    }
    url = f"{API}?{urllib.parse.urlencode(params)}"
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=25) as response:
        return json.loads(response.read())


def pick(data):
    pages = (data.get("query") or {}).get("pages") or {}
    for page in sorted(pages.values(), key=lambda p: p.get("index", 99)):
        info = (page.get("imageinfo") or [{}])[0]
        meta = info.get("extmetadata") or {}
        mime = info.get("mime", "")
        if not mime.startswith("image/") or "svg" in mime:
            continue

        licence = (meta.get("LicenseShortName", {}).get("value") or "").strip()
        if not any(token in licence.lower() for token in ALLOWED):
            continue

        thumb = info.get("thumburl")
        if not thumb:
            continue

        author = (meta.get("Artist", {}).get("value") or "Unknown").strip()
        # Artist often arrives as HTML - strip tags crudely, it's only
        # ever used as a credit string.
        for tag in ("<", ">"):
            while tag in author:
                start = author.find("<")
                end = author.find(">", start)
                if start == -1 or end == -1:
                    break
                author = author[:start] + author[end + 1:]
        author = " ".join(author.split())[:120]

        return {
            "title": page.get("title", ""),
            "url": thumb,
            "licence": licence,
            "author": author,
            "descriptionurl": info.get("descriptionurl", ""),
        }
    return None


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    credits = {}

    for key, query in WANTED.items():
        try:
            chosen = pick(search(query))
        except Exception as error:  # noqa: BLE001 - report and continue
            print(f"{key}: search failed ({error})")
            continue

        if chosen is None:
            print(f"{key}: no acceptably-licensed result")
            continue

        target = os.path.join(OUT_DIR, f"{key}.jpg")
        request = urllib.request.Request(
            chosen["url"], headers={"User-Agent": USER_AGENT}
        )
        try:
            with urllib.request.urlopen(request, timeout=40) as response:
                payload = response.read()
        except Exception as error:  # noqa: BLE001
            print(f"{key}: download failed ({error})")
            continue

        with open(target, "wb") as handle:
            handle.write(payload)

        credits[key] = chosen
        print(f"{key}: {len(payload)//1024} KB | {chosen['licence']} | {chosen['author']}")

    with open(os.path.join(OUT_DIR, "credits.json"), "w") as handle:
        json.dump(credits, handle, indent=2)
    print(f"\n{len(credits)} photos written to {OUT_DIR}")


if __name__ == "__main__":
    main()
