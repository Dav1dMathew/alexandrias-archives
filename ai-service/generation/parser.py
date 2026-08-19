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
        "church", "chapel", "synagogue", "monastery", "abbey",
        "ziggurat", "pyramid",
        # grand civic / government buildings
        "forum", "capitol", "parliament", "senate", "courthouse",
        "museum", "library", "monument", "memorial", "palace", "mansion",
        "villa", "estate", "manor",
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
    "wheel": {
        "wheel", "carousel", "ferris",
    },
    "dome": {
        "dome", "mausoleum", "tomb", "mosque", "cathedral", "basilica",
        "planetarium", "rotunda", "observatory", "capitol_dome",
    },
    "aircraft": {
        "aircraft", "plane", "airplane", "jet", "airliner", "airbus",
        "boeing", "concorde", "fighter", "biplane", "seaplane", "glider",
    },
    "vehicle": {
        "car", "vehicle", "truck", "van", "suv", "bus", "taxi", "jeep",
        "limousine", "tank", "locomotive", "train", "carriage", "wagon",
        "chariot", "buggy",
    },
    "ship": {
        "ship", "boat", "vessel", "yacht", "galleon", "frigate",
        "battleship", "submarine", "canoe", "sailboat", "schooner",
        "titanic", "ark", "longship", "steamship", "ferry",
    },
    "bridge": {
        "bridge", "viaduct", "aqueduct", "causeway", "overpass",
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
    "metal": "minecraft:iron_block",
    "metallic": "minecraft:iron_block",
    "steel": "minecraft:iron_block",
    "aluminum": "minecraft:light_gray_concrete",
    "aluminium": "minecraft:light_gray_concrete",
    "silver": "minecraft:light_gray_concrete",
    "brick": "minecraft:bricks",
    "red": "minecraft:bricks",
    "gold": "minecraft:gold_block",
    "golden": "minecraft:gold_block",
    "black": "minecraft:black_concrete",
    "dark": "minecraft:black_concrete",
    "concrete": "minecraft:light_gray_concrete",
    "modern": "minecraft:light_gray_concrete",
    "glass": "minecraft:glass",
}
DEFAULT_MATERIAL = "minecraft:stone_bricks"

# The fixed set of block IDs any parser (keyword or LLM) is allowed to
# choose - constrains free-form model output to values StructureBuilder/
# BlueprintValidator can actually place, instead of trusting the model to
# invent a valid Minecraft block ID.
VALID_MATERIALS = sorted({DEFAULT_MATERIAL, *MATERIAL_KEYWORDS.values()})
VALID_ARCHETYPES = (
    "house", "tower", "temple", "wheel", "dome", "aircraft", "vehicle",
    "ship", "bridge",
)
VALID_SCALE_TIERS = ("mini", "medium", "huge")
VALID_LEAN_DIRECTIONS = ("x", "z")
# Clamp: a real-world lean beyond this looks absurd and blows up the
# bounding box (offset grows with height * tan(angle)).
MAX_LEAN_DEGREES = 15.0

# Tower shape - the actual silhouette, not just a resized box.
VALID_CROSS_SECTIONS = ("square", "circular", "y_shape")
DEFAULT_CROSS_SECTION = "square"

MAX_ACCESSORY_COUNT = 8

# Aircraft shape - not resizable box territory at all: a fuselage,
# swept wings, and engine pods.
MIN_WINGSPAN_RATIO, MAX_WINGSPAN_RATIO = 0.4, 1.2
DEFAULT_WINGSPAN_RATIO = 0.7
MAX_ENGINE_COUNT = 4
DEFAULT_ENGINE_COUNT = 2


@dataclass
class GenerationParams:
    archetype: str
    scale_tier: str
    wall_material: str
    prompt: str
    # Populated only by the LLM-backed parser (llm_parser.py). None here
    # means "keyword-matched, no per-attribute provenance available."
    reasoning: str | None = None
    attributes: list[dict] | None = None
    # Real geometric parameters, not just archetype selection - e.g. the
    # Leaning Tower of Pisa's actual lean, not a generic tower. Only
    # meaningful for the "tower" archetype today; 0.0/"x" is a no-op lean
    # for everything else.
    lean_angle_degrees: float = 0.0
    lean_direction: str = "x"
    # Tower silhouette. cross_section changes the actual footprint shape
    # per layer (square/circular/tri-lobed "Y", the Burj Khalifa's real
    # defining feature); taper_ratio shrinks the footprint from base to
    # top (0 = no taper, 0.8 = nearly a point); setback_tiers > 0 makes
    # that shrink happen in discrete steps instead of smoothly.
    cross_section: str = "square"
    taper_ratio: float = 0.0
    setback_tiers: int = 0
    # Multi-part structures - e.g. the Taj Mahal's 4 freestanding corner
    # minarets around its central dome. accessory_count small towers get
    # generated and merged in around the main structure's perimeter.
    accessory_count: int = 0
    # Aircraft-only: wingspan relative to fuselage length (0.4 = stubby,
    # 1.2 = very wide-winged), and how many engine pods hang under the
    # wings (0-4).
    wingspan_ratio: float = DEFAULT_WINGSPAN_RATIO
    engine_count: int = DEFAULT_ENGINE_COUNT
    # Freeform, pre-parameter reasoning the LLM wrote before committing
    # to any structured field - e.g. describing the Concorde's actual
    # fuselage/wing/engine geometry before picking numbers for it. None
    # for the keyword parser (nothing to reason about).
    shape_description: str | None = None


def _tokenize(prompt: str) -> set[str]:
    return set(re.findall(r"[a-z']+", prompt.lower()))


def _match_keyword(tokens: set[str], keyword_map: dict[str, set[str]]) -> str | None:
    # Returns None when nothing matched, distinct from a real match that
    # happens to equal the default (e.g. "log cabin" genuinely matching
    # "house" is not the same as no keyword matching at all).
    for label, keywords in keyword_map.items():
        if tokens & keywords:
            return label
    return None


def match_archetype(prompt: str) -> str | None:
    """Archetype the keyword parser actually matched, or None if it had to default."""
    return _match_keyword(_tokenize(prompt), ARCHETYPE_KEYWORDS)


def parse_prompt(prompt: str) -> GenerationParams:
    tokens = _tokenize(prompt)

    archetype = _match_keyword(tokens, ARCHETYPE_KEYWORDS) or DEFAULT_ARCHETYPE
    scale_tier = _match_keyword(tokens, SCALE_KEYWORDS) or DEFAULT_SCALE

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
