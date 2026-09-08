package net.thinkerthonker.alexandriasarchives.client.preview;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.thinkerthonker.alexandriasarchives.client.gui.PreviewControlScreen;
import net.thinkerthonker.alexandriasarchives.client.gui.WelcomeScreen;
import net.thinkerthonker.alexandriasarchives.network.OpenUiPayload;
import net.thinkerthonker.alexandriasarchives.network.PreviewPayload;

/**
 * Handles preview synchronization received from the logical server.
 */
public final class PreviewClientNetworking {

    private PreviewClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(PreviewPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    PreviewManager.applyPayload(payload);

                    // The controls are a HUD overlay (PreviewHud), not a
                    // screen, so nothing needs opening here - and the player
                    // keeps full movement while positioning the structure.
                    Minecraft client = context.client();
                    if (client.screen instanceof PreviewControlScreen && !payload.active()) {
                        client.setScreen(null);
                    }
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(OpenUiPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    Minecraft client = context.client();
                    if (OpenUiPayload.WELCOME.equals(payload.screen())) {
                        client.setScreen(new WelcomeScreen());
                    }
                })
        );
    }
}
