package net.thinkerthonker.alexandriasarchives.commands;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class ImageCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("Image").executes(context -> {
            context.getSource().sendSuccess(
                    () -> Component.literal("Timeline Simulation To Be Added!"),
                    false
            );
            return 1;
        });
    }
}