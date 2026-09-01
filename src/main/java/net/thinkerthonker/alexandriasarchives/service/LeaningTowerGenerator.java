package net.thinkerthonker.alexandriasarchives.service;

import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import java.util.ArrayList;
import java.util.List;

// A 3D reconstruction of the Leaning Tower of Pisa is generated in Minecraft procedurally.
// **IMPORTANT NOTE FOR COLLABORATORS**
// As this is currently a demonstratio model, the landmark is implemented inside of the mod rather than it
// being an full architectural reconstruction through VR/AI Search Engine data reading.

public final class LeaningTowerGenerator {
  private LeaningTowerGenerator() {
    // Utility Class.
  }
  // Leaning Tower Blueprint is Generated.
  public static Blueprint generate() {
    List<BlueprintBlock> blocks = new ArrayList<>();
    
    // Dimensions.
    final int radius = 7;
    final int width = 24;
    final int height = 32;
    final int depth = 24;

    // Blueprint Center-Point.
    final int Xcenter = width / 2;
    final int Zcenter = depth / 2;

    // Builds out circular foundation.
    addFoundation(blocks, Xcenter, Zcenter, radius);
    
    // Main Tower -> The tower leans progressively towards positive X direction.
    for(int y = 2; y < height; y++) {
      
      // Progressive lean -> The tower shifts slightly outwards every few vertical layers.
      int leanOffset = y / 8;
      int XtowerCenter = Xcenter + leanOffset;
      int ZtowerCenter = Zcenter;

      // Taper's slight.
      int currentRadius;
      if(y < 8) {
        currentRadius = 7;
      }
      
      else if(y < 16) {
        currentRadius = 6;
      }
      
      else if(y < 24) {
        currentRadius = 5;
      }
      
      else {
        currentRadius = 4;
      }

      addTowerRing(blocks, XtowerCenter, ZtowerCenter, y, currentRadius);

      // In-between decorative floor.
      if(y % 4 == 0) {
        addFloorRing(blocks, XtowerCenter, ZtowerCenter, y, currentRadius + 1);
      }

      // Column Accents.
      if(y % 4 != 0) {
        addColumns(blocks, XtowerCenter, ZtowerCenter, y, currentRadius);
      }
    }

    // Bell Chamber.
    addBellChamber(blocks, Xcenter + 4, Zcenter, 32);

    // Top.
    addTop(blocks, Xcenter + 4, Zcenter, 37);

    // Max Height Calc.
    int blueprintHeight = 40;
    return new Blueprint("leaning_tower_of_pisa", "Leaning Tower Of Pisa", width, blueprintHeight, depth, blocks);
  }

  // Stone Foundation is generated.
  private static void addFoundation(List<BlueprintBlock> blokcs, int Xcenter, int Zcenter, int radius) {
    for(int y = 0; y < 2; y++) {
      for(int x = -radius - 1; x <= radius + 1; x++) {
        for(int z = -radius - 1; z <= radius + 1; z++) {
          double distance = Math.sqrt(x*x + z*z);
          if(distance <= radius + 0.5) {
            blocks.add(new BlueprintBlock(new BlockPos(Xcenter + x, y, Zcenter + z), "minecraft:stone_bricks"));
          }
        }
      }
    }
  }

  // Outer Cylindrical tower shell is generated.
  private static void addTowerRing(List<BlueprintBlock> blocks, int Xcenter, int Zcenter, int y, int radius) {
    int innerRadius = Math.max(1, radius - 2);
    for(int x = -radius; x <= radius; x++) {
      for(int z = -radius; z <= radius; z++) {
        double distance = Math.sqrt(x*x + z*z);

        // Hollow Ring is generated.
        if(distance <= radius && distance >= innerRadius) {
          String blockType = chooseStoneBlock(x, y, z);
          blocks.add(new BlueprintBlock(new BlockPos(Xcenter + x, y, Zcenter + z), blockType));
        }
      } 
    }
  }

  // Decorative Horizontal Ring Generated on top of Floor.
  private static void addFloorRing(List<BlueprintBlock> blocks, int Xcenter, int Zcenter, int y, int radius) {
    for(int x = -radius; x <= radius; x++) {
      for(int z = -radius; z <= radius; z++) {
        double distance = Math.sqrt(x*x + z*z);
        if(distance <= radius && distance >= radius - 1.5) {
          blocks.add(new BlueprintBlock(new BlockPos(Xcenter + x, y, Zcenter + z), "minecraft:smooth_quartz"));
        }
      }
    }
  }

  // Decorative Column features added around tower.
  private static void addColumns(List<BlueprintBlock> blocks, int Xcenter, int Zcenter, int y, int radius) {
    int[][] positions = {
      { radius, 0 }, { -radius, 0 }, { 0, radius }, { 0, -radius }, { radius - 1, radius - 1 }, { radius - 1, -(radius - 1) }, 
      { -(radius - 1), radius - 1 }, { -(radius - 1), -(radius - 1) }
    };
    for(int[] position : positions) {
      blocks.add(new BlueprintBlock(new BlockPos(Xcenter + position[0], y, Zcenter + position[1]), "minecraft:quartz_pillar"));
    }
  }

  // Upper-Bell Chamber is created.
  private static void addBellChamber(List<BlueprintBlock> blocks, int Xcenter, int Zcenter, int Ystart) {
    int radius = 4;
    for(int y = Ystart; y < Ystart + 5; y++) {
      for(int x = -radius; x <= radius; x++) {
        for(int z = -radius; z <= radius; z++) {
          double distance = Math.sqrt(x*x + z*z);

          // Chamber is kept open between two supporting pillars.
          if(distance <= radius && distance >= radius - 1) {
            blocks.add(new BlueprintBlock(new BlockPos(Xcenter + x, y, Zcenter + z), "minecraft:smooth_quartz"));
          }
        }
      }

      // Pillars.
      addColumns(blocks, Xcenter, Zcenter, y, radius);
    }
  }

  // Top Platform is generated.
  private static void addTop(List<BlueprintBlock> blocksm int Xcenter, int Zcenter, int y) {
    int radius = 4;
    for(int x = -radius; x <= radius; x++) {
      for(int z = -radius; z <= radius; z++) {
        double distance = Math.sqrt(x*x + z*z);
        if(distance <= radius) {
          blocks.add(new BlueprintBlock(new BlockPos(Xcenter + x, y, Zcenter + z), "minecraft:smooth_quartz"));
        }
      }
    }
  }

  // Adds a variety of stone block items integrated into strcuture to prevent it from looking flat.
  private static String chooseStoneBlock(int x, int y, int z) {
    int value = Math.abs(x*31 + y*17 + z*13);
    int variation = value % 5;
    return switch(variation) {
      case 0 -> "minecraft:smooth_quartz";
      case 1 -> "minecraft:calcite";
      case 2 -> "minecraft:quartz_block";
        default -> "minecraft:smooth_quartz";
    };
  } 
}
