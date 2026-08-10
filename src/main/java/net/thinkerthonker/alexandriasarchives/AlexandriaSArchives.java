package net.thinkerthonker.alexandriasarchives;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.resources.ResourceLocation;
import net.thinkerthonker.alexandriasarchives.commands.RootCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Main point of entry for Alexandria's Archives.
// Initializes the mod and registers the command system.
public class AlexandriaSArchives implements ModInitializer {
	public static final String MOD_ID = "alexandriasarchives";
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {

		LOGGER.info("===========================================");
		LOGGER.info("Initializing Alexandria's Archives...");
		LOGGER.info("Version 0.1.0");
		LOGGER.info("Alexandria's Archives Minecraft Mod");
		LOGGER.info("===========================================");

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess,
				 environment) -> RootCommand.register(dispatcher)
		);

		LOGGER.info("Commands Registered Successfully.");
		LOGGER.info("Alexandria's Archives Successfully Loaded!");
	}

	// Creates Resource Location using the Alexandria's Archives namespace.
	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
