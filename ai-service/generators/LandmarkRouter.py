from generators.leaning_tower import(generate_leaning_tower,)

def generate_from_prompt(prompt: str):
  normalized = (prompt.lower().strip())
  if("leaning tower" in normalized or "tower of pisa" in normalized or "pisa tower" in normalized):
    return generate_leaning_tower()
  return None
