package net.thinkerthonker.alexandriasarchives.client.preview;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

// Keyboard handling for the preview, polled on the client tick.
//
// Deliberately not key bindings: these are only live while a preview is up,
// and registering eight bindings that do nothing the rest of the time would
// clutter the player's controls screen for no reason.
public final class PreviewInput {

    private static final int[] KEYS = {
            GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN,
            GLFW.GLFW_KEY_PAGE_UP, GLFW.GLFW_KEY_PAGE_DOWN, GLFW.GLFW_KEY_R,
            GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_BACKSPACE,
    };

    // Edge detection, so holding a key nudges once rather than flying the
    // structure across the world in a single tick.
    private static final boolean[] WAS_DOWN = new boolean[KEYS.length];

    // Ticks to ignore input for after a preview appears. The command that
    // creates the preview is usually submitted with Enter, and that key is
    // still physically held when the preview goes live one tick later -
    // which read as a fresh press of "confirm" and built the structure
    // instantly, with no chance to position it.
    private static final int ARM_DELAY_TICKS = 4;
    private static int armCountdown = 0;
    private static boolean wasActive = false;

    private PreviewInput() {
        // Utility class.
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(PreviewInput::tick);
    }

    private static void tick(Minecraft client) {
        if (client.player == null) {
            return;
        }
        boolean active = PreviewManager.isActive();
        if (active && !wasActive) {
            armCountdown = ARM_DELAY_TICKS;
        }
        wasActive = active;
        if (armCountdown > 0) {
            armCountdown--;
        }

        // Only while a preview is up and no screen has focus, so typing in
        // chat or a GUI never moves the structure.
        boolean listening = active && client.screen == null && armCountdown == 0;
        long window = client.getWindow().getWindow();

        for (int i = 0; i < KEYS.length; i++) {
            // The raw state is always recorded, even when not listening.
            // Gating the sample on `listening` meant a key held down through
            // a screen close registered as a brand new press the moment
            // control returned.
            boolean down = InputConstants.isKeyDown(window, KEYS[i]);
            if (listening && down && !WAS_DOWN[i]) {
                act(client, KEYS[i]);
            }
            WAS_DOWN[i] = down;
        }
    }

    private static void act(Minecraft client, int key) {
        switch (key) {
            case GLFW.GLFW_KEY_LEFT -> nudge(-1, 0, 0);
            case GLFW.GLFW_KEY_RIGHT -> nudge(1, 0, 0);
            case GLFW.GLFW_KEY_UP -> nudge(0, 0, -1);
            case GLFW.GLFW_KEY_DOWN -> nudge(0, 0, 1);
            case GLFW.GLFW_KEY_PAGE_UP -> nudge(0, 1, 0);
            case GLFW.GLFW_KEY_PAGE_DOWN -> nudge(0, -1, 0);
            case GLFW.GLFW_KEY_R -> {
                PreviewManager.rotateClockwise();
                PreviewManager.synchronizeTransform();
            }
            case GLFW.GLFW_KEY_ENTER -> command(client, "aa build confirm");
            case GLFW.GLFW_KEY_BACKSPACE -> command(client, "aa build cancel");
            default -> {
            }
        }
    }

    private static void nudge(int x, int y, int z) {
        PreviewManager.move(x, y, z);
        PreviewManager.synchronizeTransform();
    }

    private static void command(Minecraft client, String command) {
        if (client.player != null) {
            client.player.connection.sendCommand(command);
        }
    }
}
