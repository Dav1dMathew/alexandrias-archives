package net.thinkerthonker.alexandriasarchives.client.preview;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.thinkerthonker.alexandriasarchives.network.PreviewPayload;

/**
 * Handles preview synchronization received from the logical server.
 */
public final class PreviewClientNetworking {

    private PreviewClientNetworking() {
    }

    public static void initialize() {
        ClientPlayNetworking.registerGlobalReceiver(PreviewPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PreviewManager.applyPayload(payload))
        );
    }
}
