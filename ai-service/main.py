import asyncio
from typing import Literal

from dotenv import load_dotenv

load_dotenv()  # loads ai-service/.env (gitignored) - e.g. GROQ_API_KEY

from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel, ConfigDict, Field, model_validator


BLUEPRINT_SCHEMA_VERSION = 1
MAX_BLUEPRINT_DIMENSION = 64
MAX_BLUEPRINT_BLOCKS = 50_000
MINECRAFT_BLOCK_ID_PATTERN = r"^[a-z0-9_.-]+:[a-z0-9_./-]+$"

from generation import parse_prompt, parse_prompt_llm, generate_blueprint
from generation.parser import match_archetype
from generation.osm_lookup import lookup_real_building
from generation.footprint_builder import build_from_footprint
from generation.image_source import fetch_reference_image_bytes
from generation.mosaic_builder import build_mosaic
from generation.archetypes import _slugify

app = FastAPI(title="Alexandria's Archives - AI Service")


class BlueprintBlock(BaseModel):
    model_config = ConfigDict(extra="forbid")

    x: int = Field(ge=0, lt=MAX_BLUEPRINT_DIMENSION)
    y: int = Field(ge=0, lt=MAX_BLUEPRINT_DIMENSION)
    z: int = Field(ge=0, lt=MAX_BLUEPRINT_DIMENSION)
    blockId: str = Field(pattern=MINECRAFT_BLOCK_ID_PATTERN)


class BlueprintAttribute(BaseModel):
    model_config = ConfigDict(extra="forbid")

    field: str = Field(min_length=1, max_length=128, pattern=r"\S")
    value: str = Field(min_length=1, max_length=1_000, pattern=r"\S")
    provenance: Literal["documented", "inferred", "ai_generated", "unknown"]


class Blueprint(BaseModel):
    model_config = ConfigDict(extra="forbid")

    schemaVersion: Literal[1]
    id: str = Field(min_length=1, max_length=128, pattern=r"\S")
    name: str = Field(min_length=1, max_length=256, pattern=r"\S")
    width: int = Field(ge=1, le=MAX_BLUEPRINT_DIMENSION)
    height: int = Field(ge=1, le=MAX_BLUEPRINT_DIMENSION)
    depth: int = Field(ge=1, le=MAX_BLUEPRINT_DIMENSION)
    blocks: list[BlueprintBlock] = Field(max_length=MAX_BLUEPRINT_BLOCKS)
    reasoning: str | None = Field(default=None, max_length=10_000)
    attributes: list[BlueprintAttribute] | None = Field(default=None, max_length=256)

    @model_validator(mode="after")
    def validate_block_positions(self) -> "Blueprint":
        occupied_positions: set[tuple[int, int, int]] = set()

        for block in self.blocks:
            if block.x >= self.width or block.y >= self.height or block.z >= self.depth:
                raise ValueError(
                    f"Block position ({block.x}, {block.y}, {block.z}) is outside "
                    "the declared Blueprint dimensions."
                )

            position = (block.x, block.y, block.z)
            if position in occupied_positions:
                raise ValueError(f"Duplicate block position: {position}.")
            occupied_positions.add(position)

        return self


class GenerateRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    prompt: str = Field(min_length=1, max_length=1_000, pattern=r"\S")


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
        schemaVersion=BLUEPRINT_SCHEMA_VERSION,
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
    # Real-world lookups (Wikipedia photo mosaic, OSM footprint) only
    # make sense when the prompt actually names a specific real subject.
    # A confident keyword match (e.g. "a small wooden house" matching
    # "house") means it's a generic description, not a named place - and
    # both Wikipedia's and Nominatim's fuzzy search will confidently
    # return *something* for almost any query, so without this gate a
    # totally generic prompt could get hijacked into a random article's
    # photo or a random building's footprint instead of the reliable
    # archetype system. Named-subject prompts (no keyword match) try
    # mosaic -> OSM -> archetype/LLM in that order below.
    keyword_matched = match_archetype(request.prompt) is not None

    # First: a real photo, reproduced as an extruded block-color mosaic.
    # Tried before OSM on purpose - a photo captures color, domes,
    # ornamentation and anything else that isn't "footprint extruded
    # straight up," which is all OSM's plain extrusion can ever produce.
    # For an ornate/domed building (e.g. the Taj Mahal) tracing its real
    # footprint with a single material just gives a flat, colorless,
    # oddly-angled outline - genuinely worse than a real photo's
    # silhouette and colors, even though OSM's geometry is "more real."
    image_bytes = None if keyword_matched else await asyncio.to_thread(
        fetch_reference_image_bytes, request.prompt
    )
    if image_bytes is not None:
        mosaic = build_mosaic(image_bytes)
        if mosaic is not None:
            blocks, width, height, depth = mosaic
            display_name = request.prompt.strip()
            return {
                "schemaVersion": BLUEPRINT_SCHEMA_VERSION,
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
                    "Reproduced as an extruded block-color mosaic from a real "
                    "reference photo (Wikipedia) - matches the photo's actual "
                    "silhouette and colors, not a 3D template guess."
                ),
                "attributes": [
                    {
                        "field": "source",
                        "value": "real photo, Wikipedia main image for this page",
                        "provenance": "documented",
                    },
                    {
                        "field": "form",
                        "value": "solid extruded mosaic relief, not a hollow walkable building",
                        "provenance": "documented",
                    },
                ],
            }

    # Second: OSM's real traced footprint - used when there's no good
    # photo (Wikipedia has no image for this subject), for buildings
    # boxy enough that a straight extrusion actually looks right.
    osm_building = None if keyword_matched else await asyncio.to_thread(
        lookup_real_building, request.prompt
    )
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
                "schemaVersion": BLUEPRINT_SCHEMA_VERSION,
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

    blueprint = generate_blueprint(params)
    blueprint["schemaVersion"] = BLUEPRINT_SCHEMA_VERSION
    return blueprint
