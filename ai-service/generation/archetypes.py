"""Tier 0 procedural generators.

Each generator turns GenerationParams into a (blocks, width, height, depth)
tuple. Blocks are plain (x, y, z, block_id) tuples so this module has no
dependency on FastAPI/pydantic and can be unit tested in isolation - only
the API layer (main.py) wraps the result into the Blueprint schema.
"""

import math
import re

from .parser import GenerationParams

Block = tuple[int, int, int, str]

SCALE_MULTIPLIERS = {"mini": 0.6, "medium": 1.0, "huge": 1.8}

FLOOR_MATERIAL = "minecraft:stone"
ROOF_MATERIAL = "minecraft:oak_planks"
GLASS_MATERIAL = "minecraft:glass"
COLUMN_MATERIAL = "minecraft:quartz_pillar"


def _scale(base: int, params: GenerationParams, minimum: int = 3) -> int:
    multiplier = SCALE_MULTIPLIERS[params.scale_tier]
    return max(minimum, round(base * multiplier))


def generate_house(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    width = _scale(7, params)
    depth = _scale(7, params)
    wall_height = _scale(3, params, minimum=2)

    blocks: list[Block] = []

    for x in range(width):
        for z in range(depth):
            blocks.append((x, 0, z, FLOOR_MATERIAL))

    for y in range(1, wall_height + 1):
        for x in range(width):
            blocks.append((x, y, 0, params.wall_material))
            blocks.append((x, y, depth - 1, params.wall_material))
        for z in range(1, depth - 1):
            blocks.append((0, y, z, params.wall_material))
            blocks.append((width - 1, y, z, params.wall_material))

    roof_y = wall_height + 1
    for x in range(width):
        for z in range(depth):
            blocks.append((x, roof_y, z, ROOF_MATERIAL))

    door_x = width // 2
    door_top = min(2, wall_height)
    blocks = [
        block for block in blocks
        if not (block[0] == door_x and block[2] == 0 and 1 <= block[1] <= door_top)
    ]

    if wall_height >= 2:
        blocks.append((0, 2, depth // 2, GLASS_MATERIAL))

    return blocks, width, roof_y + 1, depth


def _lean_offset(y: int, params: GenerationParams) -> int:
    # Each layer is a rigid size x size ring, stacked with a growing lateral
    # shift as height increases - the same mechanism that actually produces
    # a leaning tower (uneven-foundation settling shifts each successive
    # floor), not a distortion of the ring's shape.
    if params.lean_angle_degrees <= 0:
        return 0
    return round(y * math.tan(math.radians(params.lean_angle_degrees)))


def _size_at_layer(y: int, top_y: int, base_size: int, params: GenerationParams) -> int:
    # The actual silhouette control: how wide this layer's footprint is,
    # shrinking from base_size toward the tip as height increases. Smooth
    # (taper_ratio only) or stepped (setback_tiers) per params.
    top_size = max(2, round(base_size * (1 - params.taper_ratio)))
    if base_size == top_size or top_y <= 0:
        return base_size

    if params.setback_tiers > 0:
        tier = min(params.setback_tiers, int(y / (top_y + 1) * params.setback_tiers))
        t = tier / params.setback_tiers
    else:
        t = y / top_y

    return max(2, round(base_size - t * (base_size - top_size)))


def _square_outline(cx: float, cz: float, size: int, filled: bool, step: int = 1):
    start_x = round(cx - size / 2)
    start_z = round(cz - size / 2)
    end_x, end_z = start_x + size - 1, start_z + size - 1
    if filled:
        for x in range(start_x, end_x + 1):
            for z in range(start_z, end_z + 1):
                yield x, z
        return
    for x in range(start_x, end_x + 1, step):
        yield x, start_z
        yield x, end_z
    for z in range(start_z + step, end_z, step):
        yield start_x, z
        yield end_x, z


def _circular_outline(cx: float, cz: float, size: int, filled: bool):
    radius = size / 2
    span = math.ceil(radius) + 1
    start_x, start_z = round(cx - span), round(cz - span)
    for x in range(start_x, start_x + 2 * span + 1):
        for z in range(start_z, start_z + 2 * span + 1):
            distance = math.dist((x, z), (cx, cz))
            if filled and distance <= radius:
                yield x, z
            elif not filled and abs(distance - radius) < 0.8:
                yield x, z


def _y_shape_outline(cx: float, cz: float, size: int, filled: bool):
    # Three rectangular wings radiating from a central core 120 degrees
    # apart - the Burj Khalifa's actual defining feature (a tri-lobed
    # floor plan), approximated on a voxel grid. Always filled (the
    # "wings" are solid slabs, not a hollow ring) since a hollow tri-lobed
    # outline this thin reads as noise rather than a shape.
    arm_length = size / 2
    arm_half_width = max(1, size // 6)
    core_radius = max(1, size // 4)

    points: set[tuple[int, int]] = set()
    for x, z in _circular_outline(cx, cz, core_radius * 2, filled=True):
        points.add((x, z))

    for i in range(3):
        angle = math.radians(90 + i * 120)
        dx, dz = math.cos(angle), math.sin(angle)
        perp_dx, perp_dz = -dz, dx
        for dist in range(0, math.ceil(arm_length) + 1):
            for w in range(-arm_half_width, arm_half_width + 1):
                x = round(cx + dist * dx + w * perp_dx)
                z = round(cz + dist * dz + w * perp_dz)
                points.add((x, z))

    yield from points


def _normalize_xz_bounds(blocks: list[Block]) -> tuple[list[Block], int, int]:
    # _circular_outline's loose ring-matching tolerance can produce points
    # slightly outside the caller's assumed size/diameter (it searches a
    # window padded by 1 to catch near-miss ring hits) - so anything using
    # it can't just trust a formula-derived bounding box. Recomputes width/
    # depth from the actual block positions and shifts to start at (0, *, 0),
    # which BlueprintValidator requires.
    xs = [b[0] for b in blocks]
    zs = [b[2] for b in blocks]
    min_x, min_z = min(xs), min(zs)
    normalized = [(x - min_x, y, z - min_z, m) for x, y, z, m in blocks]
    width = max(xs) - min_x + 1
    depth = max(zs) - min_z + 1
    return normalized, width, depth


def _layer_outline(cross_section: str, cx: float, cz: float, size: int, filled: bool):
    if cross_section == "circular":
        yield from _circular_outline(cx, cz, size, filled)
    elif cross_section == "y_shape":
        yield from _y_shape_outline(cx, cz, size, filled)
    else:
        yield from _square_outline(cx, cz, size, filled)


def generate_tower(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    base_size = _scale(5, params, minimum=4)
    height = _scale(15, params, minimum=6)
    merlon_y = height + 1
    lean_x = params.lean_direction == "x"
    cross_section = params.cross_section

    # Fixed center so tapering shrinks inward symmetrically instead of
    # anchoring to a corner; lean is layered on top as a per-layer shift
    # of that same center point.
    center = (base_size - 1) / 2

    blocks: list[Block] = []

    for x, z in _layer_outline(cross_section, center, center, base_size, filled=True):
        blocks.append((x, 0, z, FLOOR_MATERIAL))

    for y in range(1, height + 1):
        offset = _lean_offset(y, params)
        cx = center + offset if lean_x else center
        cz = center + offset if not lean_x else center
        size = _size_at_layer(y, height, base_size, params)
        for x, z in _layer_outline(cross_section, cx, cz, size, filled=False):
            blocks.append((x, y, z, params.wall_material))

    # Merlons (square towers only - a tri-lobed/circular tip doesn't read
    # as battlements): every other position, top ring only.
    if cross_section == "square":
        offset = _lean_offset(merlon_y, params)
        cx = center + offset if lean_x else center
        cz = center + offset if not lean_x else center
        top_size = _size_at_layer(merlon_y, height, base_size, params)
        for x, z in _square_outline(cx, cz, top_size, filled=False, step=2):
            blocks.append((x, merlon_y, z, params.wall_material))

    blocks, width, depth = _normalize_xz_bounds(blocks)
    return blocks, width, merlon_y + 1, depth


def generate_temple(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    radius = _scale(8, params, minimum=4)
    column_height = _scale(6, params, minimum=3)
    num_columns = 12

    blocks: list[Block] = []
    center = radius
    diameter = radius * 2 + 1

    for x in range(diameter):
        for z in range(diameter):
            if math.dist((x, z), (center, center)) <= radius:
                blocks.append((x, 0, z, FLOOR_MATERIAL))

    column_radius = max(1, radius - 1)
    seen_columns: set[tuple[int, int]] = set()
    for i in range(num_columns):
        angle = 2 * math.pi * i / num_columns
        cx = center + round(column_radius * math.cos(angle))
        cz = center + round(column_radius * math.sin(angle))
        if (cx, cz) in seen_columns:
            continue
        seen_columns.add((cx, cz))
        for y in range(1, column_height + 1):
            blocks.append((cx, y, cz, COLUMN_MATERIAL))

    return blocks, diameter, column_height + 1, diameter


HUB_MATERIAL = "minecraft:iron_block"
SPOKE_MATERIAL = "minecraft:iron_bars"


def generate_wheel(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    # A Ferris wheel isn't a resized box - it's a ring standing in a
    # vertical plane (x/y), with some thickness front-to-back (z). This
    # is the shape none of house/tower/temple can produce at all, no
    # matter how they're parameterized.
    outer_radius = _scale(10, params, minimum=6)
    ring_width = max(2, outer_radius // 6)
    inner_radius = outer_radius - ring_width
    thickness = max(2, outer_radius // 5)
    hub_radius = max(1, outer_radius // 8)
    num_spokes = 16

    diameter = outer_radius * 2 + 1
    center = outer_radius

    blocks: list[Block] = []

    for z in range(thickness):
        for x in range(diameter):
            for y in range(diameter):
                distance = math.dist((x, y), (center, center))
                if inner_radius <= distance <= outer_radius:
                    blocks.append((x, y, z, params.wall_material))
                elif distance <= hub_radius:
                    blocks.append((x, y, z, HUB_MATERIAL))

        for i in range(num_spokes):
            angle = 2 * math.pi * i / num_spokes
            dx, dy = math.cos(angle), math.sin(angle)
            for dist in range(hub_radius, inner_radius + 1):
                x = round(center + dist * dx)
                y = round(center + dist * dy)
                blocks.append((x, y, z, SPOKE_MATERIAL))

    return blocks, diameter, diameter, thickness


def generate_dome(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    # A rounded dome, not a tapering cone - the radius follows a
    # hemisphere profile (r = R*sqrt(1 - t^2)) rather than shrinking
    # linearly, which is what actually reads as "dome" instead of
    # "pointy roof." Sits on a short cylindrical drum, the way most real
    # domed buildings do (Taj Mahal, mosques, capitol buildings).
    base_radius = _scale(9, params, minimum=5)
    drum_height = max(2, base_radius // 3)
    dome_height = base_radius
    diameter = base_radius * 2 + 1
    center = base_radius

    blocks: list[Block] = []

    for x, z in _circular_outline(center, center, diameter, filled=True):
        blocks.append((x, 0, z, FLOOR_MATERIAL))

    for y in range(1, drum_height + 1):
        for x, z in _circular_outline(center, center, diameter, filled=False):
            blocks.append((x, y, z, params.wall_material))

    for dy in range(0, dome_height + 1):
        y = drum_height + dy
        t = dy / dome_height
        radius_here = base_radius * math.sqrt(max(0.0, 1 - t * t))
        if radius_here < 0.5:
            blocks.append((center, y, center, params.wall_material))
            continue
        for x, z in _circular_outline(center, center, round(radius_here * 2), filled=False):
            blocks.append((x, y, z, params.wall_material))

    blocks, width, depth = _normalize_xz_bounds(blocks)
    return blocks, width, drum_height + dome_height + 1, depth


GENERATORS = {
    "house": generate_house,
    "tower": generate_tower,
    "temple": generate_temple,
    "wheel": generate_wheel,
    "dome": generate_dome,
}


def _add_accessories(
    blocks: list[Block], params: GenerationParams
) -> list[Block]:
    # Freestanding small towers around the main structure's perimeter -
    # e.g. the Taj Mahal's 4 corner minarets, distinct volumes standing
    # apart from the dome, not part of its shape.
    if params.accessory_count <= 0:
        return blocks

    main_xs = [b[0] for b in blocks]
    main_zs = [b[2] for b in blocks]
    center_x = (min(main_xs) + max(main_xs)) / 2
    center_z = (min(main_zs) + max(main_zs)) / 2
    main_span = max(max(main_xs) - min(main_xs), max(main_zs) - min(main_zs))

    accessory_params = GenerationParams(
        archetype="tower",
        scale_tier="mini",
        wall_material=params.wall_material,
        prompt=params.prompt,
        cross_section="circular",
    )
    accessory_blocks, aw, _, ad = generate_tower(accessory_params)
    accessory_span = max(aw, ad)

    placement_radius = main_span / 2 + accessory_span / 2 + 2

    combined = list(blocks)
    for i in range(params.accessory_count):
        angle = 2 * math.pi * i / params.accessory_count
        offset_x = round(center_x + placement_radius * math.cos(angle) - aw / 2)
        offset_z = round(center_z + placement_radius * math.sin(angle) - ad / 2)
        for x, y, z, material in accessory_blocks:
            combined.append((x + offset_x, y, z + offset_z, material))

    return combined


def _slugify(prompt: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "_", prompt.lower()).strip("_")
    return slug[:40] or "generated_structure"


def _dedupe(blocks: list[Block]) -> list[Block]:
    # Keeps the last block written at each position, so intentional
    # overrides (doors, windows) win over the wall/floor fill they sit on.
    by_position: dict[tuple[int, int, int], str] = {}
    for x, y, z, block_id in blocks:
        by_position[(x, y, z)] = block_id
    return [(x, y, z, block_id) for (x, y, z), block_id in by_position.items()]


def generate_blueprint(params: GenerationParams) -> dict:
    generator = GENERATORS[params.archetype]
    blocks, width, height, depth = generator(params)

    if params.accessory_count > 0 and params.archetype != "tower":
        blocks = _add_accessories(blocks, params)
        # Accessories can extend past the main structure's original
        # bounding box in every direction - recompute it from the
        # combined set and re-shift to start at (0, *, 0), same
        # requirement as every individual generator already satisfies.
        xs = [b[0] for b in blocks]
        zs = [b[2] for b in blocks]
        min_x, min_z = min(xs), min(zs)
        blocks = [(x - min_x, y, z - min_z, m) for x, y, z, m in blocks]
        width = max(xs) - min_x + 1
        depth = max(zs) - min_z + 1
        height = max(height, max(b[1] for b in blocks) + 1)

    blocks = _dedupe(blocks)

    return {
        "id": _slugify(params.prompt),
        "name": params.prompt.strip() or f"Generated {params.archetype}",
        "width": width,
        "height": height,
        "depth": depth,
        "blocks": [
            {"x": x, "y": y, "z": z, "blockId": block_id}
            for x, y, z, block_id in blocks
        ],
        "reasoning": params.reasoning,
        "attributes": params.attributes,
    }
