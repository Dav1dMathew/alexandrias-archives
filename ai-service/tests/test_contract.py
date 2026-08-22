import asyncio
import json
import sys
from pathlib import Path

import httpx
import jsonschema
import pytest
from pydantic import ValidationError


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
AI_SERVICE_ROOT = REPOSITORY_ROOT / "ai-service"
sys.path.insert(0, str(AI_SERVICE_ROOT))

from main import Blueprint, app, build_mock_blueprint  # noqa: E402


SCHEMA_PATH = REPOSITORY_ROOT / "contracts" / "blueprint.schema.json"
FIXTURE_PATH = (
    REPOSITORY_ROOT
    / "contracts"
    / "fixtures"
    / "valid"
    / "minimal-blueprint-v1.json"
)


def load_json(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def test_shared_fixture_matches_schema_and_python_model() -> None:
    schema = load_json(SCHEMA_PATH)
    fixture = load_json(FIXTURE_PATH)

    jsonschema.Draft202012Validator(schema).validate(fixture)
    model = Blueprint.model_validate(fixture)

    assert model.model_dump(mode="json", exclude_none=True) == fixture


def test_mock_blueprint_matches_shared_schema() -> None:
    schema = load_json(SCHEMA_PATH)
    payload = build_mock_blueprint().model_dump(mode="json")

    jsonschema.Draft202012Validator(schema).validate(payload)


def test_analyze_endpoint_emits_blueprint_v1() -> None:
    schema = load_json(SCHEMA_PATH)

    async def request_blueprint() -> httpx.Response:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(
            transport=transport,
            base_url="http://testserver",
        ) as client:
            return await client.post(
                "/analyze",
                files={"file": ("reference.png", b"mock image", "image/png")},
            )

    response = asyncio.run(request_blueprint())

    assert response.status_code == 200
    jsonschema.Draft202012Validator(schema).validate(response.json())
    assert response.json()["schemaVersion"] == 1


def test_generate_endpoint_emits_blueprint_v1(monkeypatch: pytest.MonkeyPatch) -> None:
    schema = load_json(SCHEMA_PATH)
    monkeypatch.setattr("main.parse_prompt_llm", lambda prompt: None)

    async def request_blueprint() -> httpx.Response:
        transport = httpx.ASGITransport(app=app)
        async with httpx.AsyncClient(
            transport=transport,
            base_url="http://testserver",
        ) as client:
            return await client.post(
                "/generate",
                json={"prompt": "a small stone house"},
            )

    response = asyncio.run(request_blueprint())

    assert response.status_code == 200
    jsonschema.Draft202012Validator(schema).validate(response.json())
    assert response.json()["schemaVersion"] == 1


def test_blueprint_rejects_duplicate_coordinates() -> None:
    fixture = load_json(FIXTURE_PATH)
    fixture["blocks"].append(dict(fixture["blocks"][0]))

    with pytest.raises(ValidationError, match="Duplicate block position"):
        Blueprint.model_validate(fixture)


def test_blueprint_rejects_coordinates_outside_declared_dimensions() -> None:
    fixture = load_json(FIXTURE_PATH)
    fixture["blocks"][0]["x"] = fixture["width"]

    with pytest.raises(ValidationError, match="outside the declared Blueprint dimensions"):
        Blueprint.model_validate(fixture)


def test_blueprint_rejects_missing_version_and_unknown_fields() -> None:
    fixture = load_json(FIXTURE_PATH)
    fixture.pop("schemaVersion")
    fixture["unexpected"] = True

    with pytest.raises(ValidationError) as exception:
        Blueprint.model_validate(fixture)

    error_types = {error["type"] for error in exception.value.errors()}
    assert error_types == {"missing", "extra_forbidden"}
