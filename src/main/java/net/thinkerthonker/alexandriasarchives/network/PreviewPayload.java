package net.thinkerthonker.alexandriasarchives.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.thinkerthonker.alexandriasarchives.AlexandriaSArchives;

/**
 * Server-to-client payload used to synchronize the active Alexandria's Archives
 * preview with the client renderer.
 */
public record PreviewPayload(
        boolean active,
        String blueprintId,
        String blueprintName,
        int width,
        int height,
        int depth,
        BlockPos origin,
        int rotation,
        BlockPos[] blockPositions,
        String[] blockIds
) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            AlexandriaSArchives.MOD_ID, "preview"
    );

    public static final Type<PreviewPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, PreviewPayload> CODEC = StreamCodec.of(
            PreviewPayload::write,
            PreviewPayload::read
    );

    private static void write(FriendlyByteBuf buf, PreviewPayload payload) {
        buf.writeBoolean(payload.active());
        buf.writeUtf(payload.blueprintId(), 128);
        buf.writeUtf(payload.blueprintName(), 256);
        buf.writeVarInt(payload.width());
        buf.writeVarInt(payload.height());
        buf.writeVarInt(payload.depth());
        buf.writeBlockPos(payload.origin());
        buf.writeVarInt(payload.rotation());

        int count = Math.min(payload.blockPositions().length, payload.blockIds().length);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buf.writeBlockPos(payload.blockPositions()[i]);
            buf.writeUtf(payload.blockIds()[i], 128);
        }
    }

    private static PreviewPayload read(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        String blueprintId = buf.readUtf(128);
        String blueprintName = buf.readUtf(256);
        int width = buf.readVarInt();
        int height = buf.readVarInt();
        int depth = buf.readVarInt();
        BlockPos origin = buf.readBlockPos();
        int rotation = buf.readVarInt();

        int count = buf.readVarInt();
        if (count < 0 || count > 100_000) {
            throw new IllegalArgumentException("Invalid Alexandria's Archives preview block count: " + count);
        }

        BlockPos[] blockPositions = new BlockPos[count];
        String[] blockIds = new String[count];
        for (int i = 0; i < count; i++) {
            blockPositions[i] = buf.readBlockPos();
            blockIds[i] = buf.readUtf(128);
        }

        return new PreviewPayload(
                active,
                blueprintId,
                blueprintName,
                width,
                height,
                depth,
                origin,
                rotation,
                blockPositions,
                blockIds
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static PreviewPayload stop() {
        return new PreviewPayload(
                false,
                "",
                "",
                0,
                0,
                0,
                BlockPos.ZERO,
                0,
                new BlockPos[0],
                new String[0]
        );
    }
}
