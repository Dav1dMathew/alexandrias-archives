package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import java.util.List;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Represents structure-building operation currently in progress.

// * IMPORTANT NOTE FOR COLLABORATORS *
// BuildJob deliberately has been designed to place each individual block during execution.
// It uses BuildManager to call tick() once per server tick to prevent large
// AI-generated structures from freezing the server.


public class BuildJob {

    // Number of placeable blocks in one tick.
    private static final int BLOCKS_PER_TICK = 100;
    private final ServerLevel level;
    private final BlockPos origin;
    private final Blueprint blueprint;

    // Stores original state of every block changed by BuildJob. A LinkedHashMap has been implemented to give us
    // a deterministic restoration order.
    private final Map<BlockPos, BlockState> originalStates = new LinkedHashMap<>();
    private final int rotation;
    private int currentIndex = 0;
    private boolean finished = false;
    private boolean cancelled = false;

    public BuildJob(ServerLevel level, BlockPos origin, Blueprint blueprint, int rotation) {
        this.level = level;
        this.origin = origin;
        this.blueprint = blueprint;
        this.rotation = Math.floorMod(rotation, 4);
    }

    // Processes next portion of build.
    public void tick() {
        if(finished || cancelled) {
            return;
        }
        List<BlueprintBlock> blocks = blueprint.getBlocks();
        int blocksThisTick = 0;
        while(currentIndex < blocks.size() && blocksThisTick < BLOCKS_PER_TICK) {
            BlueprintBlock blueprintBlock = blocks.get(currentIndex);
            BlockPos worldPosition = BlueprintTransform.transform(localPosition, origin, rotation);

            // Saves a copy of the original world state before it is modified.
            // This allows the Player to reset / undo the build later on.
            originalStates.putIfAbsent(worldPosition, level.getBlockState(worldPosition));
            StructureBuilder.placeBlock(level, origin, blueprintBlock);
            currentIndex++;
            blocksThisTick++;
        }

        if(currentIndex >=  blocks.size()) {
            finished = true;
        }
    }

    // Build Cancellation Function.
    // NOTE: WorldState Restoration function TBA in BuildManager
    public void cancel() {
        cancelled = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public ServerLevel getLevel() {
        return level;
    }

    // Counts number of processed Blocks.
    public int getProgress() {
        return currentIndex;
    }

    // Returns total number of blocks.
    public int getTotalBlocks() {
        return blueprint.getBlockCount();
    }

    // Returns progress from 0.0 to 1.0.
    public double getProgressRatio() {
        if(getTotalBlocks() == 0) {
            return 1.0;
        }
        return(double) currentIndex / getTotalBlocks();
    }

    public Blueprint getBlueprint() {
        return blueprint;
    }

    // Returns original world state. Returned Map is immutable and cannot be externally modified.
    public Map<BlockPos, BlockState> getOriginalStates() {
        return Collections.unmodifiableMap(originalStates);
    }
}
