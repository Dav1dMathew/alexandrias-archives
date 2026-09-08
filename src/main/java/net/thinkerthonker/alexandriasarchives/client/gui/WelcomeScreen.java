package net.thinkerthonker.alexandriasarchives.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// The "/aa" landing screen: pick how you want to drive the mod.
//
// Both routes end up sending the same commands. The choice only decides
// whether the player types them or is walked through them, so nothing here
// is a separate feature - it is the same interface with a different surface.
public class WelcomeScreen extends Screen {

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 196;

    // Remembered for the session so the preview screen knows whether to
    // open itself automatically or leave the player in chat.
    public static boolean visualMode = true;

    public WelcomeScreen() {
        super(Component.literal("Alexandria's Archives"));
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int buttonWidth = PANEL_WIDTH - 64;
        int buttonX = panelX + 32;

        this.addRenderableWidget(Button.builder(
                Component.literal("Visual interface"), button -> {
                    visualMode = true;
                    this.minecraft.setScreen(new PromptScreen());
                })
                .bounds(buttonX, panelY + 96, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Chat commands"), button -> {
                    visualMode = false;
                    sendChatHelp();
                })
                .bounds(buttonX, panelY + 126, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                Component.literal("Close"), button -> this.onClose())
                .bounds(buttonX + (buttonWidth - 80) / 2, panelY + PANEL_HEIGHT - 28, 80, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xCC000000);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        AAGui.panel(graphics, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);

        graphics.pose().pushPose();
        graphics.pose().translate(this.width / 2.0, panelY + 18, 0);
        graphics.pose().scale(1.7f, 1.7f, 1.7f);
        graphics.drawCenteredString(this.font, "Alexandria's Archives", 0, 0, AAGui.ACCENT);
        graphics.pose().popPose();

        graphics.drawCenteredString(this.font,
                Component.literal("Describe a structure. Watch it built, block by block.")
                        .withStyle(ChatFormatting.GRAY),
                this.width / 2, panelY + 48, AAGui.TEXT);
        graphics.drawCenteredString(this.font,
                Component.literal("How would you like to work?").withStyle(ChatFormatting.GRAY),
                this.width / 2, panelY + 76, AAGui.TEXT_DIM);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void sendChatHelp() {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.sendCommand("aa build");
        }
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
