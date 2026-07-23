package net.thinkerthonker.alexandriasarchives.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class PhotoCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        return Commands.literal("Photo").executes(context -> {
            context.getSource().sendSuccess(
                    () -> Component.literal("Photo Mode To Be Added!"),
                    false
            );
            return 1;
        });
    }
}