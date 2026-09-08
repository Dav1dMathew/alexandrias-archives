package net.thinkerthonker.alexandriasarchives.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Step one of the visual flow: a small popup asking what to build.
//
// Deliberately not a full-screen takeover - the player is choosing what to
// put in the world in front of them, so the world stays visible behind it.
public class PromptScreen extends Screen {

    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_HEIGHT = 116;

    private EditBox promptBox;

    public PromptScreen() {
        super(Component.literal("Build"));
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        this.promptBox = new EditBox(this.font, panelX + 20, panelY + 42,
                PANEL_WIDTH - 40, 20, Component.literal("Prompt"));
        this.promptBox.setMaxLength(256);
        this.promptBox.setHint(Component.literal("e.g. the eiffel tower")
                .withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.promptBox);
        this.setInitialFocus(this.promptBox);

        int buttonY = panelY + PANEL_HEIGHT - 30;
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(panelX + 20, buttonY, 90, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Generate"), b -> submit())
                .bounds(panelX + PANEL_WIDTH - 110, buttonY, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x99000000);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        AAGui.panel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.drawString(this.font, "What should I build?", panelX + 20, panelY + 16,
                AAGui.ACCENT, false);
        graphics.drawString(this.font,
                Component.literal("Naming a known landmark loads it directly.")
                        .withStyle(ChatFormatting.GRAY),
                panelX + 20, panelY + 28, AAGui.TEXT_DIM, false);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 257 || keyCode == 335) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void submit() {
        String prompt = this.promptBox.getValue().trim();
        if (prompt.isEmpty()) {
            return;
        }
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.sendCommand("aa build " + prompt);
        }
        // The preview screen opens by itself once the server answers with a
        // preview; closing here keeps the world clear in the meantime.
        this.onClose();
    }


    // Vanilla's renderBackground() applies the pause-menu blur post effect,
    // which smears everything already drawn this frame - the panel and its
    // text - while widgets drawn afterwards stay sharp. The dim is drawn by
    // hand in render() instead, so this is deliberately empty.
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
