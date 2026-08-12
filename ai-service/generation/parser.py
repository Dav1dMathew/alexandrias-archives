"""Tier 0 prompt parser.

Turns a free-text prompt into structured GenerationParams. This is a
keyword-based stand-in for an LLM call: same input/output shape, so the
matching logic in generate_blueprint() never needs to change when a real
LLM-backed parser replaces this one.
"""

from dataclasses import dataclass
import re

ARCHETYPE_KEYWORDS: dict[str, set[str]] = {
    "temple": {
        # amphitheatres / arenas
        "colosseum", "coliseum", "amphitheater", "amphitheatre", "arena",
        "stadium", "hippodrome",
        # temples / religious & ceremonial buildings
        "temple", "pantheon", "acropolis", "shrine", "cathedral", "basilica",
        "church", "chapel", "mosque", "synagogue", "monastery", "abbey",
        "ziggurat", "pyramid", "mausoleum", "tomb",
        # grand civic / government buildings
        "forum", "palace", "capitol", "parliament", "senate", "courthouse",
        "museum", "library", "monument", "memorial", "mansion", "villa",
        "estate", "manor",
    },
    "tower": {
        "tower", "lighthouse", "spire", "minaret", "obelisk", "skyscraper",
        "belltower", "campanile", "castle", "fortress", "fort", "keep",
        "citadel", "stronghold", "turret", "silo", "windmill", "watchtower",
        "pagoda", "ziggurat_tower", "clocktower",
    },
    "house": {
        "house", "cabin", "cottage", "hut", "home", "farmhouse", "bungalow",
        "shack", "lodge", "cottage", "dwelling", "shed", "barn", "inn",
        "tavern", "hostel", "apartment", "flat", "hovel",
    },
}
DEFAULT_ARCHETYPE = "house"

SCALE_KEYWORDS: dict[str, set[str]] = {
    "mini": {"mini", "tiny", "small", "miniature"},
    "huge": {
        "huge", "giant", "massive", "enormous", "megalophobia", "colossal",
        "gigantic",
    },
}
DEFAULT_SCALE = "medium"

MATERIAL_KEYWORDS: dict[str, str] = {
    "wood": "minecraft:oak_planks",
    "wooden": "minecraft:oak_planks",
    "timber": "minecraft:oak_planks",
    "log": "minecraft:oak_planks",
    "marble": "minecraft:quartz_block",
    "white": "minecraft:quartz_block",
    "classical": "minecraft:quartz_block",
    "roman": "minecraft:quartz_block",
    "greek": "minecraft:quartz_block",
}
DEFAULT_MATERIAL = "minecraft:stone_bricks"


@dataclass
class GenerationParams:
    archetype: str
    scale_tier: str
    wall_material: str
    prompt: str


def _tokenize(prompt: str) -> set[str]:
    return set(re.findall(r"[a-z']+", prompt.lower()))


def _match_keyword(tokens: set[str], keyword_map: dict[str, set[str]], default: str) -> str:
    for label, keywords in keyword_map.items():
        if tokens & keywords:
            return label
    return default


def parse_prompt(prompt: str) -> GenerationParams:
    tokens = _tokenize(prompt)

    archetype = _match_keyword(tokens, ARCHETYPE_KEYWORDS, DEFAULT_ARCHETYPE)
    scale_tier = _match_keyword(tokens, SCALE_KEYWORDS, DEFAULT_SCALE)

    wall_material = DEFAULT_MATERIAL
    for token in tokens:
        if token in MATERIAL_KEYWORDS:
            wall_material = MATERIAL_KEYWORDS[token]
            break

    return GenerationParams(
        archetype=archetype,
        scale_tier=scale_tier,
        wall_material=wall_material,
        prompt=prompt,
    )
