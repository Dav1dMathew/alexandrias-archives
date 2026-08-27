package net.thinkerthonker.alexandriasarchives.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.thinkerthonker.alexandriasarchives.AlexandriaSArchives;

// Class that translates client-side request to move/rotate an active preview to the server-side.
// This is treated as an untrusted request by the server until PreviewNetworking validates the request.

public record PreviewTransformPayload (BlockPos origin, int rotation) implements CustomPacketPayload {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AlexandriaSArchives.MOD_ID,
            "preview_transform");

    public static final Type<PreviewTransformPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, PreviewTransformPayload> CODEC = StreamCodec.of((buf, payload) -> {
        buf.writeBlockPos(payload.origin());
        buf.writeVarInt(Math.floorMod(payload.rotation(), 4));
    }, buf -> new PreviewTransformPayload(buf.readBlockPos(), Math.floorMod(buf.readVarInt(), 4)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
