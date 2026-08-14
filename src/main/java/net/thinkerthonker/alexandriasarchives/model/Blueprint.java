package net.thinkerthonker.alexandriasarchives.model;

import net.minecraft.core.BlockPos;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


// Represents a complete structure blueprint in Alexandria's Archives
// Class allows Python AI to be fed through the Blueprint JSON file which then feeds data into the
// SpringBoot AI allowing the structure to be generated in Minecraft according to the blueprint.

public class Blueprint {
    private final String ID;
    private final String name;

    private final int width;
    private final int height;
    private final int depth;

    private final List<BlueprintBlock> blocks;

    // Creates a new blueprint.
    // @param ID unique blueprint identifier.
    // @param name human-readable blueprint name.
    // @param height of structure.
    // @param depth of structure.
    // @param blocks contained in structure.

    public Blueprint(String ID, String name, int width, int height, int depth, List<BlueprintBlock> blocks) {
        this.ID = ID;
        this.name = name;
        this. width = width;
        this.height = height;
        this.depth = depth;
        this.blocks = new ArrayList<>(blocks);
    }

    public String getID() {
        return ID;
    }

    public String getName() {
        return name;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    // Returns blocks as immutable.
    public List<BlueprintBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    // Returns total number of blocks in blueprint.
    public int getBlockCount() {
        return blocks.size();
    }

    // Returns maximum possible volume of blueprint.
    public int getVolume() {
        return width * height * depth;
    }

    // Checks whether a local position is within blueprint bounds.
    public boolean containsPosition(BlockPos position) {
        return position.getX() >= 0
                && position.getX() < width
                && position.getY() >= 0
                && position.getY() < height
                && position.getZ() >= 0
                && position.getZ() < depth;
    }

}
