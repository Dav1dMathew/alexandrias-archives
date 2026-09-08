package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;

// Converts Alexandria's Archives Blueprint into Minecraft blocks.
// Class is isolated from AI, Python, SpringBoot & Images as it's only function is to directly translate a given Blueprint into Minecraft.

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
        placeBlockAt(level, origin.offset(blueprintBlock.getPosition()), blueprintBlock);
    }

    // Places at an already-resolved world position. BuildJob computes the
    // rotated position itself; the origin-relative overload above ignores
    // rotation, so using it for placement built an unrotated structure while
    // the preview showed a rotated one.
    public static void placeBlockAt(
            ServerLevel level,
            BlockPos worldPosition,
            BlueprintBlock blueprintBlock
    ) {
        // Parsed through BlockStates so bracketed properties (stair facing,
        // slab half, pillar axis) survive. Returns null only for a block id
        // that is not in the registry, which BlueprintValidator screens for.
        BlockState blockState = BlockStates.parse(blueprintBlock.getBlockID());
        if(blockState == null) {
            return;
        }

        level.setBlock(worldPosition, blockState, 3);
    }

    // Builds entire blueprint immediately.
    // For larger structures we will eventually use BuildJob to distribute placement across
    // multiple server ticks.
    public static void placeBlock(ServerLevel level, BlockPos origin, Blueprint blueprint) {
        for(BlueprintBlock blueprintBlock : blueprint.getBlocks()) {
            placeBlock(level, origin, blueprintBlock);
        }
    }
}

