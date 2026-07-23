package net.thinkerthonker.alexandriasarchives.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class BuildCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("build").executes(context -> {
            context.getSource().sendSuccess(
                    () -> Component.literal("System to be added :D"),
                    false
            );
            return 1;
        });
    }
}
