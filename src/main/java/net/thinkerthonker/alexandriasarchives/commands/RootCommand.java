package net.thinkerthonker.alexandriasarchives.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.thinkerthonker.alexandriasarchives.network.OpenUiPayload;
import net.thinkerthonker.alexandriasarchives.network.PreviewNetworking;

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
                        // "/aa" on its own opens the welcome screen, which is
                        // where a player picks between the chat commands and
                        // the guided visual flow.
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null) {
                                context.getSource().sendSuccess(
                                        () -> Component.literal("Alexandria's Archives is loaded."), false);
                                return 1;
                            }
                            PreviewNetworking.sendOpenUi(player, OpenUiPayload.WELCOME);
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