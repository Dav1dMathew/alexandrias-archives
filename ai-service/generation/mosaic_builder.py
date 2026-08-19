"""Turns a real photo into an extruded Minecraft block mosaic.

Each pixel becomes a column of the nearest-color Minecraft concrete
block, extruded a few blocks deep for real volume - not a paper-thin
mural. Unlike the archetype generators, this doesn't guess a shape at
all - it reproduces the actual photo's silhouette and color directly,
so it's the fallback for anything that doesn't fit house/tower/temple/
wheel (a Ferris wheel's support structure, a domed building, anything
with real color/detail a template can't capture). Still not a walkable
interior - a solid relief sculpture, not a hollow building.
"""

import io

from PIL import Image

Block = tuple[int, int, int, str]

MAX_MOSAIC_DIMENSION = 64  # stays within BlueprintValidator's cap

# Minecraft concrete blocks: solid, saturated, reliable colors - the
# standard palette choice for block-art. (block_id, approximate average RGB)
_PALETTE: list[tuple[str, tuple[int, int, int]]] = [
    ("minecraft:white_concrete", (207, 213, 214)),
    ("minecraft:light_gray_concrete", (125, 125, 115)),
    ("minecraft:gray_concrete", (54, 57, 61)),
    ("minecraft:black_concrete", (8, 8, 8)),
    ("minecraft:brown_concrete", (96, 60, 32)),
    ("minecraft:red_concrete", (142, 32, 32)),
    ("minecraft:orange_concrete", (224, 97, 1)),
    ("minecraft:yellow_concrete", (240, 175, 21)),
    ("minecraft:lime_concrete", (94, 168, 24)),
    ("minecraft:green_concrete", (73, 91, 36)),
    ("minecraft:cyan_concrete", (21, 119, 136)),
    ("minecraft:light_blue_concrete", (36, 137, 199)),
    ("minecraft:blue_concrete", (44, 46, 143)),
    ("minecraft:purple_concrete", (100, 32, 156)),
    ("minecraft:magenta_concrete", (169, 48, 159)),
    ("minecraft:pink_concrete", (214, 101, 143)),
]


# Blocks budget for the depth extrusion below - keeps well under
# BlueprintValidator's 50,000 block cap even at the max 64x64 footprint.
_MAX_MOSAIC_BLOCKS = 40_000
_MIN_DEPTH, _MAX_DEPTH = 3, 10


def build_mosaic(image_bytes: bytes) -> tuple[list[Block], int, int, int] | None:
    """Returns (blocks, width, height, depth) reproducing the image as a
    solid extruded volume (same color straight back through the depth,
    not a real depth map) - genuine 3D presence rather than a 1-block
    paper-thin wall, or None if the bytes aren't a decodable image."""
    try:
        image = Image.open(io.BytesIO(image_bytes)).convert("RGB")
    except Exception:
        return None

    width, height = image.size
    scale = min(1.0, MAX_MOSAIC_DIMENSION / max(width, height))
    new_width = max(1, round(width * scale))
    new_height = max(1, round(height * scale))
    image = image.resize((new_width, new_height), Image.LANCZOS)

    depth = max(_MIN_DEPTH, min(_MAX_DEPTH, _MAX_MOSAIC_BLOCKS // max(new_width * new_height, 1)))

    blocks: list[Block] = []
    pixels = image.load()
    for row in range(new_height):
        for col in range(new_width):
            block_id = _nearest_block(pixels[col, row])
            # Image row 0 is the top of the photo - Minecraft y increases
            # upward, so flip vertically to keep the mural right-side up.
            y = new_height - 1 - row
            for z in range(depth):
                blocks.append((col, y, z, block_id))

    return blocks, new_width, new_height, depth


def _nearest_block(rgb: tuple[int, int, int]) -> str:
    best_block = _PALETTE[0][0]
    best_distance = float("inf")
    for block_id, palette_rgb in _PALETTE:
        distance = sum((a - b) ** 2 for a, b in zip(rgb, palette_rgb))
        if distance < best_distance:
            best_distance = distance
            best_block = block_id
    return best_block
