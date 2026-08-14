package net.thinkerthonker.alexandriasarchives.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.builder.BlueprintValidator;
import net.thinkerthonker.alexandriasarchives.builder.BuildManager;
import net.thinkerthonker.alexandriasarchives.model.Blueprint;
import net.thinkerthonker.alexandriasarchives.model.BlueprintBlock;
import java.util.ArrayList;
import java.util.List;

// Handles /aa build command.
// This class generates a hard-coded structure allowing the program to read user input
// through their Minecraft Client, retrieve tbe Blueprint through the SpringBoot
// preview the build before fully generating the structure in the world.
public class BuildCommand {

    private BuildCommand() {
        // Utility Class.
    }

    // Registers build command under /aa.
    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("build").executes(context -> {
            context.getSource().sendSuccess(
                    () -> Component.literal("Usage: /aa build demo | /aa build cancel | /aa build undo"),false);
            return 1;
        }).then(Commands.literal("demo").executes(context -> executeDemo(
                context.getSource())))

                // /aa build cancel
                .then(Commands.literal("cancel").executes(context -> cancelBuild(
                        context.getSource())))
                // /aa build undo
                .then(Commands.literal("undo").executes(context -> undoBuild(
                        context.getSource())));
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

        if (BuildManager.isBuilding(player)) {
            source.sendFailure(Component.literal("You are already building something."));
            return 0;
        }

        Blueprint blueprint = createDemoBlueprint();
        BlueprintValidator.ValidationResult validation = BlueprintValidator.validate(blueprint);
        if (!validation.valid()) {
            source.sendFailure(Component.literal("Blueprint Validation Failed: " + validation.message()));
            return 0;
        }

        // Places structure several blocks in front of player so it doesn't immediately overlap them.
        BlockPos playerPosition = player.blockPosition();
        BlockPos buildOrigin = playerPosition.relative(player.getDirection(), 5);
        boolean started = BuildManager.startBuild(player, blueprint, buildOrigin);
        if (!started) {
            source.sendFailure(Component.literal("Build could not be started."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Alexandria's Archives: Building " +
                blueprint.getName() + "..."), true);
        return 1;
    }

//        StructureBuilder.placeBlock(level, buildOrigin, blueprint);
//        source.sendSuccess(() -> Component.literal("Alexandria's Archives built " + blueprint.getName() +
//                " containing" + blueprint.getBlockCount() + " blocks."), true);
//        return 1;
//    }

    // Player's active build is cancelled.
    private static int cancelBuild(CommandSourceStack source) {
        ServerPlayer player;
        try{
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            source.sendFailure(Component.literal("This command must run by a player"));
            return 0;
        }

        boolean cancelled = BuildManager.cancelBuild(player);
        if(!cancelled) {
            source.sendFailure(Component.literal("You don't have an active build."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal("Requesting Build Cancellation..."), false);
        return 1;
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
    // This function will later be replaced with a system that allows the blueprint
    // to be received from the Python Reconstruction System.
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
        return new Blueprint("week2_demo", "Alexandria's Archives Demo Structure",
                7, 5, 7, blocks);
    }
}
