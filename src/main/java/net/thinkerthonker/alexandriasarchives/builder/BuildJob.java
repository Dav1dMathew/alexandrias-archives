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

    // Placement is spread across ticks so a large structure never freezes
    // the server, but a fixed rate does not survive the range of sizes here:
    // at 100 blocks/tick the Concorde (2,483 blocks) finishes in a little
    // over a second while the Burj Khalifa (326,630) would take nearly three
    // minutes. Instead the rate is chosen per job to land near a target
    // duration, so every landmark reads as the same deliberate, visible
    // block-by-block assembly.
    private static final int TARGET_TICKS = 170;   // ~8.5 seconds
    private static final int MIN_BLOCKS_PER_TICK = 25;
    private static final int MAX_BLOCKS_PER_TICK = 4000;

    private final int blocksPerTick;
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
        this.blocksPerTick = Math.max(
                MIN_BLOCKS_PER_TICK,
                Math.min(MAX_BLOCKS_PER_TICK,
                        (blueprint.getBlockCount() + TARGET_TICKS - 1) / TARGET_TICKS));
    }

    // Processes next portion of build.
    public void tick() {
        if(finished || cancelled) {
            return;
        }
        List<BlueprintBlock> blocks = blueprint.getBlocks();
        int blocksThisTick = 0;
        while(currentIndex < blocks.size() && blocksThisTick < blocksPerTick) {
            BlueprintBlock blueprintBlock = blocks.get(currentIndex);
            BlockPos worldPosition = BlueprintTransform.transform(blueprintBlock.getPosition(), origin, rotation);

            // Saves a copy of the original world state before it is modified.
            // This allows the Player to reset / undo the build later on.
            originalStates.putIfAbsent(worldPosition, level.getBlockState(worldPosition));
            StructureBuilder.placeBlockAt(level, worldPosition, blueprintBlock);
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
