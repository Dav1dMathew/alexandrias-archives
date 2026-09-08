package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.BlockPos;

// The class applies translation & rotation upon a 90-degree Y-axis to local coordinates on the Blueprint.
public final class BlueprintTransform {
  private BlueprintTransform() {
    // Utility class.
  }

  // Local Blueprint position is converted to world-state position.
  // Rotation values: 0 = 0 degrees, 1 = 90 degrees, 2 = 180 degrees, 3 = 270 degrees.
  public static BlockPos transform(BlockPos localPosition, BlockPos origin, int rotation) {
    int x = localPosition.getX();
    int y = localPosition.getY();
    int z = localPosition.getZ();
    int Xrotation;
    int Zrotation;
    switch(Math.floorMod(rotation, 4)) {
      case 1 -> {
        Xrotation = -z;
        Zrotation = x;
      }
      case 2 -> {
        Xrotation = -x;
        Zrotation = -z;
      }
      case 3 -> {
        // 270 degrees. This case previously duplicated the identity
        // branch, so a quarter of all rotations silently did nothing.
        Xrotation = z;
        Zrotation = -x;
      }
      default -> {
        Xrotation = x;
        Zrotation = z;
      }
    }

    return origin.offset(Xrotation, y, Zrotation);
  }
}
