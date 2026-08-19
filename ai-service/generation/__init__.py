from .parser import GenerationParams, parse_prompt
from .llm_parser import parse_prompt_llm as _parse_prompt_ollama
from .groq_parser import parse_prompt_groq
from .archetypes import generate_blueprint


def parse_prompt_llm(prompt: str) -> GenerationParams | None:
    """Best available LLM backend, in order: Groq (fast, larger model,
    needs GROQ_API_KEY) -> local Ollama (free, no key, slower/smaller) ->
    None (signals main.py to fall back to the keyword parser). Neither
    backend raises, so this never raises either."""
    result = parse_prompt_groq(prompt)
    if result is not None:
        return result
    return _parse_prompt_ollama(prompt)


__all__ = [
    "GenerationParams", "parse_prompt", "parse_prompt_llm", "generate_blueprint",
]
