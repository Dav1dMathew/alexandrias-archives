# Blueprint contract

`blueprint.schema.json` is the canonical wire contract shared by the Minecraft
mod, the orchestration backend, and the AI service. `schemaVersion` is fixed at
`1`; a breaking field or semantic change requires a new schema version.

## Coordinate rules

- Coordinates are integers relative to the Blueprint origin.
- `x` must be less than `width`, `y` less than `height`, and `z` less than
  `depth`. JSON Schema cannot express these cross-field comparisons, so every
  producer and consumer must enforce them at runtime.
- A coordinate may appear at most once. Producers must not rely on block order
  to overwrite an earlier block.
- `blockId` is a namespaced Minecraft registry ID such as `minecraft:stone`.
  The Fabric mod remains responsible for checking that the ID exists in its
  active block registry.

## Compatibility rules

- Consumers must reject unsupported `schemaVersion` values.
- Producers must not add undeclared fields to a v1 payload.
- New optional fields may only be added after all three services can tolerate
  them. Otherwise, create Blueprint v2.
- The shared fixture in `fixtures/valid/minimal-blueprint-v1.json` is used by
  both Java and Python tests to detect contract drift.
