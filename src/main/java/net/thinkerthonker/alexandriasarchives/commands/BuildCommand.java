package net.thinkerthonker.alexandriasarchives.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.builder.BlueprintValidator;
import net.thinkerthonker.alexandriasarchives.builder.StructureBuilder;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import net.thinkerthonker.alexandriasarchives.service.AiServiceClient;
import java.util.ArrayList;
import java.util.List;

// Handles /aa build command.
// "/aa build demo" places a hard-coded structure so the pipeline can be
// verified without the AI service running.
// "/aa build <prompt>" sends the prompt to the Python AI service's
// POST /generate endpoint (Tier 0: keyword-parsed procedural generation)
// and builds whatever Blueprint comes back.
public class BuildCommand {

    private static final AiServiceClient AI_SERVICE_CLIENT = new AiServiceClient();

    private BuildCommand() {
        // Utility Class.
    }

    // Registers build command under /aa.
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("build").executes(context -> {
            context.getSource().sendSuccess(
                    () -> Component.literal("Usage: /aa build demo | /aa build <prompt>"),
                    false
            );
            return 1;
        }).then(Commands.literal("demo").executes(context -> executeDemo(
                context.getSource())
        )).then(Commands.argument("prompt", StringArgumentType.greedyString()).executes(context ->
                executeGenerate(context.getSource(), StringArgumentType.getString(context, "prompt"))
        ));
    }

    // Temp function to generate demo structure.
    private static int executeDemo(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos playerPosition = BlockPos.containing(source.getPosition());

        // Puts structure slightly in front of or above the player's position.
        BlockPos buildOrigin = playerPosition.above();
        Blueprint blueprint = createDemoBlueprint();

        BlueprintValidator.ValidationResult validation = BlueprintValidator.validate(blueprint);
        if(!validation.valid()) {
            source.sendFailure(Component.literal("Blueprint Validation Failed: " + validation.message()));
            return 0;
        }

        StructureBuilder.placeBlock(level, buildOrigin, blueprint);
        source.sendSuccess(() -> Component.literal("Alexandria's Archives built " + blueprint.getName() +
                " containing" + blueprint.getBlockCount() + " blocks."), true);
        return 1;
    }

    // Sends the prompt to the AI service and builds the returned Blueprint.
    // The HTTP call runs off the server thread; the response is handled back
    // on the server thread via MinecraftServer#execute, since world edits
    // (and sendSuccess/sendFailure) aren't safe to perform off-thread.
    private static int executeGenerate(CommandSourceStack source, String prompt) {
        ServerLevel level = source.getLevel();
        MinecraftServer server = source.getServer();
        BlockPos buildOrigin = BlockPos.containing(source.getPosition()).above();

        source.sendSuccess(
                () -> Component.literal("Sending \"" + prompt + "\" to the AI service..."),
                false
        );

        AI_SERVICE_CLIENT.generateBlueprint(prompt).whenComplete((blueprint, error) -> server.execute(() -> {
            if (error != null) {
                String reason = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                source.sendFailure(Component.literal("Failed to generate structure: " + reason));
                return;
            }

            BlueprintValidator.ValidationResult validation = BlueprintValidator.validate(blueprint);
            if (!validation.valid()) {
                source.sendFailure(Component.literal("Blueprint Validation Failed: " + validation.message()));
                return;
            }

            StructureBuilder.placeBlock(level, buildOrigin, blueprint);
            source.sendSuccess(() -> Component.literal("Alexandria's Archives built " + blueprint.getName() +
                    " containing " + blueprint.getBlockCount() + " blocks."), true);
        }));

        return 1;
    }

    // Demo Function hard-coded for Blueprint.
    // This function will later be replaced with a system
    // that allows the blueprint to be received from the Python Reconstruction System.
    private static Blueprint createDemoBlueprint() {
        List<BlueprintBlock> blocks = new ArrayList<>();
        // Floor
        for(int x = 0; x < 7; x++) {
            for(int z = 0; z < 7; z++) {
                blocks.add(new BlueprintBlock(new BlockPos(x, 0, z), "minecraft:stone"));
            }
        }
        // Four Walls.
        for(int y = 1; y < 4; y++) {
            for(int x = 0; x < 7; x++) {
                blocks.add(new BlueprintBlock(new BlockPos(x, y, 0), "minecraft:stone_bricks"));
                blocks.add(new BlueprintBlock(new BlockPos(x, y, 6), "minecraft:stone_bricks"));
            }
            for(int z = 1; z < 6; z++) {
                blocks.add(new BlueprintBlock(new BlockPos(0, y, z), "minecraft:stone_bricks"));
                blocks.add(new BlueprintBlock(new BlockPos(6, y, z), "minecraft:stone_bricks"));
            }
        }
        // Wooden Roof.
        for(int x = 0; x < 7; x++) {
            for(int z = 0; z < 7; z++) {
                blocks.add(new BlueprintBlock(new BlockPos(x, 4, z), "minecraft:oak_planks"));
            }
        }
        // Simple Doorway -> Replace blocks with air to create an entrance.
        blocks.removeIf(block -> block.getPosition().getY() >= 1
            && block.getPosition().getY() <= 2
                && block.getPosition().getX() == 3
                && block.getPosition().getZ() == 0
        );

        // Windows.
        blocks.add(new BlueprintBlock(new BlockPos(0, 2, 3), "minecraft:glass"));
        return new Blueprint("week1_demo", "Alexandria's Archives Demo Structure",
                7, 5, 7, blocks);
    }
}
