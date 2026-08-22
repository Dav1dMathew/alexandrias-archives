package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;

import java.util.HashSet;
import java.util.Set;

// Blueprints are validated before they can modify the Minecraft world.
public final class BlueprintValidator {
    public static final int MAX_WIDTH = 64;
    public static final int MAX_HEIGHT = 64;
    public static final int MAX_DEPTH = 64;
    public static final int MAX_BLOCKS = 50_000;

    private BlueprintValidator() {
        // Utility class - no instances required.
    }

    public static ValidationResult validate(Blueprint blueprint) {
        if (blueprint == null) {
            return ValidationResult.failure("Blueprint is null.");
        }
        if (blueprint.getWidth() <= 0 || blueprint.getHeight() <= 0 || blueprint.getDepth() <= 0) {
            return ValidationResult.failure("Blueprint dimensions must be greater than 0.");
        }
        if (blueprint.getWidth() > MAX_WIDTH
                || blueprint.getHeight() > MAX_HEIGHT
                || blueprint.getDepth() > MAX_DEPTH) {
            return ValidationResult.failure("Blueprint exceeds the maximum allowed dimensions.");
        }
        if (blueprint.getBlocks() == null) {
            return ValidationResult.failure("Blueprint block list is null.");
        }
        if (blueprint.getBlockCount() > MAX_BLOCKS) {
            return ValidationResult.failure("Blueprint contains too many blocks.");
        }

        Set<BlockPos> occupiedPositions = new HashSet<>();
        for (BlueprintBlock block : blueprint.getBlocks()) {
            if (block == null) {
                return ValidationResult.failure("Blueprint contains a null block.");
            }
            BlockPos position = block.getPosition();
            if (position == null) {
                return ValidationResult.failure("Blueprint block position is null.");
            }
            if (!blueprint.containsPosition(position)) {
                return ValidationResult.failure("Block exists outside blueprint bounds at " + position);
            }
            if (!occupiedPositions.add(position)) {
                return ValidationResult.failure("Duplicate block position at " + position);
            }
            if (block.getBlockID() == null) {
                return ValidationResult.failure("Blueprint block identifier is null.");
            }

            ResourceLocation blockId;
            try {
                blockId = ResourceLocation.parse(block.getBlockID());
            } catch (Exception exception) {
                return ValidationResult.failure("Invalid block identifier: " + block.getBlockID());
            }
            if (!BuiltInRegistries.BLOCK.containsKey(blockId)) {
                return ValidationResult.failure("Unknown Minecraft Block: " + block.getBlockID());
            }
        }
        return ValidationResult.success();
    }

    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, "Valid.");
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }
}
