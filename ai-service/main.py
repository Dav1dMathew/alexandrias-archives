import asyncio

from dotenv import load_dotenv

load_dotenv()  # loads ai-service/.env (gitignored) - e.g. GROQ_API_KEY

from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel

from generation import parse_prompt, parse_prompt_llm, generate_blueprint
from generation.parser import match_archetype, DEFAULT_ARCHETYPE
from generation.osm_lookup import lookup_real_building
from generation.footprint_builder import build_from_footprint
from generation.image_source import fetch_reference_image_bytes
from generation.mosaic_builder import build_mosaic
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


def _mosaic_response(prompt: str, image_bytes: bytes) -> dict | None:
    mosaic = build_mosaic(image_bytes)
    if mosaic is None:
        return None
    blocks, width, height, depth = mosaic
    display_name = prompt.strip()
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


def _osm_response(prompt: str, osm_building: dict, material: str) -> dict | None:
    try:
        blocks, width, height, depth = build_from_footprint(
            osm_building["points_latlon"], osm_building["height_meters"], material,
        )
    except ValueError:
        return None  # footprint collapsed after scaling

    display_name = prompt.strip() or osm_building["name"]
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


@app.post("/generate", response_model=Blueprint)
async def generate(request: GenerateRequest):
    # Hybrid: the keyword parser is a small, fixed vocabulary but is
    # deterministic and reliable when it actually matches. The LLM
    # (Groq if configured, else local Ollama) picks from a much richer
    # set of real 3D shapes now - house/tower/temple/wheel/dome, plus
    # geometric parameters (lean, taper, cross-section, freestanding
    # accessory towers) - so a landmark it can actually recognize (the
    # Taj Mahal -> dome + 4 minarets, a Ferris wheel -> wheel, the Burj
    # Khalifa -> tapering tower) gets real, walkable 3D geometry, not a
    # flat photo. parse_prompt_llm() never raises - None means both Groq
    # and Ollama failed/aren't configured.
    keyword_matched = match_archetype(request.prompt) is not None
    keyword_params = parse_prompt(request.prompt)
    llm_params = await asyncio.to_thread(parse_prompt_llm, request.prompt)

    if llm_params is None:
        params = keyword_params
    elif keyword_matched:
        # Keyword parser found a confident match - trust its base shape,
        # but keep the LLM's honest reasoning/provenance AND any real
        # geometric parameters it found (lean, taper, accessories, ...) -
        # the keyword parser has no concept of those at all.
        params = keyword_params
        params.reasoning = llm_params.reasoning
        params.attributes = llm_params.attributes
        params.lean_angle_degrees = llm_params.lean_angle_degrees
        params.lean_direction = llm_params.lean_direction
        params.cross_section = llm_params.cross_section
        params.taper_ratio = llm_params.taper_ratio
        params.setback_tiers = llm_params.setback_tiers
        params.accessory_count = llm_params.accessory_count
    else:
        params = llm_params

    # If this names a specific real subject (no keyword match) but even
    # the LLM could only muster the bare default archetype for it, that's
    # a weak result - the model doesn't know this well enough to pick a
    # real shape. Rescue with real data instead of shipping a generic
    # guess: try a real photo mosaic, then OSM's real footprint, before
    # falling back to the (unconvincing) archetype guess after all.
    needs_rescue = not keyword_matched and params.archetype == DEFAULT_ARCHETYPE
    if needs_rescue:
        image_bytes = await asyncio.to_thread(fetch_reference_image_bytes, request.prompt)
        if image_bytes is not None:
            response = _mosaic_response(request.prompt, image_bytes)
            if response is not None:
                return response

        osm_building = await asyncio.to_thread(lookup_real_building, request.prompt)
        if osm_building is not None:
            response = _osm_response(request.prompt, osm_building, params.wall_material)
            if response is not None:
                return response

    return generate_blueprint(params)
