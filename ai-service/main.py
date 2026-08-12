from fastapi import FastAPI, UploadFile, File
from pydantic import BaseModel

from generation import parse_prompt, generate_blueprint

app = FastAPI(title="Alexandria's Archives - AI Service")


class BlueprintBlock(BaseModel):
    x: int
    y: int
    z: int
    blockId: str


class Blueprint(BaseModel):
    id: str
    name: str
    width: int
    height: int
    depth: int
    blocks: list[BlueprintBlock]


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
    # Tier 0: keyword-parsed prompt -> procedural blueprint generator.
    # parse_prompt() stands in for an LLM call - same input/output shape,
    # so swapping in a real LLM later doesn't change this endpoint.
    params = parse_prompt(request.prompt)
    return generate_blueprint(params)
