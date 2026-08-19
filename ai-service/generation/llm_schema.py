"""Shared extraction schema for every LLM backend (Ollama, Groq, ...).

The prompt text and the sanitization/validation logic are identical
regardless of which model answers it - only the HTTP call and response
envelope differ per backend (llm_parser.py for Ollama, groq_parser.py
for Groq). Keeping this in one place means adding a new backend is just
"send this prompt, get JSON text back, hand it to parse_llm_json()".
"""

from .parser import (
    GenerationParams,
    VALID_ARCHETYPES,
    VALID_SCALE_TIERS,
    VALID_MATERIALS,
    VALID_LEAN_DIRECTIONS,
    MAX_LEAN_DEGREES,
    VALID_CROSS_SECTIONS,
    DEFAULT_CROSS_SECTION,
    DEFAULT_ARCHETYPE,
    DEFAULT_SCALE,
    DEFAULT_MATERIAL,
    MAX_ACCESSORY_COUNT,
    MIN_WINGSPAN_RATIO,
    MAX_WINGSPAN_RATIO,
    DEFAULT_WINGSPAN_RATIO,
    MAX_ENGINE_COUNT,
    DEFAULT_ENGINE_COUNT,
)

MAX_TAPER_RATIO = 0.85
MAX_SETBACK_TIERS = 8

VALID_PROVENANCE = (
    "documented", "corroborated", "inferred", "ai_generated", "unknown",
)

_PROMPT_TEMPLATE = """You are extracting structured building parameters for a Minecraft \
reconstruction of: "{prompt}"

Return ONLY a single JSON object, no other text, matching exactly this shape. \
"shape_description" MUST be the first field you write, and every field after it \
should follow from what you wrote there - think through the object's actual real-world \
geometry in that field BEFORE picking any of the structured parameters below it, the \
same way you'd sketch something before measuring it:
{{
  "shape_description": "2-3 sentences describing this subject's ACTUAL real-world \
geometry in plain engineering terms - its overall form, proportions, and any distinctive \
structural features (a bulging dome, a tapering spire, delta wings, a ring shape, \
corner towers, etc). Be concrete: rough proportions, what's symmetric, what's \
distinctive. If you don't know the subject, describe what a generic building of its \
apparent type would look like instead of guessing specifics.",
  "archetype": one of {archetypes} - use "wheel" for anything that is fundamentally \
a ring/wheel shape (a Ferris wheel, an observation wheel like the London Eye, a \
carousel) - this is a real, different shape (a ring standing upright), not a resized \
tower. Use "dome" for anything whose defining feature is a rounded dome roof (a \
mosque, a mausoleum like the Taj Mahal, a capitol building, a basilica) - this is a \
real rounded hemisphere, not a tapering "tower". Use "aircraft" for any airplane/jet \
(a fuselage with swept wings and engines) - do not force it into "tower". Do not force \
a domed, wheel-shaped, or aircraft-shaped structure into "tower".
  "accessory_count": an integer from 0 to {max_accessories} - number of freestanding \
small towers/minarets around the main structure's perimeter, ONLY if this building is \
specifically known for them (e.g. the Taj Mahal has exactly 4 corner minarets). 0 for \
anything without genuinely separate corner towers - do not guess a number. Irrelevant \
for "aircraft".
  "wingspan_ratio": a number from {min_wingspan} to {max_wingspan} - wingspan relative \
to fuselage length. Around 0.5-0.6 for a narrow-winged supersonic jet like the \
Concorde, 0.8-1.0 for a typical wide-winged airliner, higher for a glider. Only \
relevant for "aircraft", ignore otherwise.
  "engine_count": an integer from 0 to {max_engines} - how many engines mounted under \
the wings (real jets: usually 2 or 4, evenly split left/right). Only relevant for \
"aircraft", ignore otherwise.
  "scale_tier": one of {scales},
  "material": one of {materials},
  "lean_angle_degrees": a number from 0 to {max_lean} - ONLY nonzero if this specific \
building is actually, famously known to lean or tilt as a defining feature, the way \
the Leaning Tower of Pisa is famous specifically FOR leaning. A building being very \
tall, modern, or unusually shaped is NOT the same as leaning - e.g. the Burj Khalifa \
is extremely tall but does NOT lean, so it must get 0. When in doubt, use 0.
  "lean_direction": one of {lean_directions} - the axis it leans toward, irrelevant if \
lean_angle_degrees is 0,
  "cross_section": one of {cross_sections} - the tower's actual floor-plan shape. \
"square" is the default/normal case for most towers, skyscrapers and castles. \
"circular" for genuinely cylindrical towers (silos, some lighthouses/minarets). \
"y_shape" ONLY for a building whose floor plan is a well-known tri-lobed/Y shape - \
the Burj Khalifa is the definitive example. Do not guess "y_shape" - use it only if \
you are confident.
  "taper_ratio": a number from 0 to {max_taper} - how much the tower narrows from base \
to tip. 0 means constant width top to bottom (true for most buildings, including most \
towers/castles). Only use a higher value (0.3-0.7) for a building genuinely famous for \
tapering to a point, like the Burj Khalifa, an obelisk, or a spire. 0 if unsure.
  "setback_tiers": an integer from 0 to {max_setbacks} - number of visible stepped \
setback tiers as the building narrows going up (0 = no visible steps). Only nonzero \
for a building genuinely known for a "wedding cake"/ziggurat stepped profile. 0 if \
unsure.
  "reasoning": "one sentence explaining the archetype/scale/lean/shape choice",
  "attributes": [
    {{"field": "<short field name>", "value": "<short value>", "provenance": one of {provenance}}}
  ]
}}

Rules for "attributes": list 2-4 real facts about the subject if you have any \
(dimensions, era, purpose, notable feature). Label each one honestly:
- "documented": genuinely well-established historical/architectural fact
- "corroborated": supported by general knowledge but not precisely certain
- "inferred": a reasonable guess based on the building's type/era, not a specific fact
- "ai_generated": invented purely to fill in a gap, not based on real information
- "unknown": you don't actually know this and are not guessing

If the prompt does not name a real building, use "unknown" for every attribute and \
lean_angle_degrees of 0. Do not mark anything "documented" unless you are genuinely \
confident it's correct. Do not invent a lean that doesn't really exist."""


def build_extraction_prompt(prompt: str) -> str:
    return _PROMPT_TEMPLATE.format(
        prompt=prompt,
        archetypes=list(VALID_ARCHETYPES),
        scales=list(VALID_SCALE_TIERS),
        materials=list(VALID_MATERIALS),
        provenance=list(VALID_PROVENANCE),
        max_lean=MAX_LEAN_DEGREES,
        lean_directions=list(VALID_LEAN_DIRECTIONS),
        cross_sections=list(VALID_CROSS_SECTIONS),
        max_taper=MAX_TAPER_RATIO,
        max_setbacks=MAX_SETBACK_TIERS,
        max_accessories=MAX_ACCESSORY_COUNT,
        min_wingspan=MIN_WINGSPAN_RATIO,
        max_wingspan=MAX_WINGSPAN_RATIO,
        max_engines=MAX_ENGINE_COUNT,
    )


def parse_llm_json(parsed, prompt: str) -> GenerationParams | None:
    if not isinstance(parsed, dict):
        return None

    archetype = parsed.get("archetype")
    scale_tier = parsed.get("scale_tier")
    material = parsed.get("material")

    if archetype not in VALID_ARCHETYPES:
        archetype = DEFAULT_ARCHETYPE
    if scale_tier not in VALID_SCALE_TIERS:
        scale_tier = DEFAULT_SCALE
    if material not in VALID_MATERIALS:
        material = DEFAULT_MATERIAL

    reasoning = parsed.get("reasoning")
    if not isinstance(reasoning, str):
        reasoning = None

    shape_description = parsed.get("shape_description")
    if not isinstance(shape_description, str) or not shape_description.strip():
        shape_description = None

    attributes = _sanitize_attributes(parsed.get("attributes"))

    lean_angle, lean_direction = _sanitize_lean(
        parsed.get("lean_angle_degrees"), parsed.get("lean_direction"), archetype,
    )
    cross_section, taper_ratio, setback_tiers = _sanitize_shape(
        parsed.get("cross_section"),
        parsed.get("taper_ratio"),
        parsed.get("setback_tiers"),
        archetype,
    )

    try:
        accessory_count = max(0, min(int(parsed.get("accessory_count", 0)), MAX_ACCESSORY_COUNT))
    except (TypeError, ValueError):
        accessory_count = 0

    wingspan_ratio, engine_count = _sanitize_aircraft(
        parsed.get("wingspan_ratio"), parsed.get("engine_count"), archetype,
    )

    return GenerationParams(
        archetype=archetype,
        scale_tier=scale_tier,
        wall_material=material,
        prompt=prompt,
        reasoning=reasoning,
        attributes=attributes,
        lean_angle_degrees=lean_angle,
        lean_direction=lean_direction,
        cross_section=cross_section,
        taper_ratio=taper_ratio,
        setback_tiers=setback_tiers,
        accessory_count=accessory_count,
        wingspan_ratio=wingspan_ratio,
        engine_count=engine_count,
        shape_description=shape_description,
    )


def _sanitize_aircraft(raw_wingspan, raw_engines, archetype: str) -> tuple[float, int]:
    if archetype != "aircraft":
        return DEFAULT_WINGSPAN_RATIO, DEFAULT_ENGINE_COUNT

    try:
        wingspan_ratio = max(MIN_WINGSPAN_RATIO, min(float(raw_wingspan), MAX_WINGSPAN_RATIO))
    except (TypeError, ValueError):
        wingspan_ratio = DEFAULT_WINGSPAN_RATIO

    try:
        engine_count = max(0, min(int(raw_engines), MAX_ENGINE_COUNT))
    except (TypeError, ValueError):
        engine_count = DEFAULT_ENGINE_COUNT

    return wingspan_ratio, engine_count


def _sanitize_shape(raw_cross_section, raw_taper, raw_setbacks, archetype: str) -> tuple[str, float, int]:
    if archetype != "tower":
        return DEFAULT_CROSS_SECTION, 0.0, 0

    cross_section = (
        raw_cross_section if raw_cross_section in VALID_CROSS_SECTIONS
        else DEFAULT_CROSS_SECTION
    )

    try:
        taper_ratio = max(0.0, min(float(raw_taper), MAX_TAPER_RATIO))
    except (TypeError, ValueError):
        taper_ratio = 0.0

    try:
        setback_tiers = max(0, min(int(raw_setbacks), MAX_SETBACK_TIERS))
    except (TypeError, ValueError):
        setback_tiers = 0

    return cross_section, taper_ratio, setback_tiers


def _sanitize_lean(raw_angle, raw_direction, archetype: str) -> tuple[float, str]:
    if archetype != "tower":
        return 0.0, "x"

    try:
        angle = float(raw_angle)
    except (TypeError, ValueError):
        return 0.0, "x"

    if angle <= 0:
        return 0.0, "x"

    angle = min(angle, MAX_LEAN_DEGREES)
    direction = raw_direction if raw_direction in VALID_LEAN_DIRECTIONS else "x"
    return angle, direction


def _sanitize_attributes(raw_attributes) -> list[dict] | None:
    if not isinstance(raw_attributes, list):
        return None

    cleaned = []
    for entry in raw_attributes:
        if not isinstance(entry, dict):
            continue
        field = entry.get("field")
        value = entry.get("value")
        provenance = entry.get("provenance")
        if not isinstance(field, str) or not isinstance(value, str):
            continue
        if provenance not in VALID_PROVENANCE:
            provenance = "unknown"
        cleaned.append({"field": field, "value": value, "provenance": provenance})

    return cleaned or None
