"""Free, real building geometry via OpenStreetMap.

No API key, no billing - Nominatim (geocoding) and Overpass (feature
data) are both free public OSM services. For a named real building this
gives an actual traced footprint polygon and often a real height/level
count, instead of guessing which of 3 templates is closest.

Both services are rate-limited public infrastructure - this module is
meant to be called once per /generate request, not looped or batched.
"""

import json
import math
import re
import urllib.parse
import urllib.request
import urllib.error

NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"
OVERPASS_URL = "https://overpass-api.de/api/interpreter"
USER_AGENT = "AlexandriasArchives/0.1 (hackathon project; contact: n/a)"
REQUEST_TIMEOUT_SECONDS = 15
METERS_PER_LEVEL = 3.0
DEFAULT_HEIGHT_METERS = 10.0


def lookup_real_building(prompt: str) -> dict | None:
    """Returns {"name", "points_latlon", "height_meters"} for a real,
    named building found on OpenStreetMap, or None if nothing usable was
    found (no match, no candidate is actually tagged as a building, or
    either service is unreachable/times out). Never raises.

    Prompts are often more than just a place name ("the burj khalifa at
    sunset, how it looked being built") - Nominatim's free-text search
    can rank a wrong nearby feature (a park, plaza, road) above the real
    building when there's extra descriptive text diluting the query, and
    that wrong feature is still a valid "way", so a naive first-match
    would confidently extrude a park into a giant flat slab. To guard
    against that, every candidate is verified via Overpass to actually
    carry a building=* tag before being accepted; candidates without one
    are skipped in favor of the next.
    """
    for query in _query_variants(prompt):
        for candidate in _geocode(query):
            footprint = _fetch_footprint(candidate["osm_type"], candidate["osm_id"])
            if footprint is None or len(footprint["points_latlon"]) < 3:
                continue
            if not footprint["is_building"]:
                continue
            return {
                "name": candidate.get("display_name") or prompt,
                "points_latlon": footprint["points_latlon"],
                "height_meters": footprint["height_meters"],
            }

    return None


def _query_variants(prompt: str) -> list[str]:
    # Nominatim's free-text search is surprisingly fragile to phrasing -
    # a leading "the" was observed to shift "the burj khalifa" onto
    # entirely wrong nearby results (a mall, a residential district)
    # where plain "burj khalifa" correctly matched the tower. Real player
    # prompts are also often more than a bare place name ("...at sunset,
    # how it looked being built"), which dilutes the match further. Try
    # progressively simpler variants rather than betting everything on
    # the raw prompt as typed.
    variants = [prompt]

    no_article = re.sub(r"^\s*(the|a|an)\s+", "", prompt, flags=re.IGNORECASE)
    if no_article != prompt:
        variants.append(no_article)

    # Descriptive/creative suffixes tend to start at a comma or one of
    # these connector words - keep only what's before that.
    before_suffix = re.split(
        r",|\bon how\b|\bwhen it\b|\bwith a\b|\bat sunset\b|\bas it\b",
        no_article, maxsplit=1, flags=re.IGNORECASE,
    )[0].strip()
    if before_suffix and before_suffix not in variants:
        variants.append(before_suffix)

    return variants


def _geocode(prompt: str) -> list[dict]:
    query = urllib.parse.urlencode({"q": prompt, "format": "json", "limit": 5})
    url = f"{NOMINATIM_URL}?{query}"

    try:
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
            results = json.loads(response.read())
    except (urllib.error.URLError, TimeoutError, OSError, json.JSONDecodeError):
        return []

    if not isinstance(results, list):
        return []

    # Only "way"/"relation" results carry footprint geometry - a "node"
    # result is just a labeled point (e.g. a bus stop with the same name)
    # with nothing to extrude. Keep all such candidates, in Nominatim's
    # own relevance order - lookup_real_building() tries each in turn
    # until one is verified as an actual building.
    candidates = []
    for result in results:
        if result.get("osm_type") in ("way", "relation"):
            try:
                osm_id = int(result["osm_id"])
            except (KeyError, TypeError, ValueError):
                continue
            candidates.append({
                "osm_type": result["osm_type"],
                "osm_id": osm_id,
                "display_name": result.get("name") or result.get("display_name"),
            })

    return candidates


def _fetch_footprint(osm_type: str, osm_id: int) -> dict | None:
    query = f"[out:json][timeout:{REQUEST_TIMEOUT_SECONDS - 2}];{osm_type}({osm_id});out geom tags;"
    data = urllib.parse.urlencode({"data": query}).encode("utf-8")

    try:
        request = urllib.request.Request(
            OVERPASS_URL, data=data, headers={"User-Agent": USER_AGENT}
        )
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
            body = json.loads(response.read())
    except (urllib.error.URLError, TimeoutError, OSError, json.JSONDecodeError):
        return None

    elements = body.get("elements")
    if not isinstance(elements, list) or not elements:
        return None

    element = elements[0]
    geometry = element.get("geometry")
    if not isinstance(geometry, list) or len(geometry) < 3:
        # Relations (multipolygons) don't carry a flat "geometry" list the
        # same way ways do - not handled yet, fall back to the archetype
        # system rather than build something wrong.
        return None

    points_latlon = [
        (point["lat"], point["lon"]) for point in geometry
        if isinstance(point, dict) and "lat" in point and "lon" in point
    ]
    if len(points_latlon) < 3:
        return None

    tags = element.get("tags") or {}
    height_meters = _estimate_height(tags)
    # The authoritative check, not Nominatim's search-result "class"
    # (which for e.g. Burj Al Arab is "tourism"/"hotel", not "building" -
    # Nominatim's class describes what kind of place it is, not whether
    # the underlying OSM way is a mapped structure). A park, plaza, or
    # road can still be a "way" but will not have this tag.
    is_building = bool(tags.get("building"))

    return {
        "points_latlon": points_latlon,
        "height_meters": height_meters,
        "is_building": is_building,
    }


def _estimate_height(tags: dict) -> float:
    raw_height = tags.get("height")
    if raw_height:
        try:
            return float("".join(c for c in str(raw_height) if c.isdigit() or c == "."))
        except ValueError:
            pass

    raw_levels = tags.get("building:levels")
    if raw_levels:
        try:
            return float(raw_levels) * METERS_PER_LEVEL
        except ValueError:
            pass

    return DEFAULT_HEIGHT_METERS
