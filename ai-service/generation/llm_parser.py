"""LLM-backed prompt parser (local, free - runs against Ollama).

Same input/output shape as parser.parse_prompt(), per the report's own
architecture note: the keyword parser was always meant to be a drop-in
stand-in for this. The real value over the keyword version is provenance -
each extracted attribute is labeled with how confident the model actually
is, per Alexandria's Archives' own trust framework (Documented /
Corroborated / Inferred / AI-generated / Unknown), instead of silently
presenting a guess as fact.

Never raises out to the caller: any failure (Ollama not running, timeout,
malformed/invalid JSON, an enum value the model invented) returns None,
which signals main.py to fall back to the keyword parser. A wrong or
missing local model must never break /generate.
"""

import json
import urllib.request
import urllib.error

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
)

MAX_TAPER_RATIO = 0.85
MAX_SETBACK_TIERS = 8

OLLAMA_URL = "http://localhost:11434/api/generate"
MODEL_NAME = "qwen2.5:3b"
# The full extraction prompt (reasoning + 2-4 attributes + provenance +
# lean reasoning) routinely takes qwen2.5:3b 15-25s to generate on a
# laptop CPU/GPU - 25s was cutting it too close and caused silent,
# hard-to-diagnose fallbacks to the keyword parser's generic default.
REQUEST_TIMEOUT_SECONDS = 60

VALID_PROVENANCE = (
    "documented", "corroborated", "inferred", "ai_generated", "unknown",
)

_PROMPT_TEMPLATE = """You are extracting structured building parameters for a Minecraft \
reconstruction of: "{prompt}"

Return ONLY a single JSON object, no other text, matching exactly this shape:
{{
  "archetype": one of {archetypes},
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


def parse_prompt_llm(prompt: str) -> GenerationParams | None:
    request_prompt = _PROMPT_TEMPLATE.format(
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
    )

    payload = json.dumps({
        "model": MODEL_NAME,
        "prompt": request_prompt,
        "format": "json",
        "stream": False,
    }).encode("utf-8")

    try:
        request = urllib.request.Request(
            OLLAMA_URL, data=payload, headers={"Content-Type": "application/json"}
        )
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
            body = json.loads(response.read())
    except (urllib.error.URLError, TimeoutError, OSError, json.JSONDecodeError):
        return None

    try:
        parsed = json.loads(body["response"])
    except (KeyError, json.JSONDecodeError, TypeError):
        return None

    return _to_generation_params(parsed, prompt)


def _to_generation_params(parsed: dict, prompt: str) -> GenerationParams | None:
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

    raw_attributes = parsed.get("attributes")
    attributes = _sanitize_attributes(raw_attributes)

    lean_angle, lean_direction = _sanitize_lean(
        parsed.get("lean_angle_degrees"), parsed.get("lean_direction"), archetype,
    )
    cross_section, taper_ratio, setback_tiers = _sanitize_shape(
        parsed.get("cross_section"),
        parsed.get("taper_ratio"),
        parsed.get("setback_tiers"),
        archetype,
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
    )


def _sanitize_shape(raw_cross_section, raw_taper, raw_setbacks, archetype: str) -> tuple[str, float, int]:
    # Shape params only mean anything for towers - same reasoning as lean.
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
    # Lean only means anything for towers - silently drop it otherwise
    # rather than trust a model that filled the field in regardless.
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
