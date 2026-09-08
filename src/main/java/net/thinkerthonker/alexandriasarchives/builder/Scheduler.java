package net.thinkerthonker.alexandriasarchives.builder;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

// Runs a task a number of server ticks from now, on the server thread.
//
// Used for the short holds on the progress bar. Doing this with a delayed
// CompletableFuture would run the callback off-thread, which is not safe for
// anything that touches level or player state.
public final class Scheduler {

    private record Task(MinecraftServer server, int dueTick, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static int tickCounter = 0;

    private Scheduler() {
        // Utility class.
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (TASKS.isEmpty()) {
                return;
            }
            List<Task> due = new ArrayList<>();
            TASKS.removeIf(task -> {
                if (tickCounter >= task.dueTick()) {
                    due.add(task);
                    return true;
                }
                return false;
            });
            for (Task task : due) {
                task.action().run();
            }
        });
    }

    public static void after(ServerPlayer player, int ticks, Runnable action) {
        if (player == null || player.getServer() == null) {
            return;
        }
        TASKS.add(new Task(player.getServer(), tickCounter + Math.max(1, ticks), action));
    }
}
