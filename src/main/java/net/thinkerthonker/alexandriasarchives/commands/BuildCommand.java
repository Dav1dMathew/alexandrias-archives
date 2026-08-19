package net.thinkerthonker.alexandriasarchives.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.block.Blocks;
import net.thinkerthonker.alexandriasarchives.builder.BlueprintValidator;
import net.thinkerthonker.alexandriasarchives.builder.StructureBuilder;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import net.thinkerthonker.alexandriasarchives.service.AiServiceClient;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

        clearArea(level, buildOrigin, blueprint);
        StructureBuilder.placeBlock(level, buildOrigin, blueprint);
        source.sendSuccess(() -> Component.literal("Alexandria's Archives built " + blueprint.getName() +
                " containing" + blueprint.getBlockCount() + " blocks."), true);
        return 1;
    }

    // Sends the prompt to the AI service and builds the returned Blueprint.
    // The HTTP call runs off the server thread; the response is handled back
    // on the server thread via MinecraftServer#execute, since world edits
    // (and sendSuccess/sendFailure) aren't safe to perform off-thread.
    //
    // A boss bar tracks progress through the request's stages (sending ->
    // waiting on the AI -> validating -> clearing -> placing -> done),
    // since the whole round trip can take anywhere from ~2s (Groq, a
    // simple archetype) to ~90s worst case (a chained OSM+LLM fallback) -
    // a static "Sending..." chat line gives no sense of whether anything
    // is still happening. The player-facing bar cannot show true
    // per-block progress during placement (StructureBuilder.placeBlock
    // is one synchronous call, not tick-batched like BuildJob), so
    // stages are the honest granularity available, not a smooth percentage.
    private static int executeGenerate(CommandSourceStack source, String prompt) {
        ServerLevel level = source.getLevel();
        MinecraftServer server = source.getServer();
        BlockPos buildOrigin = BlockPos.containing(source.getPosition()).above();
        ServerPlayer player = source.getPlayer();

        ServerBossEvent bar = null;
        if (player != null) {
            bar = new ServerBossEvent(
                    Component.literal("Alexandria's Archives: contacting AI..."),
                    BossEvent.BossBarColor.YELLOW,
                    BossEvent.BossBarOverlay.PROGRESS
            );
            bar.setProgress(0.1f);
            bar.addPlayer(player);
        }
        final ServerBossEvent progressBar = bar;

        source.sendSuccess(
                () -> Component.literal("Sending \"" + prompt + "\" to the AI service..."),
                false
        );

        AI_SERVICE_CLIENT.generateBlueprint(prompt).whenComplete((blueprint, error) -> server.execute(() -> {
            if (error != null) {
                String reason = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
                source.sendFailure(Component.literal("Failed to generate structure: " + reason));
                failBar(progressBar, player, "Failed: " + reason);
                return;
            }

            updateBar(progressBar, "Validating blueprint...", 0.5f, BossEvent.BossBarColor.YELLOW);
            BlueprintValidator.ValidationResult validation = BlueprintValidator.validate(blueprint);
            if (!validation.valid()) {
                source.sendFailure(Component.literal("Blueprint Validation Failed: " + validation.message()));
                failBar(progressBar, player, "Validation failed: " + validation.message());
                return;
            }

            updateBar(progressBar, "Clearing build area...", 0.7f, BossEvent.BossBarColor.YELLOW);
            clearArea(level, buildOrigin, blueprint);

            updateBar(progressBar, "Placing " + blueprint.getBlockCount() + " blocks...", 0.9f, BossEvent.BossBarColor.YELLOW);
            StructureBuilder.placeBlock(level, buildOrigin, blueprint);

            source.sendSuccess(() -> Component.literal("Alexandria's Archives built " + blueprint.getName() +
                    " containing " + blueprint.getBlockCount() + " blocks."), true);
            completeBar(progressBar, player);
        }));

        return 1;
    }

    // Advances the bar to a new stage - null-safe so callers don't need
    // their own "if bar != null" guard at every step.
    private static void updateBar(ServerBossEvent bar, String title, float progress, BossEvent.BossBarColor color) {
        if (bar == null) {
            return;
        }
        bar.setName(Component.literal("Alexandria's Archives: " + title));
        bar.setProgress(progress);
        bar.setColor(color);
    }

    // Turns the bar red with the failure reason, then removes it shortly
    // after so the player actually gets to read why before it vanishes.
    private static void failBar(ServerBossEvent bar, ServerPlayer player, String message) {
        if (bar == null) {
            return;
        }
        bar.setName(Component.literal("Alexandria's Archives: " + message));
        bar.setColor(BossEvent.BossBarColor.RED);
        bar.setProgress(1.0f);
        scheduleRemoval(bar, player, 3000);
    }

    // Shows a brief green "complete" state before removing the bar,
    // rather than having it disappear the instant placement finishes.
    private static void completeBar(ServerBossEvent bar, ServerPlayer player) {
        if (bar == null) {
            return;
        }
        bar.setName(Component.literal("Alexandria's Archives: Build complete!"));
        bar.setColor(BossEvent.BossBarColor.GREEN);
        bar.setProgress(1.0f);
        scheduleRemoval(bar, player, 1500);
    }

    private static void scheduleRemoval(ServerBossEvent bar, ServerPlayer player, long delayMillis) {
        CompletableFuture.runAsync(() -> {}, CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS))
                .thenRun(() -> player.getServer().execute(() -> bar.removePlayer(player)));
    }

    // Clears a volume around the build origin before placing, sized to the
    // incoming blueprint plus a margin. Without this, repeated /aa build
    // calls at (roughly) the same spot only overwrite the positions the
    // *new* blueprint actually touches - anything from a previous,
    // differently-shaped build outside that footprint is left behind as a
    // disconnected leftover fragment. Synchronous, so a very large build
    // (e.g. a real-world footprint reconstruction) may cause a brief
    // pause; acceptable for how infrequently those are built.
    private static void clearArea(ServerLevel level, BlockPos origin, Blueprint blueprint) {
        int margin = 5;
        int maxSize = Math.max(blueprint.getWidth(),
                Math.max(blueprint.getHeight(), blueprint.getDepth())) + margin;

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -margin; x < maxSize; x++) {
            for (int y = -margin; y < maxSize; y++) {
                for (int z = -margin; z < maxSize; z++) {
                    cursor.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
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
