package net.thinkerthonker.alexandriasarchives.model;

import net.minecraft.core.BlockPos;

// Represents a block inside a blueprint in Alexandria's Archives.
// This class stores the local position of a block relative to the blueprint block's origin.
// This class enables the block to be locally positioned rather than setting it to a world position,
// enabling the blueprint to be moved, rotated, scaled, previewed and placed at different locations.

public class BlueprintBlock {

    private final BlockPos position;
    private final String blockID;

    // Creates a blueprint block
    // @param position local position within blueprint
    // @param blockID Minecraft block ID, i.e.: "minecraft:oak_wood"

    public BlueprintBlock(BlockPos position, String blockID) {
        this.position = position;
        this.blockID = blockID;
    }

    // Returns local position of block
    public BlockPos getPosition() {
        return position;
    }

    // Returns Minecraft blockID
    public String getBlockID() {
        return blockID;
    }
}
