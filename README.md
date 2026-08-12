<div align="center">

<img src="src/main/resources/assets/alexandrias-archives/icon.png" width="96" alt="Alexandria's Archives icon">

# Alexandria's Archives

**AI-reconstructed history, rebuilt block by block in Minecraft.**

[![Build](https://github.com/Dav1dMathew/alexandrias-archives/actions/workflows/build.yml/badge.svg)](https://github.com/Dav1dMathew/alexandrias-archives/actions/workflows/build.yml)
[![License: CC0](https://img.shields.io/badge/license-CC0--1.0-lightgrey.svg)](LICENSE)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21-3f6b8a)
![Java](https://img.shields.io/badge/Java-21-3f6b8a)
![Fabric](https://img.shields.io/badge/Fabric-Loom-3f6b8a)

</div>

---

## What is this?

**Alexandria's Archives** (project codename **AEGIS**) turns a text prompt, an image, or a real blueprint of a historical or real-world structure into a **navigable, walkable 3D reconstruction inside Minecraft**.

Give it a building. An AI service infers its geometry, a backend orchestrates the pipeline, and a Fabric mod builds the result directly into the Minecraft world — brick by brick — so you can walk through history instead of just reading about it.

Planned features on top of the core reconstruction:

- 🏛️ **Three scale tiers** — Mini, life-sized, and "megalophobia-esque" oversized builds
- 🕰️ **Timeline generation** — auto-generated growth and economic development across historical periods
- 🧭 **Evidence vs. inference labelling** — see clearly what's historically documented vs. AI-guessed
- 👻 **Historical ghosts** — villagers standing in for the people who once lived there
- 📸 **Photo mode** — for capturing and sharing reconstructions
- 🥽 **Voice / prompt input**, with VR-style interaction as a stretch goal

## How it works

<p align="center">
  <img src="docs/architecture-diagram.svg" width="820" alt="Diagram of the Alexandria's Archives pipeline: a player prompt flows from the Fabric mod, to the Spring Boot backend, to the FastAPI AI service, and back as a Blueprint that gets rendered into the Minecraft world.">
</p>

Three services speak a shared JSON **Blueprint** contract:

| Stage | Service | Role |
|---|---|---|
| 1 | **Fabric Mod** (Java, this repo's `src/`) | Reads the player's `/aa build` prompt, sends the request, validates and places the returned structure in the world tick-by-tick |
| 2 | **Spring Boot backend** (`:8080`) | Orchestrates the request — receives it from the mod and forwards it to the AI service |
| 3 | **FastAPI AI service** (`:8000`, this repo's [`ai-service/`](ai-service)) | Analyzes the prompt/image and returns a structured `Blueprint` (dimensions + block list) |

The mod then validates the blueprint (`BlueprintValidator`) against sane size/block-count limits, and places it into the world in rate-limited batches (`BuildJob`) so large AI-generated builds don't freeze the server.

### Reconstruction tiers

The AI service builds up reconstruction fidelity in tiers rather than attempting full photo-to-3D reconstruction in one leap:

- **Tier 0 — Prompt → procedural generation (`POST /generate`, implemented)** — a keyword-based parser reads the prompt for a building archetype (house / tower / temple), a scale tier (mini / medium / huge), and a material, then a procedural generator produces the `Blueprint`. It's a stand-in for an LLM-based parser: same input/output shape, so swapping in a real LLM later is a drop-in change.
- **Tier 1 — Photo → blueprint (`POST /analyze`, stubbed)** — will run photogrammetry on real building photos and voxelize the resulting mesh into blocks. Currently returns a fixed mock structure.
- **Tier 2 — Site grounding (planned)** — ground scale/orientation using real building footprint + height data (OSM / LIDAR).

## Repository layout

```
alexandrias-archives/
├── src/main/java/.../alexandriasarchives/
│   ├── commands/       # /aa, /aa build, /aa image, /aa history, /aa photo
│   ├── builder/         # BlueprintValidator, StructureBuilder, BuildJob (tick-batched placement)
│   └── model/            # Blueprint, BlueprintBlock — the shared structure format
├── ai-service/          # Python FastAPI reconstruction service
│   ├── main.py           # GET /health, POST /generate (Tier 0), POST /analyze (Tier 1 stub)
│   ├── generation/        # prompt parsing + procedural blueprint generators
│   └── requirements.txt
└── docs/
    └── architecture-diagram.svg
```

## Getting started

### Minecraft mod

See the [Fabric documentation](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) for IDE setup, then:

```bash
./gradlew runClient
```

Once in a world, `/aa build demo` places a small hardcoded structure so you can confirm the pipeline works end-to-end.

### AI service

```bash
cd ai-service
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
```

Visit `http://localhost:8000/docs` for the interactive Swagger UI. Try `POST /generate` with `{"prompt": "a massive stone lighthouse"}` — different archetypes, scales, and materials produce genuinely different structures. `/analyze` (photo upload) still returns a fixed mock `Blueprint` until Tier 1 lands.

## Status

This is an early-stage, in-progress project. The mod can validate and place a hardcoded demo structure. The AI service's Tier 0 prompt-to-blueprint generation is real and working (`/generate`), producing distinct houses, towers, and temples from free-text prompts — but the mod and the AI/backend services aren't wired together yet, `/analyze` (photo input) is still a mock, and the real photogrammetry-based reconstruction doesn't exist. See the project's weekly goals for the current roadmap.

## Team

| Role | Author |
|---|---|
| Minecraft Integration | David Mathew |
| Spring Boot API | Maayan Hazra |
| Python AI Engineer | Jyotishk Ajmera |
| Python AI Programmer | Akshat Barjatya |

## License

This project builds on the Fabric example mod template, available under [CC0](LICENSE). Feel free to learn from it and incorporate it into your own projects.
