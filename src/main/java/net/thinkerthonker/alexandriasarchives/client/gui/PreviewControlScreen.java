package net.thinkerthonker.alexandriasarchives.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.thinkerthonker.alexandriasarchives.client.preview.PreviewManager;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import org.lwjgl.glfw.GLFW;

// Step two of the visual flow: position the preview, then commit.
//
// This sits at the bottom of the screen rather than in the middle, and does
// not pause the game, because the whole point is to keep looking at the
// structure while nudging it. Keys are drawn as caps instead of listed as
// text - "press the thing that looks like this" beats reading a table.
public class PreviewControlScreen extends Screen {

    private static final int BAR_HEIGHT = 92;
    private static final int MOVE_STEP = 1;

    public PreviewControlScreen() {
        super(Component.literal("Preview"));
    }

    @Override
    protected void init() {
        int barY = this.height - BAR_HEIGHT;
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> command("aa build cancel"))
                .bounds(this.width / 2 - 104, barY + BAR_HEIGHT - 26, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Build it"), b -> command("aa build confirm"))
                .bounds(this.width / 2 + 4, barY + BAR_HEIGHT - 26, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!PreviewManager.isActive()) {
            // The preview went away (built, cancelled, or the server said so).
            this.onClose();
            return;
        }

        int barY = this.height - BAR_HEIGHT;
        graphics.fill(0, barY, this.width, this.height, 0xCC0E1119);
        graphics.fill(0, barY, this.width, barY + 1, AAGui.PANEL_BORDER);

        Blueprint blueprint = PreviewManager.getBlueprint();
        String title = blueprint == null ? "Preview" : blueprint.getName();
        graphics.drawString(this.font, title, 14, barY + 10, AAGui.ACCENT, false);
        if (blueprint != null) {
            String size = blueprint.getWidth() + " x " + blueprint.getHeight() + " x "
                    + blueprint.getDepth() + "   " + blueprint.getBlockCount() + " blocks";
            graphics.drawString(this.font, size, 14, barY + 22, AAGui.TEXT_DIM, false);
        }

        int row = barY + 40;
        AAGui.control(graphics, this.font, 14, row,
                new String[]{"←", "→", "↑", "↓"}, "Move");
        AAGui.control(graphics, this.font, 240, row,
                new String[]{"R"}, "Rotate");
        AAGui.control(graphics, this.font, 340, row,
                new String[]{"PgUp", "PgDn"}, "Raise / lower");

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    // No dim and no blur: the structure behind this bar is the thing being
    // judged, so the world has to stay fully legible while the bar is up.
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_LEFT -> nudge(-MOVE_STEP, 0, 0);
            case GLFW.GLFW_KEY_RIGHT -> nudge(MOVE_STEP, 0, 0);
            case GLFW.GLFW_KEY_UP -> nudge(0, 0, -MOVE_STEP);
            case GLFW.GLFW_KEY_DOWN -> nudge(0, 0, MOVE_STEP);
            case GLFW.GLFW_KEY_PAGE_UP -> nudge(0, MOVE_STEP, 0);
            case GLFW.GLFW_KEY_PAGE_DOWN -> nudge(0, -MOVE_STEP, 0);
            case GLFW.GLFW_KEY_R -> {
                PreviewManager.rotateClockwise();
                PreviewManager.synchronizeTransform();
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> command("aa build confirm");
            default -> {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return true;
    }

    private void nudge(int x, int y, int z) {
        PreviewManager.move(x, y, z);
        PreviewManager.synchronizeTransform();
    }

    private void command(String command) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.sendCommand(command);
        }
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
