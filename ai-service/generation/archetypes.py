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


def generate_tower(params: GenerationParams) -> tuple[list[Block], int, int, int]:
    size = _scale(5, params, minimum=3)
    height = _scale(15, params, minimum=6)
    merlon_y = height + 1
    lean_x = params.lean_direction == "x"

    max_offset = _lean_offset(merlon_y, params)
    width = size + max_offset if lean_x else size
    depth = size + max_offset if not lean_x else size

    blocks: list[Block] = []

    for x in range(size):
        for z in range(size):
            blocks.append((x, 0, z, FLOOR_MATERIAL))

    def ring(y: int, material: str, step: int = 1) -> None:
        offset = _lean_offset(y, params)
        dx, dz = (offset, 0) if lean_x else (0, offset)
        for x in range(0, size, step):
            blocks.append((x + dx, y, 0 + dz, material))
            blocks.append((x + dx, y, size - 1 + dz, material))
        for z in range(step, size - 1, step):
            blocks.append((0 + dx, y, z + dz, material))
            blocks.append((size - 1 + dx, y, z + dz, material))

    for y in range(1, height + 1):
        ring(y, params.wall_material)

    # Merlons: every other position, top ring only.
    ring(merlon_y, params.wall_material, step=2)

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


GENERATORS = {
    "house": generate_house,
    "tower": generate_tower,
    "temple": generate_temple,
}


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
