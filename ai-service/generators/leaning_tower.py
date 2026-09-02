import math
from models.blueprint import(Blueprint, BlueprintBlock, Position)

def generate_leaning_twoer() -> Blueprint:
  blocks = []
  width = 24
  depth = 24
  height = 40
  xCenter = width // 2
  zCenter = depth // 2
  radius = 7
  
  # Foundation
  for y in range(2):
    for x in range(-radius - 1, radius + 2):
      for z in range(-radius - 1, radius + 2):
        distance = math.sqrt(x*x + z*z)
        if distance <= radius + 0.5:
          blocks.append(BlueprintBlock(position=Position(x=xCenter + x, y, z=zCenter + z,), blockID="minecraft:stone_bricks",))

  # Tower
  for y in range(2, 32):
    lean_offset = y // 8
    xCenter_tower = (xCenter + lean_offset)
    zCenter_tower = zCenter
    if y < 8:
      current_radius = 7
    elif y < 16:
      current_radius = 6
    elif y < 24:
      current_radius = 5
    else:
      current_radius = 4
    inner_radius = max(1, current_radius - 2)
    for x in range(-current_radius, current_radius + 1):
      for z in range(-current_radius, current_radius + 1):
        distance = math.sqrt(x*x + z*z)
        if(distance <= current_radius and distance >= inner_radius):
          block_ID = choose_stone(x, y, z)
          blocks.append(BlueprintBlock(position=Position(x=xCenter_tower+x, y, z=zCenter_tower + z,), blockID = block_ID,))

  # Bell Chamber
  xCenter_bell = xCenter + 4
  for y in range(32, 37):
    bell_radius = 4
    for x in range(-bell_radius, bell_radius + 1):
      for z in range(-bell_radius, bell_radius + 1):
        distance = math.sqrt(x*x + z*z)
        if(distance <= bell_radius and distance >= bell_radius - 1):
          blocks.append(BlueprintBlock(position=Position(x=xCenter_bell + x, y, z=zCenter_bell + z,), blockID="minecraft:smooth_quartz",))

  return Blueprint(id="leaning_tower_of_pisa", name="Leaning Tower of Pisa", width=width, height=height, depth=depth, blocks=blocks,)
  def choose_stone(x: int, y: int, z: int,) -> str:
    value = abs(x*31 + y*17 + z*13)
    variation = value % 5
    if variation == 1:
      return "minecraft:calcite"
    if variation == 2:
      return "minecraft:quartz_block"
    return "minecraft:smooth_quartz"
