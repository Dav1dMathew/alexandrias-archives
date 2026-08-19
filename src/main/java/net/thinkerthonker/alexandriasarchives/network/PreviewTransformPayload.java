package net.thinkerthonker.alexandriasarchives.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.thinkerthonker.alexandriasarchives.AlexandriaSArchives;

/**
 * Sends the client's current preview origin and rotation to the logical server.
 * The server remains authoritative when /aa build confirm is executed.
 */
public record PreviewTransformPayload(BlockPos origin, int rotation) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            AlexandriaSArchives.MOD_ID, "preview_transform"
    );

    public static final Type<PreviewTransformPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, PreviewTransformPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.origin());
                buf.writeVarInt(payload.rotation());
            },
            buf -> new PreviewTransformPayload(buf.readBlockPos(), Math.floorMod(buf.readVarInt(), 4))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
