package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;

// Blueprints are validated before user can modify the Minecraft World.
// Future Version Amendments:
// * Permission Checks
// * Chunk Validators
// * Block Allowlists
// * NBT Validation
// * Anti-Abuse Limits
// * Multiplayer Security

public final class BlueprintValidator {
    // Maximum dimensions permissible
    public static final int MAX_WIDTH = 64;
    public static final int MAX_HEIGHT = 64;
    public static final int MAX_DEPTH = 64;


    // Maximum amount of blocks in one build.
    public static final int MAX_BLOCKS = 50_000;
    private BlueprintValidator() {
        // Utility class - no instances required.
    }

    // Validates a blueprint.
    // @param blueprint to validate
    // @param validation result
    public static ValidationResult validate(Blueprint blueprint) {
        if (blueprint == null) {
            return ValidationResult.failure("Blueprint is null.");
        }

        if (blueprint.getWidth() <= 0 || blueprint.getHeight() <= 0 || blueprint.getDepth() <= 0) {
            return ValidationResult.failure("Blueprint dimensions must be greater than 0.");
        }

        if(blueprint.getWidth() > MAX_WIDTH || blueprint.getHeight() > MAX_HEIGHT ||  blueprint.getDepth() > MAX_DEPTH) {
            return ValidationResult.failure("Blueprint exceeds the maximum allowed dimensions.");
        }

        if(blueprint.getBlockCount() > MAX_BLOCKS) {
            return ValidationResult.failure("Blueprint contains too many blocks.");
        }

        for(BlueprintBlock blueprintBlock: blueprint.getBlocks()) {
            if(!blueprint.containsPosition(
                    blueprintBlock.getPosition())) {
                return ValidationResult.failure("Block exists outside blueprint bounds at " + blueprintBlock.getPosition());
            }
            ResourceLocation blockID;
            try {
                blockID = ResourceLocation.parse(
                        blueprintBlock.getBlockID()
                );
            } catch (Exception exception) {
                return ValidationResult.failure("Invalid block identifier: " + blueprintBlock.getBlockID());
            }
            if(!BuiltInRegistries.BLOCK.containsKey(blockID)) {
                return ValidationResult.failure("Unknown Minecraft Block: " + blueprintBlock.getBlockID());
            }
        }
        return ValidationResult.success();
    }

    // Communication of validation outcome handled via a small result object.
    public record ValidationResult(
            boolean valid,
            String message
    ) {
        public static ValidationResult success() {
            return new ValidationResult(true, "Valid.");
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }
}
