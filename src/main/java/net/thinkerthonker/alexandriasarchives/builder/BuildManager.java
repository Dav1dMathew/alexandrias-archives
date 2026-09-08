package net.thinkerthonker.alexandriasarchives.builder;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.network.PreviewNetworking;
// import java.util.ArrayDeque;
// import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// BuildManager is the central manager for Alexandria's Archives' structure generation.
// It starts the build process and processes the BuildJob within the WorldState through every server tick.
// Additonally, it handles build cancellation, restoration and the 'Undo' command.
public final class BuildManager {

    // One active build per player.
    private static final Map<UUID, BuildJob> ACTIVE_JOBS = new HashMap<>();

    // Stores individual player data for each one's most recent completed build.
    // NOTE: As of Week 2 only 1 Undo level has been implemented.
    private static final Map<UUID, Map<BlockPos, BlockState>> LAST_BUILDS = new HashMap<>();
    private static final Map<UUID, PreviewSession> PREVIEWS = new HashMap<>();
    private BuildManager() {
    }

    // Registers server tick listener and calls this exactly once during mod initialization.
    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(BuildManager::tick);
    }

    // Processes all active construction jobs.
    private static void tick(MinecraftServer server) {
        ACTIVE_JOBS.entrySet().removeIf(entry -> {
            UUID playerID = entry.getKey();
            BuildJob job = entry.getValue();

            // Processes the next set of blocks.
            job.tick();
            ServerPlayer player = server.getPlayerList().getPlayer(playerID);

            // Finished Successfully.
            if(job.isFinished()) {
                LAST_BUILDS.put(playerID, new HashMap<>(job.getOriginalStates()));
                if(player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Alexandria's Archives: " + "Build Job Complete! " + job.getTotalBlocks()
                                    + "blocks placed."));
                }
                return true;
            }

            // Build Cancelled.
            if(job.isCancelled()) {
                restore(job.getLevel(), job.getOriginalStates());
                if(player != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Alexandria's Archives: Build Canceled."));
                }
                return true;
            }
            return false;
        });
    }

    // Function for when Player starts a new build.
    // @return false if Player currently has a build running.
    public static boolean startBuild(ServerPlayer player, Blueprint blueprint, BlockPos origin, int rotation) {
        UUID  playerID = player.getUUID();
        if(ACTIVE_JOBS.containsKey(playerID)) {
            return false;
        }
        ServerLevel level = player.serverLevel();
        BuildJob job = new BuildJob(level, origin, blueprint, rotation);
        ACTIVE_JOBS.put(playerID, job);
        return true;
    }

    public static boolean startPreview(ServerPlayer player, Blueprint blueprint, BlockPos origin) {
        UUID playerID = player.getUUID();
        if(ACTIVE_JOBS.containsKey(playerID)) {
            return false;
        }

        if(PREVIEWS.containsKey(playerID)) {
            return false;
        }

        if(blueprint == null || origin == null) {
            return false;
        }

        PREVIEWS.put(playerID, new PreviewSession(blueprint, origin));
        // PreviewNetworking.sendPreview(player, blueprint, origin, 0);
        return true;
    }

    public static PreviewSession getPreview(ServerPlayer player){
        return PREVIEWS.get(player.getUUID());
    }

    // Player cancels an active build.
    public static boolean cancelBuild(ServerPlayer player) {
        BuildJob job = ACTIVE_JOBS.get(player.getUUID());
        if(job == null) {
            return false;
        }
        job.cancel();
        return true;
    }

    // Build Preview is removed & cancelled.
    public static boolean cancelPreview(ServerPlayer player) {
        UUID playerID = player.getUUID();
        PreviewSession removed = PREVIEWS.remove(playerID);
        if(removed == null) {
            return false;
        }
        PreviewNetworking.sendStop(player);
        return true;
    }

    public static boolean confirmPreview(ServerPlayer player) {
        UUID playerID = player.getUUID();
        if(ACTIVE_JOBS.containsKey(playerID)) {
            return false;
        }
        PreviewSession preview = PREVIEWS.get(playerID);
        if(preview == null) {
            return false;
        }
        boolean started = startBuild(player, preview.getBlueprint(), preview.getOrigin(), preview.getRotation());
        if(!started) {
            return false;
        }
        PREVIEWS.remove(playerID);
        
        // Stops rendering.
        PreviewNetworking.sendStop(player);
        return true;
    }

    // Restores the previously completed build.
    public static boolean undo(ServerPlayer player) {
        UUID playerID = player.getUUID();
        Map<BlockPos, BlockState> previousStates = LAST_BUILDS.remove(playerID);
        if(previousStates == null || previousStates.isEmpty()) {
            return false;
        }
        restore(player.serverLevel(), previousStates);
        return true;
    }

    // Blocks are restored to their previous states.
    private static void restore(ServerLevel level, Map<BlockPos, BlockState> states) {
        for(Map.Entry<BlockPos, BlockState> entry : states.entrySet()) {
            level.setBlock(entry.getKey(), entry.getValue(), 3);
        }
    }

    // Checks if Player has a build currently running.
    public static boolean isBuilding(ServerPlayer player) {
        return ACTIVE_JOBS.containsKey(player.getUUID());
    }
    public static boolean hasActiveBuildOrPreview(ServerPlayer player) {
        UUID playerID = player.getUUID();
        return ACTIVE_JOBS.containsKey(playerID) || PREVIEWS.containsKey(playerID);
    }
}
