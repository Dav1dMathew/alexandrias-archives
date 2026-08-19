"""Free real reference photo lookup via Wikipedia.

No API key, no billing - Wikipedia's own MediaWiki API returns each
notable page's main image ("pageimages"), which for any landmark is
almost always the definitive photo of the thing. Feeds mosaic_builder.py.
"""

import json
import urllib.parse
import urllib.request
import urllib.error

WIKIPEDIA_API_URL = "https://en.wikipedia.org/w/api.php"
USER_AGENT = "AlexandriasArchives/0.1 (+https://github.com/Dav1dMathew/alexandrias-archives)"
REQUEST_TIMEOUT_SECONDS = 15
THUMBNAIL_SIZE = 400  # px, downscaled further to block resolution later


def fetch_reference_image_bytes(prompt: str) -> bytes | None:
    """Returns raw image bytes for the prompt's best-matching Wikipedia
    page's main photo, or None if there's no match, no image, or either
    request fails/times out. Never raises."""
    thumbnail_url = _find_thumbnail_url(prompt)
    if thumbnail_url is None:
        return None
    return _download(thumbnail_url)


def _find_thumbnail_url(prompt: str) -> str | None:
    query = urllib.parse.urlencode({
        "action": "query",
        "generator": "search",
        "gsrsearch": prompt,
        "gsrlimit": 1,
        "prop": "pageimages",
        "pithumbsize": THUMBNAIL_SIZE,
        "format": "json",
    })
    url = f"{WIKIPEDIA_API_URL}?{query}"

    try:
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
            body = json.loads(response.read())
    except (urllib.error.URLError, TimeoutError, OSError, json.JSONDecodeError):
        return None

    try:
        pages = body["query"]["pages"]
        page = next(iter(pages.values()))
        return page["thumbnail"]["source"]
    except (KeyError, StopIteration, TypeError):
        return None


def _download(url: str) -> bytes | None:
    try:
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
            return response.read()
    except (urllib.error.URLError, TimeoutError, OSError):
        return None
