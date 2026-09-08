package net.thinkerthonker.alexandriasarchives.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import java.util.HashSet;
import java.util.Set;

// Blueprints are validated before user can modify the Minecraft World.
// Future blueprints will originate from the Python AI system, allowing this class
// to act as a mediator between systems.
// Future Version Amendments:
// * Permission Checks
// * Chunk Validators
// * Block Allowlists
// * NBT Validation
// * Anti-Abuse Limits
// * Multiplayer Security

public final class BlueprintValidator {
    // Maximum dimensions permissible
    public static final int MAX_WIDTH = 128;
    public static final int MAX_HEIGHT = 128;
    public static final int MAX_DEPTH = 128;


    // Maximum amount of blocks in one build.
    public static final int MAX_BLOCKS = 100_000;
    private BlueprintValidator() {
        // Utility class - no instances required.
    }

    // Validates a blueprint.
    // @param blueprint to validate
    // @param validation result
    // Curated landmark models bypass the AI-oriented size caps.
    public static ValidationResult validateLandmark(Blueprint blueprint) {
        return validate(blueprint, true);
    }

    public static ValidationResult validate(Blueprint blueprint) {
        return validate(blueprint, false);
    }

    private static ValidationResult validate(Blueprint blueprint, boolean trusted) {
        if(blueprint == null) {
            return ValidationResult.failure("Blueprint is null.");
        }

        if(blueprint.getID() == null || blueprint.getID().isBlank()) {
            return ValidationResult.failure("Blueprint ID is missing.");
        }

        if(blueprint.getName() == null || blueprint.getName().isBlank()) {
            return ValidationResult.failure("Blueprint name is missing.");
        }

        if(blueprint.getWidth() <= 0 || blueprint.getHeight() <= 0 || blueprint.getDepth() <= 0) {
            return ValidationResult.failure("Blueprint dimensions must be greater than 0.");
        }

        // The size caps guard against a hallucinated blueprint from the AI
        // service asking for something absurd. The bundled landmarks are
        // curated, converted offline and checked in, so they are exempt -
        // the 1:1 Burj Khalifa is 179x831x169 and 326,630 blocks, all of it
        // deliberate.
        if(!trusted) {
            if(blueprint.getWidth() > MAX_WIDTH || blueprint.getHeight() > MAX_HEIGHT ||  blueprint.getDepth() > MAX_DEPTH) {
                return ValidationResult.failure("Blueprint exceeds the maximum allowed dimensions.");
            }

            if(blueprint.getBlockCount() > MAX_BLOCKS) {
                return ValidationResult.failure("Blueprint contains too many blocks.");
            }
        }

        Set<BlockPos> occupiedPos =  new HashSet<>();
        for(BlueprintBlock block: blueprint.getBlocks()) {
            if(block == null) {
                return ValidationResult.failure("Blueprint contains a null block.");
            }
            BlockPos position = block.getPosition();
            if(!blueprint.containsPosition(position)) {
                return ValidationResult.failure("Block exists outside blueprint bounds at " + position);
            }

            // Prevents same coordinate from being occupied by multiple blueprint entries.
            if(!occupiedPos.add(position)) {
                return ValidationResult.failure("Duplicate Block Position: " + position);
            }

            ResourceLocation blockID;
            try {
                blockID = ResourceLocation.parse(BlockStates.blockId(block.getBlockID()));
            }
            catch (Exception exception) {
                return ValidationResult.failure("Invalid block ID: " + block.getBlockID());
            }

            if(!BuiltInRegistries.BLOCK.containsKey(blockID)) {
                return ValidationResult.failure("Unknown Minecraft Block: " + block.getBlockID());
            }
        }
        return ValidationResult.success();
    }

    // Communication of validation outcome handled via a small result object.
    public record ValidationResult(boolean valid, String message) {
        public static ValidationResult success() {
            return new ValidationResult(true, "Valid.");
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }
    }
}
