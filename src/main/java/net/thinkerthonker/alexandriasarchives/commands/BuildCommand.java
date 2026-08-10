package net.thinkerthonker.alexandriasarchives.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.thinkerthonker.alexandriasarchives.builder.BlueprintValidator;
import net.thinkerthonker.alexandriasarchives.builder.StructureBuilder;
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
                    () -> Component.literal("System to be added :D"),
                    false
            );
            return 1;
        }).then(Commands.literal("demo").executes(context -> executeDemo(
                context.getSource())));
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
