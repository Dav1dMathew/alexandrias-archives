package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import java.util.List;

// Represents structure-building operation currently in progress.
// Instead of placing 50,000 blocks in one server tick, a BuildJob places a limited
// number of blocks a time.
// It prevents large AI-generated structures from freezing the server.

public class BuildJob {
    // Number of placeable blocks in one tick.
    private static final int BLOCKS_PER_TICK = 100;
    private final ServerLevel level;
    private final BlockPos origin;
    private final Blueprint blueprint;
    private int currentIndex = 0;
    private boolean finished = false;
    private boolean cancelled = false;

    public BuildJob(ServerLevel level, BlockPos origin, Blueprint blueprint) {
        this.level = level;
        this.origin = origin;
        this.blueprint = blueprint;
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
            StructureBuilder.placeBlock(level, origin, blueprintBlock);
            currentIndex++;
            blocksThisTick++;
        }

        if(currentIndex >=  blocks.size()) {
            finished = true;
        }
    }

    // Build Cancellation Function\
    public void cancel() {
        cancelled = true;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isCancelled() {
        return cancelled;
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
}
