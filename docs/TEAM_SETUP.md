# Team Setup Guide

Everything needed to run Alexandria's Archives locally: the Minecraft mod, the AI service, and a free LLM key. Written after actually getting all three running from scratch tonight — every command here has been run for real, not guessed.

There are three pieces, and you need all three running at once:

1. **Minecraft + the mod** (Java/Fabric)
2. **The AI service** (Python, runs on your machine at `localhost:8000`)
3. **A Groq API key** (free — this is what makes `/aa build <prompt>` actually understand what you typed)

---

## 1. Prerequisites

- **Java 21** (`java -version` should show 21.x)
- **Python 3.11+**
- **Git**
- A Minecraft Java Edition account, with **Fabric Loader** installed for **Minecraft 1.21**, loader version **0.19.3** (or newer 0.19.x) — install via [fabricmc.net/use/installer](https://fabricmc.net/use/installer/)
- **Fabric API** `0.102.0+1.21` — download from [Modrinth](https://modrinth.com/mod/fabric-api/version/0.102.0+1.21) and drop it in your `mods` folder (see step 3)

---

## 2. Clone and check out the branch

```bash
git clone https://github.com/Dav1dMathew/alexandrias-archives.git
cd alexandrias-archives
git checkout "akshat'simplementation"
```

(The apostrophe in the branch name is real — quote it in your shell.)

---

## 3. Build and install the mod

```bash
./gradlew build
```

First build downloads Minecraft/mappings/Loom and takes a few minutes; after that it's fast (~10-15s).

Copy the built jar into your mods folder, alongside Fabric API:

```bash
# macOS
cp build/libs/alexandrias-archives-1.0.0.jar ~/Library/Application\ Support/minecraft/mods/

# Windows (PowerShell)
copy build\libs\alexandrias-archives-1.0.0.jar $env:APPDATA\.minecraft\mods\

# Linux
cp build/libs/alexandrias-archives-1.0.0.jar ~/.minecraft/mods/
```

Your `mods` folder should now have **two** jars: `alexandrias-archives-1.0.0.jar` and the Fabric API jar from step 1.

**Every time someone pushes a change to the Java code**, you'll need to `git pull`, `./gradlew build` again, re-copy the jar, and fully restart Minecraft (the mod only loads at JVM startup — a running client won't pick up a new jar).

---

## 4. Get a free Groq API key

This is what powers the actual "AI" part — turning a prompt into a real building shape with real facts, instead of falling back to plain keyword guessing.

1. Go to **[console.groq.com](https://console.groq.com)** and sign up (free, no credit card).
2. Create an API key.
3. **Do not share this key or commit it anywhere.** Each teammate should use their own.

---

## 5. Set up the AI service

```bash
cd ai-service
python3 -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install -r requirements.txt
```

Create a file `ai-service/.env` (this file is gitignored — it will never get committed, and that's intentional) with your own key from step 4:

```
GROQ_API_KEY=your_key_here
```

Start the service:

```bash
uvicorn main:app --port 8000
```

Leave this running in its own terminal. Confirm it's up:

```bash
curl http://localhost:8000/health
# should print: {"status":"ok"}
```

**If you skip the Groq key entirely**, the service still works — it falls back to a local model via Ollama if you have one running (see step 6, optional), and finally to plain keyword matching if neither is available. `/aa build a small house` will always work; `/aa build the taj mahal` will look much better with a real key.

---

## 6. (Optional) Local fallback model via Ollama

Only needed if you want things to keep working when Groq is unreachable, or you'd rather not use a hosted key at all.

1. Install [Ollama](https://ollama.com/download).
2. Pull the model the service expects:
   ```bash
   ollama pull qwen2.5:3b
   ```
3. Make sure the Ollama app/service is running (`ollama serve`, or just open the app) before you start the AI service.

This is noticeably slower (10-45s per request) and less accurate than Groq, but needs no account or key.

---

## 7. Run it

With the AI service running (step 5) and Minecraft launched on the Fabric profile with the mod installed (step 3):

In-game, either type:
```
/aa build the leaning tower of pisa
```
or press **`G`** to open the build GUI instead of typing the command by hand.

You should see a boss bar at the top tracking progress (contacting AI → validating → clearing → placing), then a real structure appear.

---

## Troubleshooting

- **"Failed to generate structure" / times out** — check the AI service terminal is still running and `curl http://localhost:8000/health` responds. If it crashed, restart it.
- **Command not recognized (`/aa` does nothing)** — the mod isn't loaded. Check both jars are in `mods`, check the Minecraft version/Fabric Loader version match, and check the game's log for a crash on startup.
- **Everything defaults to a generic house/box shape** — your Groq key probably isn't loading. Check `ai-service/.env` exists, has no typos, and that you restarted `uvicorn` after creating/editing it (env vars load once at startup).
- **Java changes don't show up in-game** — you need to rebuild (`./gradlew build`), re-copy the jar, and **fully quit and relaunch** Minecraft. Python/AI-service changes don't need this — just restart `uvicorn`.
