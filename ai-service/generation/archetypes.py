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


def _normalize_bounds(blocks: list[Block]) -> tuple[list[Block], int, int, int]:
    # Full 3-axis version of _normalize_xz_bounds - needed by generators
    # (aircraft) where height isn't a simple "layer index" the caller
    # already controls, so y can go negative too (an engine pod hanging
    # below the wing's y-center, for instance).
    xs = [b[0] for b in blocks]
    ys = [b[1] for b in blocks]
    zs = [b[2] for b in blocks]
    min_x, min_y, min_z = min(xs), min(ys), min(zs)
    normalized = [(x - min_x, y - min_y, z - min_z, m) for x, y, z, m in blocks]
    width = max(xs) - min_x + 1
    height = max(ys) - min_y + 1
    depth = max(zs) - min_z + 1
    return normalized, width, height, depth


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

    # Spire. A strongly tapering tower (the Burj Khalifa, an obelisk, a
    # cathedral spire) is defined by rising to a point - without this the
    # profile just stops on a flat cap partway up, which reads as an
    # unfinished building rather than a finished one.
    if params.taper_ratio >= 0.35:
        spire_height = max(2, round(height * 0.18))
        offset = _lean_offset(height, params)
        tip_x = round(center + offset) if lean_x else round(center)
        tip_z = round(center + offset) if not lean_x else round(center)
        for dy in range(1, spire_height + 1):
            blocks.append((tip_x, height + dy, tip_z, params.wall_material))

    blocks, width, depth = _normalize_xz_bounds(blocks)
    # Height from the blocks actually placed, not from merlon_y: merlons
    # are square-only and the spire is taper-only, so a formula-derived
    # height over-reported the bounding box (a y_shape tower claimed 29
    # while topping out at 27), leaving phantom empty courses that made
    # the structure look permanently unfinished.
    actual_height = max(b[1] for b in blocks) + 1
    return blocks, width, actual_height, depth


def generate_lattice_tower(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    # A wrought-iron lattice tower - the Eiffel Tower's actual shape,
    # which "tower" cannot produce. generate_tower extrudes one closed
    # footprint per layer, so its output is always a building: a solid
    # shaft you could walk floors in. The Eiffel Tower is the opposite -
    # four separate splayed legs standing apart, joined by platforms,
    # merging into a single shaft only near the top. You have to be able
    # to see daylight between the legs or it reads as a skyscraper, which
    # is exactly how the old "eiffel" preset came out.
    #
    # Proportions are the real tower's, as fractions of its 330m:
    # first platform 57m (0.17), second 115m (0.35), top floor 276m
    # (0.84), then the antenna mast to the tip.
    height = _scale(30, params, minimum=14)
    base_half = max(3, round(height * 0.28))

    p1_y = max(2, round(height * 0.17))
    p2_y = max(p1_y + 2, round(height * 0.35))
    top_y = max(p2_y + 2, round(height * 0.84))

    p1_half = max(2, round(base_half * 0.56))
    p2_half = max(1, round(base_half * 0.30))
    top_half = max(1, round(base_half * 0.14))

    def half_at(y: int) -> float:
        # Piecewise through the real platform widths. The legs are convex
        # curves in life; across this few blocks the straight segments
        # between platforms are indistinguishable from the curve, and they
        # keep the platform edges landing exactly on a block.
        if y <= p1_y:
            return base_half + (p1_half - base_half) * (y / max(1, p1_y))
        if y <= p2_y:
            return p1_half + (p2_half - p1_half) * ((y - p1_y) / max(1, p2_y - p1_y))
        if y <= top_y:
            return p2_half + (top_half - p2_half) * ((y - p2_y) / max(1, top_y - p2_y))
        return top_half

    blocks: list[Block] = []
    seen: set[tuple[int, int, int]] = set()

    def put(x: int, y: int, z: int, material: str) -> None:
        key = (x, y, z)
        if key not in seen:
            seen.add(key)
            blocks.append((x, y, z, material))

    wall = params.wall_material

    # --- the four legs, standing apart all the way to the second platform
    for y in range(0, p2_y + 1):
        h = half_at(y)
        # Leg thickness tapers with the tower so the legs stay in
        # proportion rather than reading as four heavy pillars up top.
        t = 1 if h < 4 else 2
        for sx in (-1, 1):
            for sz in (-1, 1):
                ox = round(sx * h)
                oz = round(sz * h)
                for dx in range(t):
                    for dz in range(t):
                        put(ox - sx * dx, y, oz - sz * dz, wall)

    # --- the arch between the legs, under the first platform
    arch_rise = max(2, round(p1_y * 0.8))
    for sign in (-1, 1):
        # The arch springs from the inner face of the legs, not from the
        # tower's full half-width - run it to the outer face and it puts
        # stray blocks out past the legs with a gap behind them.
        edge = max(2, round(half_at(0)) - 2)
        for i in range(-edge, edge + 1):
            frac = i / max(1, edge)
            # Semi-ellipse springing from the legs and peaking at centre.
            y = round(arch_rise * math.sqrt(max(0.0, 1.0 - frac * frac)))
            if y <= 0:
                continue
            put(i, y, sign * edge, wall)
            put(sign * edge, y, i, wall)

    # --- platforms: real slabs, wider than the shaft they sit on
    for level_y, level_half in ((p1_y, p1_half), (p2_y, p2_half), (top_y, top_half)):
        overhang = 2 if level_y < top_y else 1
        span = round(half_at(level_y)) + overhang
        for x in range(-span, span + 1):
            for z in range(-span, span + 1):
                put(x, level_y, z, FLOOR_MATERIAL)

    # --- the shaft above the second platform, where the legs have merged
    for y in range(p2_y + 1, top_y):
        h = half_at(y)
        size = max(1, round(h * 2))
        if size <= 2:
            for dx in range(-1, 1):
                for dz in range(-1, 1):
                    put(dx, y, dz, wall)
        else:
            for x, z in _square_outline(0.0, 0.0, size, filled=False):
                put(x, y, z, wall)

    # --- the top floor's cabin, then the antenna mast
    for y in range(top_y + 1, top_y + max(2, round(height * 0.05)) + 1):
        for dx in range(-1, 1):
            for dz in range(-1, 1):
                put(dx, y, dz, wall)

    mast_top = height
    for y in range(top_y + max(2, round(height * 0.05)) + 1, mast_top + 1):
        put(0, y, 0, wall)

    blocks, width, actual_height, depth = _normalize_bounds(blocks)
    return blocks, width, actual_height, depth


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


PLATFORM_MATERIAL = "minecraft:smooth_quartz"

# Onion-dome silhouette control: the radius bulges out past the base
# before tapering to a point, instead of a plain hemisphere shrinking
# monotonically. BULGE_HEIGHT_FRACTION is how far up (as a fraction of
# dome height) the widest point sits; BULGE_FACTOR is how much wider
# than the base radius it gets there.
BULGE_HEIGHT_FRACTION = 0.3
BULGE_FACTOR = 1.2


def _onion_radius(t: float, base_radius: float) -> float:
    if t <= BULGE_HEIGHT_FRACTION:
        # Smooth rise from base_radius to the bulge peak.
        local_t = t / BULGE_HEIGHT_FRACTION
        return base_radius * (1 + (BULGE_FACTOR - 1) * math.sin(math.pi / 2 * local_t))
    # Smooth taper from the bulge peak down to a point at the tip.
    local_t = (t - BULGE_HEIGHT_FRACTION) / (1 - BULGE_HEIGHT_FRACTION)
    return base_radius * BULGE_FACTOR * math.cos(math.pi / 2 * local_t)


def generate_dome(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    # An onion dome, not a plain hemisphere or a cone - the radius
    # bulges outward past the base before tapering to a point (see
    # _onion_radius), which is what actually reads as a real dome
    # silhouette (mosques, the Taj Mahal, many capitol buildings) rather
    # than a smooth half-ball. Sits on a drum, which sits on a real
    # raised square platform - not floating straight off the ground.
    base_radius = _scale(9, params, minimum=5)
    drum_height = max(2, base_radius // 3)
    dome_height = base_radius
    diameter = base_radius * 2 + 1
    center = base_radius

    platform_margin = max(2, base_radius // 3)
    platform_height = max(1, base_radius // 6)
    platform_half = base_radius + platform_margin

    blocks: list[Block] = []

    # Platform: a real raised square base, wider than the dome/drum
    # footprint, in a visually distinct material.
    for x in range(center - platform_half, center + platform_half + 1):
        for z in range(center - platform_half, center + platform_half + 1):
            for y in range(platform_height):
                blocks.append((x, y, z, PLATFORM_MATERIAL))

    for x, z in _circular_outline(center, center, diameter, filled=True):
        blocks.append((x, platform_height, z, FLOOR_MATERIAL))

    for dy in range(1, drum_height + 1):
        y = platform_height + dy
        for x, z in _circular_outline(center, center, diameter, filled=False):
            blocks.append((x, y, z, params.wall_material))

    for dy in range(0, dome_height + 1):
        y = platform_height + drum_height + dy
        t = dy / dome_height
        radius_here = _onion_radius(t, base_radius)
        if radius_here < 0.5:
            blocks.append((center, y, center, params.wall_material))
            continue
        for x, z in _circular_outline(center, center, round(radius_here * 2), filled=False):
            blocks.append((x, y, z, params.wall_material))

    # Small finial spire at the very tip, common on real onion domes.
    finial_y = platform_height + drum_height + dome_height + 1
    for dy in range(3):
        blocks.append((center, finial_y + dy, center, params.wall_material))

    blocks, width, depth = _normalize_xz_bounds(blocks)
    return blocks, width, finial_y + 3, depth


ENGINE_MATERIAL = "minecraft:iron_block"
COCKPIT_MATERIAL = "minecraft:black_concrete"


def generate_aircraft(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    # A fuselage + swept delta wings + a tail fin + engine pods - none of
    # this is "a resized box," it's a genuinely different topology from
    # every other archetype. X = nose-to-tail length, Y = height,
    # Z = wingspan. Wings sweep backward and taper to a point outward
    # (a real delta silhouette, not a rectangle), matching how a
    # Concorde-style aircraft actually looks from above.
    length = _scale(24, params, minimum=14)
    fuselage_radius = max(2, length // 10)
    # Minimum bumped from +3 to +7: a realistic narrow wingspan_ratio
    # (~0.45-0.5, correct for a Concorde) combined with the old minimum
    # produced only 3-4 blocks of actual wing at medium scale - thin
    # enough to read as an unrecognizable stub rather than a wing.
    half_span = max(fuselage_radius + 7, round(length * params.wingspan_ratio / 2))

    y_center = fuselage_radius + 3
    z_center = half_span + 2

    blocks: list[Block] = []

    # Fuselage: tapers to a point at the nose, tapers (less sharply) at
    # the tail, constant radius in between.
    for x in range(length):
        if x < length * 0.18:
            t = x / max(1, length * 0.18)
            radius_here = max(1, round(fuselage_radius * t))
        elif x > length * 0.85:
            t = (length - 1 - x) / max(1, length * 0.15)
            radius_here = max(1, round(fuselage_radius * max(t, 0.2)))
        else:
            radius_here = fuselage_radius

        for a, b in _circular_outline(y_center, z_center, radius_here * 2, filled=False):
            blocks.append((x, a, b, params.wall_material))

    # Cockpit windows: a small dark band near the nose.
    cockpit_x = round(length * 0.14)
    blocks.append((cockpit_x, y_center + fuselage_radius // 2, z_center, COCKPIT_MATERIAL))

    # Wings: delta shape - widest (front-to-back) at the fuselage,
    # sweeping back and narrowing to a point at the tip.
    wing_root_x = round(length * 0.4)
    wing_root_chord = round(length * 0.3)
    sweep = 0.85
    taper = 0.45

    wing_span_available = half_span - fuselage_radius
    for s in range(1, wing_span_available + 1):
        leading_x = wing_root_x + round(s * sweep)
        chord = max(1, wing_root_chord - round(s * taper))
        trailing_x = leading_x + chord
        for x in range(leading_x, trailing_x + 1):
            for side in (1, -1):
                z = z_center + side * (fuselage_radius + s)
                # 2 blocks thick vertically - a single-layer wing was
                # nearly invisible at typical viewing distance/angle.
                blocks.append((x, y_center, z, params.wall_material))
                blocks.append((x, y_center - 1, z, params.wall_material))

    # Tail fin: a swept vertical fin, tallest where it meets the
    # fuselage and tapering down toward the tail tip.
    tail_start_x = round(length * 0.82)
    tail_height = max(2, fuselage_radius * 3)
    tail_span = max(1, length - 1 - tail_start_x)
    for x in range(tail_start_x, length):
        progress = (x - tail_start_x) / tail_span
        fin_height = round(tail_height * (1 - progress))
        for h in range(1, fin_height + 1):
            blocks.append((x, y_center + fuselage_radius + h, z_center, params.wall_material))

    # Engine pods: small cylinders hanging under the wings, spaced along
    # the span - engine_count of them, split evenly left/right.
    pod_length = max(3, length // 8)
    pod_radius = max(1, fuselage_radius // 2)
    pairs = max(1, params.engine_count // 2) if params.engine_count > 0 else 0
    for pair in range(pairs):
        span_fraction = 0.35 + pair * (0.4 / max(1, pairs))
        span_offset = max(1, round(wing_span_available * span_fraction))
        pod_x = wing_root_x + round(span_offset * sweep) - pod_length // 2
        pod_y = y_center - fuselage_radius - pod_radius - 1
        for side in (1, -1):
            pod_z = z_center + side * (fuselage_radius + span_offset)
            for dx in range(pod_length):
                for a, b in _circular_outline(pod_y, pod_z, pod_radius * 2, filled=True):
                    blocks.append((pod_x + dx, a, b, ENGINE_MATERIAL))

    blocks, width, height, depth = _normalize_bounds(blocks)
    return blocks, width, height, depth


WHEEL_MATERIAL = "minecraft:black_concrete"
WINDOW_MATERIAL = "minecraft:light_blue_stained_glass"


def generate_vehicle(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    # A ground vehicle: a boxy body shell (low "hood" up front, taller
    # cabin from roughly a third of the way back) riding on 4 wheels -
    # covers the exact gap that sent "bmw x5 2015" to a flat photo
    # mosaic instead of any real geometry.
    length = _scale(8, params, minimum=5)
    width = max(3, round(length * 0.55))
    wheel_radius = max(1, length // 7)
    body_height = max(2, length // 3)
    y0 = wheel_radius  # body rides on top of the wheels

    cabin_start = round(length * 0.28)

    blocks: list[Block] = []

    for x in range(length):
        tall = x >= cabin_start
        top_y = y0 + body_height if tall else y0 + max(1, body_height - 2)
        for z in range(width):
            edge = x == 0 or x == length - 1 or z == 0 or z == width - 1
            blocks.append((x, y0, z, params.wall_material))
            if edge:
                for y in range(y0 + 1, top_y):
                    blocks.append((x, y, z, params.wall_material))
            blocks.append((x, top_y, z, params.wall_material))
        if tall:
            window_y = y0 + body_height - 1
            blocks.append((x, window_y, 0, WINDOW_MATERIAL))
            blocks.append((x, window_y, width - 1, WINDOW_MATERIAL))

    # 4 wheels, one flat disk per corner (in the side/x-y plane, one
    # block deep at each side of the body).
    for x_pos in (wheel_radius, length - 1 - wheel_radius):
        for z_pos in (0, width - 1):
            for a, b in _circular_outline(wheel_radius, x_pos, wheel_radius * 2, filled=True):
                blocks.append((b, a, z_pos, WHEEL_MATERIAL))

    blocks, width, height, depth = _normalize_bounds(blocks)
    return blocks, width, height, depth


DECK_MATERIAL = "minecraft:dark_oak_planks"
MAST_MATERIAL = "minecraft:oak_log"


def generate_ship(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    # A hull tapering to a point at the bow, full beam from ~30% back to
    # a flat stern, with a mast and a crossbeam yard - a real ship
    # silhouette, not a tower lying on its side.
    length = _scale(20, params, minimum=12)
    beam = max(4, round(length * 0.28))
    hull_height = max(2, length // 8)
    deck_y = hull_height
    mast_height = max(4, length // 3)
    center_z = beam // 2

    blocks: list[Block] = []

    for x in range(length):
        if x < length * 0.3:
            t = x / max(1.0, length * 0.3)
            half_width = max(1, round(center_z * t))
        else:
            half_width = center_z

        for z in range(center_z - half_width, center_z + half_width + 1):
            for y in range(hull_height):
                edge = z in (center_z - half_width, center_z + half_width) or x == 0
                if edge or y == 0:
                    blocks.append((x, y, z, params.wall_material))
            blocks.append((x, deck_y, z, DECK_MATERIAL))

    mast_x = round(length * 0.4)
    for y in range(deck_y, deck_y + mast_height):
        blocks.append((mast_x, y, center_z, MAST_MATERIAL))

    yard_y = deck_y + round(mast_height * 0.6)
    yard_half = min(center_z, 3)
    for dz in range(-yard_half, yard_half + 1):
        blocks.append((mast_x, yard_y, center_z + dz, MAST_MATERIAL))

    blocks, width, height, depth = _normalize_bounds(blocks)
    return blocks, width, height, depth


CABLE_MATERIAL = "minecraft:chain"


def generate_bridge(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    # A genuinely different topology from every other archetype: a
    # horizontal span, not a centered vertical structure. Two towers,
    # a flat deck between them, and a sagging suspension cable with
    # vertical hangers down to the deck - the actual defining silhouette
    # of a suspension bridge (Golden Gate, Brooklyn Bridge, ...).
    # Capped at 60, not just scaled - "huge" (1.8x) would put a 40-block
    # base span at 72, over BlueprintValidator's 64-block dimension cap,
    # which would fail validation outright rather than just looking bad.
    span = min(60, _scale(40, params, minimum=24))
    tower_height = max(6, span // 4)
    deck_y = max(2, tower_height // 2)
    deck_width = max(3, span // 10)
    tower_positions = (round(span * 0.15), round(span * 0.85))

    blocks: list[Block] = []

    for x in range(span):
        for z in range(deck_width):
            blocks.append((x, deck_y, z, params.wall_material))

    for tx in tower_positions:
        for z in (0, deck_width - 1):
            for y in range(tower_height + 1):
                blocks.append((tx, y, z, params.wall_material))
        for z in range(deck_width):
            blocks.append((tx, tower_height, z, params.wall_material))

    half_span = span / 2
    for z in (0, deck_width - 1):
        for x in range(span):
            t = (x - half_span) / half_span
            cable_y = min(tower_height, deck_y + round((tower_height - deck_y) * (t * t)))
            blocks.append((x, cable_y, z, CABLE_MATERIAL))
            if x % 4 == 0:
                for y in range(deck_y, cable_y):
                    blocks.append((x, y, z, CABLE_MATERIAL))

    blocks, width, height, depth = _normalize_bounds(blocks)
    return blocks, width, height, depth


GENERATORS = {
    "house": generate_house,
    "tower": generate_tower,
    "lattice_tower": generate_lattice_tower,
    "temple": generate_temple,
    "wheel": generate_wheel,
    "dome": generate_dome,
    "aircraft": generate_aircraft,
    "vehicle": generate_vehicle,
    "ship": generate_ship,
    "bridge": generate_bridge,
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

    # Slightly past the platform's edge, not hugging the dome - real
    # corner minarets (the Taj Mahal, many mosques) stand well clear of
    # the main structure on their own corners of the platform.
    placement_radius = main_span / 2 * 1.15 + accessory_span / 2 + 2

    # 4 accessories reads as "corner towers" (the common real case) only
    # if they land on the diagonals, not the cardinal N/E/S/W midpoints -
    # offset the starting angle by 45 degrees so they do.
    start_angle = math.pi / 4 if params.accessory_count == 4 else 0.0

    combined = list(blocks)
    for i in range(params.accessory_count):
        angle = start_angle + 2 * math.pi * i / params.accessory_count
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

    # shape_description is the LLM's freeform geometry reasoning, written
    # before it committed to any structured field - surfaced to the
    # player as part of "reasoning" since the API contract doesn't have
    # a separate field for it, rather than silently discarding it.
    if params.shape_description:
        reasoning = params.shape_description + (
            f" {params.reasoning}" if params.reasoning else ""
        )
    else:
        reasoning = params.reasoning

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
        "reasoning": reasoning,
        "attributes": params.attributes,
    }
