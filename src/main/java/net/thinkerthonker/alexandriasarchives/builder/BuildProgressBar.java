package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// The boss bar shown while a structure loads and assembles.
//
// Placement is deliberately spread over ~8 seconds of visible block-by-block
// work, which is long enough that silence reads as a freeze. The bar gives
// that time a shape: a yellow loading phase while the model is resolved and
// validated, a green fill that tracks real placement progress, and a red
// state that stays up long enough to actually read on failure.
public final class BuildProgressBar {

    private static final Map<UUID, ServerBossEvent> BARS = new HashMap<>();

    private BuildProgressBar() {
        // Utility class.
    }

    public static void open(ServerPlayer player, String title) {
        if (player == null) {
            return;
        }
        close(player);
        ServerBossEvent bar = new ServerBossEvent(
                Component.literal("Alexandria's Archives: " + title),
                BossEvent.BossBarColor.YELLOW,
                BossEvent.BossBarOverlay.PROGRESS
        );
        bar.setProgress(0.0f);
        bar.addPlayer(player);
        BARS.put(player.getUUID(), bar);
    }

    public static void update(ServerPlayer player, String title, float progress) {
        ServerBossEvent bar = bar(player);
        if (bar == null) {
            return;
        }
        bar.setName(Component.literal("Alexandria's Archives: " + title));
        bar.setProgress(Math.max(0.0f, Math.min(1.0f, progress)));
    }

    public static void placing(ServerPlayer player, String name, int placed, int total, float progress) {
        update(player, name + " - placing " + placed + " / " + total, progress);
        ServerBossEvent bar = bar(player);
        if (bar != null) {
            bar.setColor(BossEvent.BossBarColor.GREEN);
        }
    }

    public static void complete(ServerPlayer player, String name) {
        ServerBossEvent bar = bar(player);
        if (bar == null) {
            return;
        }
        bar.setName(Component.literal("Alexandria's Archives: " + name + " complete"));
        bar.setColor(BossEvent.BossBarColor.GREEN);
        bar.setProgress(1.0f);
        // Held briefly rather than vanishing on the last block, so the
        // finished state is actually seen.
        Scheduler.after(player, 30, () -> close(player));
    }

    public static void fail(ServerPlayer player, String reason) {
        ServerBossEvent bar = bar(player);
        if (bar == null) {
            return;
        }
        bar.setName(Component.literal("Alexandria's Archives: " + reason));
        bar.setColor(BossEvent.BossBarColor.RED);
        bar.setProgress(1.0f);
        Scheduler.after(player, 60, () -> close(player));
    }

    public static void close(ServerPlayer player) {
        if (player == null) {
            return;
        }
        ServerBossEvent bar = BARS.remove(player.getUUID());
        if (bar != null) {
            bar.removeAllPlayers();
        }
    }

    private static ServerBossEvent bar(ServerPlayer player) {
        return player == null ? null : BARS.get(player.getUUID());
    }
}
