"""Turns a real OSM building footprint into Minecraft blocks.

Unlike the archetype generators (archetypes.py), this doesn't pick a
shape - it traces the building's actual real-world outline, so the
result's silhouette is genuinely that building's, not a guess.
"""

import math

Block = tuple[int, int, int, str]

FLOOR_MATERIAL = "minecraft:stone"
ROOF_MATERIAL = "minecraft:stone_bricks"

MAX_DIMENSION_BLOCKS = 60  # stays under BlueprintValidator's 64 with margin
MAX_HEIGHT_BLOCKS = 60
MIN_HEIGHT_BLOCKS = 3


def _project_to_meters(points_latlon: list[tuple[float, float]]) -> list[tuple[float, float]]:
    # Small-area flat-earth approximation - accurate enough at
    # building/city-block scale, meaningfully wrong over many kilometers.
    lat0 = sum(lat for lat, _ in points_latlon) / len(points_latlon)
    lon0 = sum(lon for _, lon in points_latlon) / len(points_latlon)
    meters_per_lat_degree = 110_540.0
    meters_per_lon_degree = 111_320.0 * math.cos(math.radians(lat0))

    return [
        ((lon - lon0) * meters_per_lon_degree, (lat - lat0) * meters_per_lat_degree)
        for lat, lon in points_latlon
    ]


def _point_in_polygon(x: float, z: float, polygon: list[tuple[float, float]]) -> bool:
    # Standard ray-casting test.
    inside = False
    n = len(polygon)
    for i in range(n):
        x1, z1 = polygon[i]
        x2, z2 = polygon[(i + 1) % n]
        if (z1 > z) != (z2 > z):
            x_intersect = x1 + (z - z1) * (x2 - x1) / (z2 - z1)
            if x < x_intersect:
                inside = not inside
    return inside


def _bresenham(x0: int, z0: int, x1: int, z1: int) -> list[tuple[int, int]]:
    points = []
    dx, dz = abs(x1 - x0), -abs(z1 - z0)
    sx = 1 if x0 < x1 else -1
    sz = 1 if z0 < z1 else -1
    err = dx + dz
    x, z = x0, z0
    while True:
        points.append((x, z))
        if x == x1 and z == z1:
            break
        e2 = 2 * err
        if e2 >= dz:
            err += dz
            x += sx
        if e2 <= dx:
            err += dx
            z += sz
    return points


def build_from_footprint(
    points_latlon: list[tuple[float, float]],
    height_meters: float,
    wall_material: str,
) -> tuple[list[Block], int, int, int]:
    projected = _project_to_meters(points_latlon)

    xs = [x for x, _ in projected]
    zs = [z for _, z in projected]
    span_x, span_z = max(xs) - min(xs), max(zs) - min(zs)
    max_span = max(span_x, span_z, 1.0)

    # One shared scale for footprint AND height, so a genuinely tall,
    # slender building (e.g. ~150m wide, ~500m tall) shrinks proportionally
    # instead of each dimension being capped independently - which would
    # otherwise crush a skyscraper's real aspect ratio into a squat cube
    # (footprint maxed at 60 blocks wide, height separately maxed at 60
    # blocks tall, when its true proportions are nowhere near 1:1).
    scale = min(
        1.0,
        MAX_DIMENSION_BLOCKS / max_span,
        MAX_HEIGHT_BLOCKS / max(height_meters, 1.0),
    )

    min_x, min_z = min(xs), min(zs)
    grid_polygon = [
        (round((x - min_x) * scale), round((z - min_z) * scale))
        for x, z in projected
    ]
    # Dedupe consecutive identical points introduced by rounding at low scale.
    deduped = [grid_polygon[0]]
    for point in grid_polygon[1:]:
        if point != deduped[-1]:
            deduped.append(point)
    if len(deduped) > 1 and deduped[0] == deduped[-1]:
        deduped.pop()
    grid_polygon = deduped

    if len(grid_polygon) < 3:
        raise ValueError("footprint collapsed to fewer than 3 points after scaling")

    height_blocks = max(
        MIN_HEIGHT_BLOCKS, min(MAX_HEIGHT_BLOCKS, round(height_meters * scale))
    )

    poly_xs = [p[0] for p in grid_polygon]
    poly_zs = [p[1] for p in grid_polygon]
    width = max(poly_xs) - min(poly_xs) + 1
    depth = max(poly_zs) - min(poly_zs) + 1

    blocks: list[Block] = []

    # Floor: fill every point inside the polygon.
    for x in range(min(poly_xs), max(poly_xs) + 1):
        for z in range(min(poly_zs), max(poly_zs) + 1):
            if _point_in_polygon(x + 0.5, z + 0.5, grid_polygon):
                blocks.append((x, 0, z, FLOOR_MATERIAL))

    # Walls: trace the actual polygon edges (not a bounding shape) at
    # every height level.
    wall_xz: set[tuple[int, int]] = set()
    for i in range(len(grid_polygon)):
        x0, z0 = grid_polygon[i]
        x1, z1 = grid_polygon[(i + 1) % len(grid_polygon)]
        wall_xz.update(_bresenham(x0, z0, x1, z1))

    for y in range(1, height_blocks + 1):
        for x, z in wall_xz:
            blocks.append((x, y, z, wall_material))

    # Roof cap, same footprint as the floor.
    for x in range(min(poly_xs), max(poly_xs) + 1):
        for z in range(min(poly_zs), max(poly_zs) + 1):
            if _point_in_polygon(x + 0.5, z + 0.5, grid_polygon):
                blocks.append((x, height_blocks + 1, z, ROOF_MATERIAL))

    return blocks, width, height_blocks + 2, depth
