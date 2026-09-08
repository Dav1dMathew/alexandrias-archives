package net.thinkerthonker.alexandriasarchives.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

// Shared look for the mod's screens, plus the key-cap drawing used by the
// preview controls.
//
// The controls are shown as drawn keys rather than a written list: a player
// hunting for "which button rotates this" reads a picture of the key far
// faster than a sentence naming it.
public final class AAGui {

    public static final int ACCENT = 0xFFD9A441;   // warm gold
    public static final int PANEL = 0xFF141821;
    public static final int PANEL_BORDER = 0xFF3A4A63;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int TEXT_DIM = 0xFFA9B4C4;

    private static final int CAP_FACE = 0xFF2B3346;
    private static final int CAP_EDGE = 0xFF5A6B8C;
    private static final int CAP_SHADOW = 0xFF0E1119;

    private AAGui() {
        // Utility class.
    }

    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, PANEL_BORDER);
        graphics.fill(x, y, x + width, y + height, PANEL);
    }

    /** Draws a single key cap and returns the width it occupied. */
    public static int keyCap(GuiGraphics graphics, Font font, int x, int y, String label) {
        int textWidth = font.width(label);
        int width = Math.max(18, textWidth + 10);
        int height = 18;

        // Drop shadow first, then the face, then a lighter top edge - enough
        // relief to read as a physical key at this size.
        graphics.fill(x + 1, y + 2, x + width + 1, y + height + 2, CAP_SHADOW);
        graphics.fill(x, y, x + width, y + height, CAP_EDGE);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, CAP_FACE);
        graphics.drawString(font, label, x + (width - textWidth) / 2, y + 5, TEXT, false);
        return width;
    }

    /** A row of key caps followed by a description. */
    public static void control(GuiGraphics graphics, Font font, int x, int y,
                               String[] keys, String description) {
        int cursor = x;
        for (String key : keys) {
            cursor += keyCap(graphics, font, cursor, y, key) + 4;
        }
        graphics.drawString(font, description, cursor + 6, y + 5, TEXT_DIM, false);
    }
}
