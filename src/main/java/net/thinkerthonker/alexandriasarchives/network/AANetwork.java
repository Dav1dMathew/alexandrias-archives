package net.thinkerthonker.alexandriasarchives.network;

import net.minecraft.resources.ResourceLocation;
import net.thinkerthonker.alexandriasarchives.AlexandriaSArchives;

// This is the central location for all of Alexandria's Archives networking identifiers.
// It tells the client to start previewing the bluepirnt whilst leaving the server authoritative
// over modifying the actual world in any way.
public final class AANetwork {
    // Construction preview started using Packet identifier.
    public static final ResourceLocation START_PREVIEW = AlexandriaSArchives.id("start_preview");
    public static final ResourceLocation STOP_PREVIEW = AlexandriaSArchives.id("stop_preview");
    private AANetwork() {
        // Utility Class.
    }
}