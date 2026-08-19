import asyncio

from dotenv import load_dotenv

load_dotenv()  # loads ai-service/.env (gitignored) - e.g. GROQ_API_KEY

from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel

from generation import parse_prompt, parse_prompt_llm, generate_blueprint
from generation.parser import match_archetype
from generation.osm_lookup import lookup_real_building
from generation.footprint_builder import build_from_footprint
from generation.archetypes import _slugify

app = FastAPI(title="Alexandria's Archives - AI Service")


class BlueprintBlock(BaseModel):
    x: int
    y: int
    z: int
    blockId: str


class BlueprintAttribute(BaseModel):
    field: str
    value: str
    provenance: str


class Blueprint(BaseModel):
    id: str
    name: str
    width: int
    height: int
    depth: int
    blocks: list[BlueprintBlock]
    reasoning: str | None = None
    attributes: list[BlueprintAttribute] | None = None


class GenerateRequest(BaseModel):
    prompt: str


def build_mock_blueprint() -> Blueprint:
    blocks: list[BlueprintBlock] = []

    # Floor
    for x in range(7):
        for z in range(7):
            blocks.append(BlueprintBlock(x=x, y=0, z=z, blockId="minecraft:stone"))

    # Walls
    for y in range(1, 4):
        for x in range(7):
            blocks.append(BlueprintBlock(x=x, y=y, z=0, blockId="minecraft:stone_bricks"))
            blocks.append(BlueprintBlock(x=x, y=y, z=6, blockId="minecraft:stone_bricks"))
        for z in range(1, 6):
            blocks.append(BlueprintBlock(x=0, y=y, z=z, blockId="minecraft:stone_bricks"))
            blocks.append(BlueprintBlock(x=6, y=y, z=z, blockId="minecraft:stone_bricks"))

    # Roof
    for x in range(7):
        for z in range(7):
            blocks.append(BlueprintBlock(x=x, y=4, z=z, blockId="minecraft:oak_planks"))

    return Blueprint(
        id="mock_building_001",
        name="Mock Analyzed Structure",
        width=7,
        height=5,
        depth=7,
        blocks=blocks,
    )


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/analyze", response_model=Blueprint)
async def analyze(file: UploadFile = File(...)):
    # Placeholder for the Tier 1 (photo -> blueprint) pipeline. For now
    # this ignores the uploaded file's contents and returns a fixed mock
    # blueprint so downstream services can be built against a stable
    # contract.
    return build_mock_blueprint()


@app.post("/generate", response_model=Blueprint)
async def generate(request: GenerateRequest):
    # Tries real geometry first: if the prompt names an actual mapped
    # building, OpenStreetMap (free, no API key) has its real traced
    # footprint and often a real height - tracing that outline is
    # genuinely accurate, not a guess between 3 templates. Falls through
    # to the archetype system below if there's no OSM match, no traced
    # footprint (a point-only result), or the lookup times out/fails -
    # lookup_real_building() never raises.
    osm_building = await asyncio.to_thread(lookup_real_building, request.prompt)
    if osm_building is not None:
        material = parse_prompt(request.prompt).wall_material
        try:
            blocks, width, height, depth = build_from_footprint(
                osm_building["points_latlon"], osm_building["height_meters"], material,
            )
        except ValueError:
            blocks = None  # footprint collapsed after scaling - fall through

        if blocks:
            display_name = request.prompt.strip() or osm_building["name"]
            return {
                "id": _slugify(display_name),
                "name": display_name,
                "width": width,
                "height": height,
                "depth": depth,
                "blocks": [
                    {"x": x, "y": y, "z": z, "blockId": block_id}
                    for x, y, z, block_id in blocks
                ],
                "reasoning": (
                    f"Traced from {osm_building['name']}'s real footprint on "
                    "OpenStreetMap, extruded to its real/estimated height - "
                    "not a template guess."
                ),
                "attributes": [
                    {
                        "field": "height",
                        "value": f"{osm_building['height_meters']:.0f} m",
                        "provenance": "documented",
                    },
                    {
                        "field": "footprint",
                        "value": "real traced outline from OpenStreetMap",
                        "provenance": "documented",
                    },
                ],
            }

    # Hybrid: the keyword parser is a small, fixed vocabulary but is
    # deterministic and reliable when it actually matches. The local LLM
    # (via Ollama - free, no API key) is not consistently better at
    # archetype classification - a 3B model misclassifies famous buildings
    # it should know - but it's genuinely good at the thing that actually
    # differentiates this project: honestly labeling how confident each
    # extracted fact is (documented/inferred/ai_generated/unknown) rather
    # than presenting a guess as fact.
    #
    # So: trust the keyword parser's archetype/scale/material whenever it
    # has a confident (non-default) match, and always keep the LLM's
    # reasoning/attributes for the provenance layer. parse_prompt_llm()
    # never raises - it returns None on any failure (Ollama not running,
    # timeout, malformed output) - so /generate always succeeds even
    # without a local model available.
    keyword_matched = match_archetype(request.prompt) is not None
    keyword_params = parse_prompt(request.prompt)
    llm_params = await asyncio.to_thread(parse_prompt_llm, request.prompt)

    if llm_params is None:
        params = keyword_params
    elif keyword_matched:
        # Keyword parser found a confident match - trust its base shape,
        # but keep the LLM's honest reasoning/provenance AND any real
        # geometric parameters it found (e.g. an actual lean angle) - the
        # keyword parser has no concept of those at all.
        params = keyword_params
        params.reasoning = llm_params.reasoning
        params.attributes = llm_params.attributes
        params.lean_angle_degrees = llm_params.lean_angle_degrees
        params.lean_direction = llm_params.lean_direction
        params.cross_section = llm_params.cross_section
        params.taper_ratio = llm_params.taper_ratio
        params.setback_tiers = llm_params.setback_tiers
    else:
        params = llm_params

    return generate_blueprint(params)
