package net.thinkerthonker.alexandriasarchives.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.thinkerthonker.alexandriasarchives.builder.BuildManager;
import net.thinkerthonker.alexandriasarchives.builder.PreviewSession;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;

public final class PreviewNetworking {
    private static final int MAX_PREVIEW_MOVE_PER_PACKET = 32;
    private static final int MAX_PREVIEW_DISTANCE_FROM_PLAYER = 128;

    private PreviewNetworking() {
    }
    public static void initialize () {
        PayloadTypeRegistry.playS2C().register(PreviewPayload.TYPE, PreviewPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(PreviewPayload.TYPE, PreviewPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(PreviewTransformPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> handleTransform(player, payload));});
    }
    private static void handleTransform (ServerPlayer player, PreviewTransformPayload payload){
        PreviewSession preview = BuildManager.getPreview(player);
        if (preview == null) {
            return;
        }
        BlockPos current = preview.getOrigin();
        BlockPos requested = payload.origin();
        if(!isReasonableMove(current, requested)) {
            sendAuthoritativePreview(player, preview);
            return;
        }
        if(!isNearPlayer(player, requested)) {
            sendAuthoritativePreview(player, preview);
            return;
        }
        preview.setOrigin(requested);
        preview.setRotation(payload.rotation());
        sendAuthoritativePreview(player, preview);
    }
    private static boolean isReasonableMove(BlockPos current, BlockPos requested) {
        return Math.abs(requested.getX() - current.getX()) <= MAX_PREVIEW_MOVE_PER_PACKET &&
                Math.abs(requested.getY() - current.getY()) <= MAX_PREVIEW_MOVE_PER_PACKET &&
                Math.abs(requested.getZ() - current.getZ()) <= MAX_PREVIEW_MOVE_PER_PACKET;
    }
    private static boolean isNearPlayer(ServerPlayer player, BlockPos origin) {
        return player.blockPosition().distSqr(origin) <= (double) MAX_PREVIEW_DISTANCE_FROM_PLAYER *
                MAX_PREVIEW_DISTANCE_FROM_PLAYER;
    }

    public static void sendPreview(ServerPlayer player, Blueprint blueprint, BlockPos origin, int rotation) {
        BlueprintBlock[] blocks = blueprint.getBlocks().toArray(new BlueprintBlock[0]);
        BlockPos[] positions = new BlockPos[blocks.length];
        String[] IDs = new String[blocks.length];
        for(int i = 0; i < blocks.length; i++){
            positions[i] = blocks[i].getPosition();
            IDs[i] = blocks[i].getBlockID();
        }
        ServerPlayNetworking.send(player, new PreviewPayload(true, blueprint.getID(), blueprint.getName(),
                blueprint.getWidth(), blueprint.getHeight(), blueprint.getDepth(), origin, Math.floorMod(rotation, 4),
                positions, IDs));
    }
    public static void sendAuthoritativePreview(ServerPlayer player, PreviewSession preview) {
        sendPreview(player, preview.getBlueprint(), preview.getOrigin(), preview.getRotation());
    }
    public static void sendStop(ServerPlayer player) {
        ServerPlayNetworking.send(player, PreviewPayload.stop());
    }
}