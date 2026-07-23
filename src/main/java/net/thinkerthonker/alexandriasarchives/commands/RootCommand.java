package net.thinkerthonker.alexandriasarchives.commands;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * ============================================================================
 * Alexandria's Archives
 * RootCommand.java
 * ----------------------------------------------------------------------------
 * Registers the root "/aa" command and all Alexandria's Archives subcommands.
 *
 * Available Commands:
 *   /aa
 *   /aa build
 *   /aa image
 *   /aa history
 *   /aa photo
 *
 * Future subcommands can simply be added using another .then(...)
 *
 * Author: Alexandria's Archives Team
 * ============================================================================
 */
public final class RootCommand {

    /**
     * Private constructor prevents instantiation.
     * This class only contains static utility methods.
     */
    private RootCommand() {
    }

    /**
     * Registers the Alexandria's Archives root command.
     *
     * @param dispatcher Minecraft's Brigadier command dispatcher.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("aa")

                        // Executes when the player types "/aa"
                        .executes(context -> {

                            context.getSource().sendSuccess(
                                    () -> Component.literal("Alexandria's Archives Successfully Loaded!"),
                                    false
                            );

                            return 1;
                        })

                        // Register subcommands
                        .then(BuildCommand.register())
                        .then(ImageCommand.register())
                        .then(HistoryCommand.register())
                        .then(PhotoCommand.register())
        );
    }
}