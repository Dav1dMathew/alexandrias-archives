from pydantic import BaseModel, Field

class Position(BaseModel):
  x: int
  y: int
  z: int

class BlueprintBlock(BaseModel):
  position: Position
  blockID: str

class Blueprint(BaseModel):
  id: str
  name: str
  width: int = Field(gt=0)
  height: int = Field(gt=0)
  depth: int = Field(gt=0)
  blocks: list[BlueprintBlock]
