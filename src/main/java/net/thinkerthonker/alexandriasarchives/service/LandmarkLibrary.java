package net.thinkerthonker.alexandriasarchives.service;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Loads the hand-curated landmark models that ship inside the mod.
//
// These are real community builds converted offline by
// tools/convert_landmarks.py, which normalises four different source
// formats (Sponge .schem, pre-1.13 MCEdit .schematic and two superflat
// world saves) into one gzipped NBT layout:
//
//   Id, Name, Width, Height, Length
//   Palette    list of block state strings
//   Positions  int array, packed (y * Length + z) * Width + x
//   States     int array, parallel to Positions, index into Palette
//
// Only non-air cells are stored, so the bounding box costs nothing when a
// structure is mostly empty - the Eiffel Tower's box is 1.6M cells but only
// 34,840 of them are blocks.
public final class LandmarkLibrary {

    private static final String ROOT = "/data/alexandriasarchives/landmarks/";
    private static final Map<String, Blueprint> CACHE = new ConcurrentHashMap<>();

    private LandmarkLibrary() {
        // Utility class.
    }

    /** Loads a landmark by id, or returns null if it is missing or unreadable. */
    public static Blueprint load(String landmarkId) {
        Blueprint cached = CACHE.get(landmarkId);
        if (cached != null) {
            return cached;
        }

        try (InputStream stream = LandmarkLibrary.class.getResourceAsStream(ROOT + landmarkId + ".nbt")) {
            if (stream == null) {
                return null;
            }
            CompoundTag tag = NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
            Blueprint blueprint = decode(tag);
            if (blueprint != null) {
                CACHE.put(landmarkId, blueprint);
            }
            return blueprint;
        } catch (Exception unreadable) {
            return null;
        }
    }

    private static Blueprint decode(CompoundTag tag) {
        int width = tag.getInt("Width");
        int height = tag.getInt("Height");
        int length = tag.getInt("Length");
        if (width <= 0 || height <= 0 || length <= 0) {
            return null;
        }

        ListTag paletteTag = tag.getList("Palette", Tag.TAG_STRING);
        String[] palette = new String[paletteTag.size()];
        for (int i = 0; i < paletteTag.size(); i++) {
            palette[i] = paletteTag.getString(i);
        }

        int[] positions = tag.getIntArray("Positions");
        int[] states = tag.getIntArray("States");
        int count = Math.min(positions.length, states.length);

        List<BlueprintBlock> blocks = new ArrayList<>(count);
        int layer = width * length;
        for (int i = 0; i < count; i++) {
            int stateIndex = states[i];
            if (stateIndex < 0 || stateIndex >= palette.length) {
                continue;
            }
            int packed = positions[i];
            int y = packed / layer;
            int remainder = packed - y * layer;
            int z = remainder / width;
            int x = remainder - z * width;
            blocks.add(new BlueprintBlock(new BlockPos(x, y, z), palette[stateIndex]));
        }

        return new Blueprint(
                tag.getString("Id"),
                tag.getString("Name"),
                width, height, length,
                blocks
        );
    }
}
