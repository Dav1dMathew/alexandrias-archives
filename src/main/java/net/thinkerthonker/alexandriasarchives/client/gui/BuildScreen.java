package net.thinkerthonker.alexandriasarchives.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Full-screen replacement for typing "/aa build <prompt>" by hand.
// Purely a client-side input surface - on submit it sends the exact
// same command the chat version would, so all server-side validation/
// generation/placement logic is completely unchanged and unaware this
// GUI exists.
public class BuildScreen extends Screen {

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 170;
    private static final int ACCENT_COLOR = 0xFFD9A441; // warm gold, matches the project's docs/diagram palette
    private static final int PANEL_BACKGROUND = 0xE6141821;
    private static final int PANEL_BORDER = 0xFF3A4A63;

    private EditBox promptBox;

    public BuildScreen() {
        super(Component.literal("Alexandria's Archives"));
    }

    @Override
    protected void init() {
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        this.promptBox = new EditBox(
                this.font,
                panelX + 24,
                panelY + 62,
                PANEL_WIDTH - 48,
                22,
                Component.literal("Describe a structure to build")
        );
        this.promptBox.setMaxLength(256);
        this.promptBox.setHint(Component.literal("e.g. \"the leaning tower of pisa\"").withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.promptBox);
        this.setInitialFocus(this.promptBox);

        int buttonY = panelY + PANEL_HEIGHT - 34;
        int buttonWidth = 120;

        this.addRenderableWidget(Button.builder(Component.literal("Build"), button -> submit())
                .bounds(panelX + PANEL_WIDTH - 24 - buttonWidth, buttonY, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), button -> this.onClose())
                .bounds(panelX + 24, buttonY, buttonWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Demo"), button -> sendCommand("aa build demo"))
                .bounds(panelX + 24 + buttonWidth + 12, buttonY, buttonWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Full-window dim, then a centered panel - the actual "full
        // screen" surface (not a small vanilla-style dialog box).
        this.renderTransparentBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, this.height, 0x99000000);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(panelX - 2, panelY - 2, panelX + PANEL_WIDTH + 2, panelY + PANEL_HEIGHT + 2, PANEL_BORDER);
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_BACKGROUND);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(this.width / 2.0, panelY + 16, 0);
        guiGraphics.pose().scale(1.8f, 1.8f, 1.8f);
        guiGraphics.drawCenteredString(this.font, "Alexandria's Archives", 0, 0, ACCENT_COLOR);
        guiGraphics.pose().popPose();

        guiGraphics.drawCenteredString(
                this.font,
                Component.literal("Describe what you want to build").withStyle(ChatFormatting.GRAY),
                this.width / 2, panelY + 42, 0xFFFFFF
        );

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Enter/numpad-enter submits, same as most chat-like text inputs.
        if ((keyCode == 257 || keyCode == 335) && this.promptBox.isFocused()) {
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
        sendCommand("aa build " + prompt);
    }

    private void sendCommand(String command) {
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
