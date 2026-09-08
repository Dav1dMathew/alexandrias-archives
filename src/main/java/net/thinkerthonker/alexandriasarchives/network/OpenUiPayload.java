package net.thinkerthonker.alexandriasarchives.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.thinkerthonker.alexandriasarchives.AlexandriaSArchives;

// Server -> client nudge to open one of the mod's screens.
//
// Commands only run server-side, so "/aa" cannot open a client screen by
// itself. Everything the screens then do travels back as ordinary chat
// commands, which keeps the server authoritative and means the GUI is a
// pure convenience layer over the text interface rather than a parallel
// code path.
public record OpenUiPayload(String screen) implements CustomPacketPayload {

    public static final String WELCOME = "welcome";
    public static final String PROMPT = "prompt";
    public static final String PREVIEW = "preview";

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(AlexandriaSArchives.MOD_ID, "open_ui");
    public static final Type<OpenUiPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, OpenUiPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.screen(), 32),
            buf -> new OpenUiPayload(buf.readUtf(32))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
