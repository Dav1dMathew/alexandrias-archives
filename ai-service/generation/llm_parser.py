"""LLM-backed prompt parser: local, free, runs against Ollama.

Slower and less reliable than a hosted model (see groq_parser.py), but
needs no API key/account/billing - the "always available" fallback tier
between Groq and the plain keyword parser.

Never raises out to the caller: any failure (Ollama not running, timeout,
malformed/invalid JSON) returns None, which signals main.py to fall back
further down the chain.
"""

import json
import urllib.request
import urllib.error

from .parser import GenerationParams
from .llm_schema import build_extraction_prompt, parse_llm_json

OLLAMA_URL = "http://localhost:11434/api/generate"
MODEL_NAME = "qwen2.5:3b"
# The full extraction prompt (reasoning + 2-4 attributes + provenance +
# lean reasoning) routinely takes qwen2.5:3b 15-25s to generate on a
# laptop CPU/GPU - 25s was cutting it too close and caused silent,
# hard-to-diagnose fallbacks to the keyword parser's generic default.
REQUEST_TIMEOUT_SECONDS = 60


def parse_prompt_llm(prompt: str) -> GenerationParams | None:
    request_prompt = build_extraction_prompt(prompt)

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

    return parse_llm_json(parsed, prompt)
