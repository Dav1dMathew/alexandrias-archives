from fastapi import FastAPI
from pydantic import BaseModel
from generators.LandmarkRouter import (generate_from_prompt)

app = FastAPI(title="Alexandria's Archives AI", version="1.0.0",)
class GenerateRequest(BaseModel):
  prompt: str

@app.get("/health")
def health():
  return{
    "status": "healthy"
  }

@app.post("/generate")
def generate(request: GenerateRequest):
  blueprint = generate_from_prompt(request.prompt)
  if blueprint is not None:
    return blueprint.model_dump()
  return {
    "error": ("Unsupported landmark for demo.")
  }
