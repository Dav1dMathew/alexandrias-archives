package net.thinkerthonker.alexandriasarchives.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.thinkerthonker.alexandriasarchives.client.preview.PreviewManager;
import net.thinkerthonker.alexandriasarchives.client.preview.PreviewRenderer;
import net.thinkerthonker.alexandriasarchives.client.preview.PreviewClientNetworking;

// Handles the client-side entry point for Alexandria's Archives.
// This class handles the starting of the Falcraft preview layer and keeps the rendering
// of the preview separate from the server-side building.
public class AAClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PreviewManager.initialize();
        PreviewClientNetworking.initialize();
        PreviewRenderer.initialize();
        ClientTickEvents.END_CLIENT_TICK.register(PreviewManager::tick);
    }
}
