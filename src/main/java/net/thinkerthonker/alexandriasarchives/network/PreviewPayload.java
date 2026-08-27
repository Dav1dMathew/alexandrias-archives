package net.thinkerthonker.alexandriasarchives.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.thinkerthonker.alexandriasarchives.AlexandriaSArchives;

public record PreviewPayload(boolean active, String blueprintID, String blueprintName, int width, int height, int depth,
                             BlockPos origin, int rotation, BlockPos[] blockPositions, String[] blockIDs)
        implements CustomPacketPayload {
    public static final int MAX_BLOCKS = 100_000;
    public static final int MAX_DIMENSIONS = 512;
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(AlexandriaSArchives.MOD_ID,
            "preview");
    public static final Type<PreviewPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<FriendlyByteBuf, PreviewPayload> CODEC = StreamCodec.of(PreviewPayload::write,
            PreviewPayload::read);
    public static void write(FriendlyByteBuf buf, PreviewPayload payload) {
        buf.writeBoolean(payload.active());
        if(!payload.active()) {
            return;
        }
        buf.writeUtf(payload.blueprintID(), 128);
        buf.writeUtf(payload.blueprintName(), 256);
        buf.writeInt(payload.width());
        buf.writeInt(payload.height());
        buf.writeInt(payload.depth());
        buf.writeBlockPos(payload.origin());
        buf.writeVarInt(Math.floorMod(payload.rotation(), 4));
        int count = Math.min(payload.blockPositions().length, payload.blockIDs().length);
        if(count > MAX_BLOCKS) {
            throw new IllegalArgumentException("Preview contains too many blocks: " + count);
        }
        buf.writeVarInt(count);
        for(int i = 0; i < count; i++) {
            buf.writeBlockPos(payload.blockPositions()[i]);
            buf.writeUtf(payload.blockIDs()[i], 128);
        }
    }
    private static PreviewPayload read(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        if(!active) {
            return stop();
        }
        String blueprintID = buf.readUtf(128);
        String blueprintName = buf.readUtf(256);
        int width = readDimension(buf, "width");
        int height = readDimension(buf, "height");
        int depth = readDimension(buf, "depth");
        BlockPos origin = buf.readBlockPos();
        int rotation = Math.floorMod(buf.readVarInt(), 4);
        int count = buf.readVarInt();
        if(count < 0 || count > MAX_BLOCKS) {
            throw new IllegalArgumentException("Preview contains too many blocks: " + count);
        }
        BlockPos[] positions = new BlockPos[count];
        String[] IDs = new String[count];
        for(int i = 0; i < count; i++) {
            positions[i] = buf.readBlockPos();
            IDs[i] = buf.readUtf(128);
        }
        return new PreviewPayload(true, blueprintID, blueprintName, width, height, depth,
                origin, rotation, positions, IDs);
    }
    private static int readDimension(FriendlyByteBuf buf, String name) {
        int value = buf.readVarInt();
        if(value < 0 || value > MAX_DIMENSIONS) {
            throw new IllegalArgumentException("Invalid Preview " + name + ": " + value);
        }
        return value;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
    public static PreviewPayload stop() {
        return new PreviewPayload(false, "", "", 0, 0, 0,
                BlockPos.ZERO, 0, new BlockPos[0], new String[0]);
    }
}
