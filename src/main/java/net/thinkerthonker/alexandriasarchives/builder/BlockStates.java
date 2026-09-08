package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Turns a textual block state into a real BlockState.
//
// Blueprint blocks are stored as the string Minecraft itself uses -
// "minecraft:stone_brick_stairs[facing=north,half=bottom]". Dropping the
// bracketed part and taking defaultBlockState() would leave every stair,
// slab and pillar in a landmark facing the same way; the Leaning Tower
// alone carries 223 distinct states, so the properties are most of what
// makes it look like the building.
//
// Parsing is cached because a large landmark places hundreds of thousands
// of blocks but only ever uses a couple of hundred distinct states.
public final class BlockStates {

    private static final Map<String, BlockState> CACHE = new ConcurrentHashMap<>();

    private BlockStates() {
        // Utility class.
    }

    /** Returns null when the block id is not in the registry. */
    public static BlockState parse(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        BlockState cached = CACHE.get(description);
        if (cached != null) {
            return cached;
        }

        String name = description;
        String properties = null;
        int bracket = description.indexOf('[');
        if (bracket >= 0 && description.endsWith("]")) {
            name = description.substring(0, bracket);
            properties = description.substring(bracket + 1, description.length() - 1);
        }

        ResourceLocation id;
        try {
            id = ResourceLocation.parse(name);
        } catch (Exception malformed) {
            return null;
        }
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            return null;
        }

        Block block = BuiltInRegistries.BLOCK.get(id);
        BlockState state = block.defaultBlockState();
        if (properties != null && !properties.isEmpty()) {
            for (String pair : properties.split(",")) {
                int equals = pair.indexOf('=');
                if (equals <= 0) {
                    continue;
                }
                Property<?> property = block.getStateDefinition()
                        .getProperty(pair.substring(0, equals).trim());
                if (property != null) {
                    state = with(state, property, pair.substring(equals + 1).trim());
                }
            }
        }

        CACHE.put(description, state);
        return state;
    }

    /** Strips any bracketed properties, leaving a plain block id. */
    public static String blockId(String description) {
        if (description == null) {
            return null;
        }
        int bracket = description.indexOf('[');
        return bracket < 0 ? description : description.substring(0, bracket);
    }

    // Separate method so the property's value type is captured; a property
    // read out of a StateDefinition is only known as Property<?>.
    private static <T extends Comparable<T>> BlockState with(
            BlockState state, Property<T> property, String value) {
        return property.getValue(value)
                .map(parsed -> state.setValue(property, parsed))
                .orElse(state);
    }
}
