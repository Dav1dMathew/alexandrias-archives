package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;

// Converts Alexandria's Archives Blueprint into Minecraft blocks.
// Class is isolated from AI, Python, SpringBoot & Images as it's only function is
// to directly translate a given Blueprint into Minecraft.

public final class StructureBuilder {
    private StructureBuilder() {
        // Utility class.
    }

    // Places one blueprint block into a Minecraft World.
    // @param level Minecraft server level
    // @param origin world position where the blueprint starts
    // @param blueprintBlock block from blueprint

    public static void placeBlock(
            ServerLevel level,
            BlockPos origin,
            BlueprintBlock blueprintBlock
    ) {
        BlockPos worldPosition = origin.offset(
                blueprintBlock.getPosition()
        );
        ResourceLocation blockID = ResourceLocation.parse(
                blueprintBlock.getBlockID()
        );
        Block block = BuiltInRegistries.BLOCK.get(blockID);
        BlockState blockState = block.defaultBlockState();
        level.setBlock(worldPosition, blockState, 3);
    }

    // Builds entire blueprint immediately.
    // For larger structures we will eventually use BuildJob to distribute placement across
    // multiple server ticks.
    public static void placeBlock(
            ServerLevel level,
            BlockPos origin,
            Blueprint blueprint
    ) {
        for(BlueprintBlock blueprintBlock : blueprint.getBlocks()) {
            placeBlock(level, origin, blueprintBlock);
        }
    }
}

