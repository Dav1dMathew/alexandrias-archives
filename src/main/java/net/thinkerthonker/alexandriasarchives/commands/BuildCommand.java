package net.thinkerthonker.alexandriasarchives.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.builder.BlueprintValidator;
import net.thinkerthonker.alexandriasarchives.builder.BuildManager;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import net.thinkerthonker.alexandriasarchives.network.PreviewNetworking;
import net.thinkerthonker.alexandriasarchives.service.AiServiceClient;
import net.thinkerthonker.alexandriasarchives.service.LandmarkRegistry;
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
                    () -> Component.literal("Usage: /aa build demo | /aa build <prompt> | /aa build confirm | /aa build cancel | /aa build undo"),
                    false
            );
            return 1;
        }).then(Commands.literal("demo").executes(context -> executeDemo(
                context.getSource())
        )).then(Commands.literal("confirm").executes(context ->
                confirmBuild(context.getSource())
        )).then(Commands.literal("cancel").executes(context ->
                cancelBuild(context.getSource())
        )).then(Commands.literal("undo").executes(context ->
                undoBuild(context.getSource())
        )).then(Commands.argument("prompt", StringArgumentType.greedyString()).executes(context ->
                executeGenerate(context.getSource(), StringArgumentType.getString(context, "prompt"))
        ));
    }

    // Temp function to generate demo structure.
    private static int executeDemo(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must run by a player"));
            return 0;
        }

        if (BuildManager.hasActiveBuildOrPreview(player)) {
            source.sendFailure(Component.literal("You already have an active build or preview."));
            return 0;
        }

        ServerLevel level = source.getLevel();
        BlockPos playerPosition = player.blockPosition();
        BlockPos buildOrigin = playerPosition.relative(player.getDirection(), 5);
        Blueprint blueprint = createDemoBlueprint();

        BlueprintValidator.ValidationResult validation = BlueprintValidator.validate(blueprint);
        if(!validation.valid()) {
            source.sendFailure(Component.literal("Blueprint Validation Failed: " + validation.message()));
            return 0;
        }

        boolean started = BuildManager.startPreview(player, blueprint, buildOrigin);
        if (!started) {
            source.sendFailure(Component.literal("Preview could not be loaded."));
            return 0;
        }

        PreviewNetworking.sendPreview(player, blueprint, buildOrigin, 0);
        source.sendSuccess(() -> Component.literal("Alexandria's Archives: Preview Ready!"), false);
        source.sendSuccess(() -> Component.literal("Controls: R = rotate | Arrow Keys = move | Page Up/Down = vertical move"), false);
        source.sendSuccess(() -> Component.literal("Use /aa build confirm to build."), false);
        source.sendSuccess(() -> Component.literal("Use /aa build cancel to cancel."), false);
        return 1;
    }

    // Sends the prompt to the AI service and builds the returned Blueprint.
    // The HTTP call runs off the server thread; the response is handled back
    // on the server thread via MinecraftServer#execute, since world edits
    // (and sendSuccess/sendFailure) aren't safe to perform off-thread.
    private static int executeGenerate(CommandSourceStack source, String prompt) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must run by a player"));
            return 0;
        }

        if (BuildManager.hasActiveBuildOrPreview(player)) {
            source.sendFailure(Component.literal("You already have an active build or preview."));
            return 0;
        }

        if (prompt == null || prompt.isBlank()) {
            source.sendFailure(Component.literal("Please provide a building prompt."));
            return 0;
        }

        MinecraftServer server = source.getServer();
        BlockPos buildOrigin = player.blockPosition().relative(player.getDirection(), 5);

        source.sendSuccess(
                () -> Component.literal("Sending \"" + prompt + "\" to the AI service..."),
                false
        );

        AI_SERVICE_CLIENT.generateBlueprint(prompt).whenComplete((blueprint, error) -> server.execute(() -> {
            if (error != null) {
                String reason = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                if (reason == null || reason.isBlank()) {
                    reason = "Unknown AI service error";
                }
                source.sendFailure(Component.literal("Failed to generate structure: " + reason));
                return;
            }

            if (blueprint == null) {
                source.sendFailure(Component.literal("The AI service returned no blueprint."));
                return;
            }

            BlueprintValidator.ValidationResult validation = BlueprintValidator.validate(blueprint);
            if (!validation.valid()) {
                source.sendFailure(Component.literal("Blueprint Validation Failed: " + validation.message()));
                return;
            }

            boolean started = BuildManager.startPreview(player, blueprint, buildOrigin);
            if (!started) {
                source.sendFailure(Component.literal("Preview could not be loaded."));
                return;
            }

            PreviewNetworking.sendPreview(player, blueprint, buildOrigin, 0);
            source.sendSuccess(() -> Component.literal("Alexandria's Archives: AI Preview Ready!"), true);
            source.sendSuccess(() -> Component.literal("Controls: R = rotate | Arrow Keys = move | Page Up/Down = vertical move"), false);
            source.sendSuccess(() -> Component.literal("Use /aa build confirm to build."), false);
            source.sendSuccess(() -> Component.literal("Use /aa build cancel to cancel."), false);
        }));

        return 1;
    }

    // Confirms the current preview and starts the actual build job.
    private static int confirmBuild(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must run by a player"));
            return 0;
        }

        boolean confirmed = BuildManager.confirmPreview(player);
        if (!confirmed) {
            source.sendFailure(Component.literal("No active preview available."));
            return 0;
        }

        PreviewNetworking.sendStop(player);
        source.sendSuccess(() -> Component.literal("Alexandria's Archives: Build Confirmed!"), true);
        return 1;
    }

    // Player's active build is cancelled.
    private static int cancelBuild(CommandSourceStack source) {
        ServerPlayer player;
        try{
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must run by a player"));
            return 0;
        }

        if (BuildManager.cancelPreview(player)) {
            PreviewNetworking.sendStop(player);
            source.sendSuccess(() -> Component.literal("Alexandria's Archives: Preview cancelled."), false);
            return 1;
        }

        boolean cancelled = BuildManager.cancelBuild(player);
        if(!cancelled) {
            source.sendFailure(Component.literal("You don't have an active build or preview."));
            return 0;
        }

        PreviewNetworking.sendStop(player);
        source.sendSuccess(() -> Component.literal("Requesting Build Cancellation..."), false);
        return 1;
    }

    private static int executeGenerate(CommandSourceStack source, String prompt) {
        Blueprint landmarkBlueprint = LandmarkRegistry.generate(prompt);
        if(landmarkBlueprint != null) {
            BlueprintValidator.ValidationResult validation = BlueprintValidator.validate(landmarkBlueprint);
            if(!validation.valid()) {
                source.sendFailure(Component.literal("Validation Failed for Blueprint landmark: " + validation.message()));
                return 0;
            }
            BlockPos buildOrigin = player.blockPosition().relative(player.getDirection(), 8);
            boolean started = BuildManager.startPreview(player, landmarkBlueprint, buildOrigin);
            if(!started) {
                source.sendFailure(Component.literal("Preview could not be loaded."));
                return 0;
            }
            PreviewNetworking.sendPreview(player, landmarkBlueprint, buildOrigin, 0);
            source.sendSuccess(() -> Component.literal("Alexandria's Archives: " + landmarkBlueprint.getName() + " preview ready!"), true);
            source.sendSuccess(() -> Component.literal("Use '/aa build confirm' to generate structure."), false);
            return 1;
        }
    }

    // Player uses "Undo" command on previous completed build.
    private static int undoBuild(CommandSourceStack source) {
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must run by a player"));
            return 0;
        }

        boolean undone = BuildManager.undo(player);
        if(!undone) {
            source.sendFailure(Component.literal("No build available to undo."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Alexandria's Archives: Previous build undone."), true);
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
