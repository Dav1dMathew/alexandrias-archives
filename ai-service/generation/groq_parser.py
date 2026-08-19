"""LLM-backed prompt parser: hosted, via Groq's API.

Needs GROQ_API_KEY set (loaded from ai-service/.env - see main.py). Groq
serves much larger models (70B vs. the local 3B) on purpose-built
low-latency hardware, so this is both faster and meaningfully more
accurate than the local Ollama fallback at the same extraction task -
tries this first when a key is configured.

Never raises out to the caller: no key, an HTTP/network failure, a
timeout, or malformed JSON all return None, signalling main.py to fall
back to the local Ollama model, then the keyword parser. A missing or
invalid Groq key must never break /generate.
"""

import json
import os
import urllib.request
import urllib.error

from .parser import GenerationParams
from .llm_schema import build_extraction_prompt, parse_llm_json

GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
MODEL_NAME = "openai/gpt-oss-120b"
REQUEST_TIMEOUT_SECONDS = 30
# Groq sits behind Cloudflare, which blocks requests with no/default
# User-Agent as bot traffic (HTTP 403, Cloudflare error 1010) - not a
# Groq-specific requirement, just needs to look like a real client.
USER_AGENT = "AlexandriasArchives/0.1 (+https://github.com/Dav1dMathew/alexandrias-archives)"


def parse_prompt_groq(prompt: str) -> GenerationParams | None:
    api_key = os.environ.get("GROQ_API_KEY")
    if not api_key:
        return None

    request_prompt = build_extraction_prompt(prompt)

    payload = json.dumps({
        "model": MODEL_NAME,
        "messages": [{"role": "user", "content": request_prompt}],
        "response_format": {"type": "json_object"},
        "temperature": 0.3,
    }).encode("utf-8")

    try:
        request = urllib.request.Request(
            GROQ_URL,
            data=payload,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {api_key}",
                "User-Agent": USER_AGENT,
            },
        )
        with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT_SECONDS) as response:
            body = json.loads(response.read())
    except (urllib.error.URLError, TimeoutError, OSError, json.JSONDecodeError):
        # Covers urllib.error.HTTPError too (a URLError subclass) - a bad/
        # expired key, rate limit, or any other HTTP failure all just
        # degrade to the next fallback rather than break /generate.
        return None

    try:
        content = body["choices"][0]["message"]["content"]
        parsed = json.loads(content)
    except (KeyError, IndexError, TypeError, json.JSONDecodeError):
        return None

    return parse_llm_json(parsed, prompt)
