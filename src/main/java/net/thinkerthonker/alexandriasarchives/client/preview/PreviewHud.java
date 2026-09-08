package net.thinkerthonker.alexandriasarchives.client.preview;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.thinkerthonker.alexandriasarchives.client.gui.AAGui;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;

// The preview controls, drawn as a HUD overlay rather than a Screen.
//
// A Screen grabs the cursor and swallows movement keys, so the player could
// not walk around the structure they were being asked to position - which is
// the one thing you actually want to do while judging placement. Drawn on
// the HUD instead, the game stays fully playable and the controls are keys
// rather than mouse targets.
public final class PreviewHud {

    private PreviewHud() {
        // Utility class.
    }

    public static void initialize() {
        HudRenderCallback.EVENT.register(PreviewHud::render);
    }

    private static void render(GuiGraphics graphics, Object tickCounter) {
        if (!PreviewManager.isActive()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.screen != null || client.options.hideGui) {
            return;
        }

        int width = client.getWindow().getGuiScaledWidth();
        int barHeight = 62;
        int barY = 4;

        graphics.fill(4, barY, width - 4, barY + barHeight, 0xCC0E1119);
        graphics.fill(4, barY, width - 4, barY + 1, AAGui.PANEL_BORDER);

        Blueprint blueprint = PreviewManager.getBlueprint();
        String title = blueprint == null ? "Preview" : blueprint.getName();
        graphics.drawString(client.font, title, 14, barY + 7, AAGui.ACCENT, false);
        if (blueprint != null) {
            String size = blueprint.getWidth() + " x " + blueprint.getHeight() + " x "
                    + blueprint.getDepth() + "  -  " + blueprint.getBlockCount() + " blocks";
            graphics.drawString(client.font, size, 14, barY + 19, AAGui.TEXT_DIM, false);
        }

        int row = barY + 34;
        int x = 14;
        x += controls(graphics, client, x, row, new String[]{"←", "→", "↑", "↓"}, "Move");
        x += controls(graphics, client, x + 18, row, new String[]{"R"}, "Rotate");
        x += controls(graphics, client, x + 36, row, new String[]{"PgUp", "PgDn"}, "Raise / lower");
        x += controls(graphics, client, x + 54, row, new String[]{"Enter"}, "Build it");
        controls(graphics, client, x + 72, row, new String[]{"Bksp"}, "Cancel");
    }

    private static int controls(GuiGraphics graphics, Minecraft client, int x, int y,
                                String[] keys, String label) {
        int start = x;
        for (String key : keys) {
            x += AAGui.keyCap(graphics, client.font, x, y, key) + 4;
        }
        graphics.drawString(client.font, label, x + 4, y + 5, AAGui.TEXT_DIM, false);
        return (x + 4 + client.font.width(label)) - start;
    }
}
