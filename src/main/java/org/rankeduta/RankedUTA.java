package org.rankeduta;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.rankeduta.command.Command;
import org.rankeduta.command.Event;
import org.rankeduta.define.ServerRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class RankedUTA implements ModInitializer {
	public static final String MOD_ID = "Ranked UTA";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final UUID SERVER_UUID = UUID.randomUUID();

	public static final String PROPERTY_PATH = "server.properties";

	public static ServerRole serverRole = ServerRole.UNKNOWN;
	public static String apiUrl = "";

	@Override
	public void onInitialize() {
		// Initialize the mod
		LOGGER.info("Ranked UTA mod is initializing...");

		// Load properties and configurations
		loadProperties();

		// Register events and commands
		registerEvents();
		registerCommands();

		LOGGER.info("Ranked UTA mod initialized successfully.");
	}

	private void loadProperties() {
		// Load server properties from a file
		LOGGER.info("Loading server properties...");
		// Implementation for loading properties
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(PROPERTY_PATH));

			if (!properties.containsKey("server-role")) {
				try (FileOutputStream out = new FileOutputStream(PROPERTY_PATH, true)) {
					properties.setProperty("server-role", ServerRole.UNKNOWN.getRole());
					properties.store(out, "Server Role Property");
				} catch (IOException e) {
					LOGGER.error("Failed to create default server properties file at {}", PROPERTY_PATH, e);
				}
			}

			if (!properties.containsKey("api-url")) {
				try (FileOutputStream out = new FileOutputStream(PROPERTY_PATH, true)) {
					properties.setProperty("api-url", "");
					properties.store(out, "API URL Property");
				} catch (IOException e) {
					LOGGER.error("Failed to create default server properties file at {}", PROPERTY_PATH, e);
				}
			}

			apiUrl = properties.getProperty("api-url", "");
			serverRole = ServerRole.fromString(properties.getProperty("server-role", ServerRole.UNKNOWN.getRole()));
			LOGGER.info("Server properties loaded: apiUrl={}, serverRole={}", apiUrl, serverRole);
		} catch (IOException e) {
			LOGGER.error("Failed to load server properties from '{}'", PROPERTY_PATH, e);
		}
	}

	private void registerEvents() {
		LOGGER.debug("Registering events...");
		ServerLifecycleEvents.SERVER_STARTED.register(Event::ServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(Event::ServerStopping);
		ServerPlayConnectionEvents.JOIN.register(Event::PlayerJoin);
		ServerPlayConnectionEvents.DISCONNECT.register(Event::PlayerLeave);
	}

	private void registerCommands() {
		LOGGER.debug("Registering commands...");
		Command.register();
	}
}