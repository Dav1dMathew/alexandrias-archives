package net.thinkerthonker.alexandriasarchives.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.thinkerthonker.alexandriasarchives.builder.BuildManager;
import net.thinkerthonker.alexandriasarchives.builder.PreviewSession;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;

/**
 * Registers and sends the server-to-client preview payload and receives
 * authoritative client preview transforms.
 */
public final class PreviewNetworking {

    private PreviewNetworking() {
    }

    public static void initialize() {
        PayloadTypeRegistry.playS2C().register(PreviewPayload.TYPE, PreviewPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PreviewTransformPayload.TYPE, PreviewTransformPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PreviewTransformPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            PreviewSession preview = BuildManager.getPreview(player);
            if (preview == null) {
                return;
            }

            context.server().execute(() -> {
                PreviewSession currentPreview = BuildManager.getPreview(player);
                if (currentPreview != null) {
                    currentPreview.setOrigin(payload.origin());
                    currentPreview.setRotation(payload.rotation());
                }
            });
        });
    }

    public static void sendPreview(ServerPlayer player, Blueprint blueprint, BlockPos origin, int rotation) {
        BlueprintBlock[] blocks = blueprint.getBlocks().toArray(new BlueprintBlock[0]);
        BlockPos[] positions = new BlockPos[blocks.length];
        String[] ids = new String[blocks.length];

        for (int i = 0; i < blocks.length; i++) {
            positions[i] = blocks[i].getPosition();
            ids[i] = blocks[i].getBlockID();
        }

        ServerPlayNetworking.send(player, new PreviewPayload(
                true,
                blueprint.getID(),
                blueprint.getName(),
                blueprint.getWidth(),
                blueprint.getHeight(),
                blueprint.getDepth(),
                origin,
                rotation,
                positions,
                ids
        ));
    }

    public static void sendStop(ServerPlayer player) {
        ServerPlayNetworking.send(player, PreviewPayload.stop());
    }
}
