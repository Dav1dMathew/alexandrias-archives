package net.thinkerthonker.alexandriasarchives.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.thinkerthonker.alexandriasarchives.client.gui.BuildScreen;
import net.thinkerthonker.alexandriasarchives.client.preview.PreviewManager;
import net.thinkerthonker.alexandriasarchives.client.preview.PreviewRenderer;
import org.lwjgl.glfw.GLFW;

// Handles the client-side entry point for Alexandria's Archives.
// This class handles the starting of the Falcraft preview layer and keeps the rendering
// of the preview separate from the server-side building.
public class AAClient implements ClientModInitializer {

    private static KeyMapping openBuildScreenKey;

    @Override
    public void onInitializeClient() {
        PreviewManager.initialize();
        PreviewRenderer.initialize();

        openBuildScreenKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.alexandriasarchives.open_build_screen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.alexandriasarchives"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PreviewManager.tick(client);

            while (openBuildScreenKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new BuildScreen());
                }
            }
        });
    }
}
