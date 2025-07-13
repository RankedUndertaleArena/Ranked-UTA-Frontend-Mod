package org.rankeduta;

import org.json.JSONObject;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.rankeduta.defines.ServerRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Properties;
import org.rankeduta.services.commands.Command;

public class RankedUTA implements ModInitializer {
	// Define the logger for this mod
	public static final String MOD_ID = "Ranked UTA";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static String PROPERTY_PATH = "server.properties";

	public static ServerRole serverRole = ServerRole.unknown;

	@Override
	public void onInitialize() {
		// Register the command
		Command.register();

        // Properties Read
		File propertyFile = new File(PROPERTY_PATH);
		Properties properties = new Properties();
		// Load the properties file
		try (FileInputStream input = new FileInputStream(propertyFile)) {
			properties.load(input);
			LOGGER.info("Loaded properties from {}", PROPERTY_PATH);
			serverRole = ServerRole.fromString(properties.getProperty("server-role", "unknown").toLowerCase());
			if (!serverRole.equals(ServerRole.lobby) && !serverRole.equals(ServerRole.match))
				serverRole = ServerRole.unknown;
		} catch (IOException e) {
			LOGGER.error("Failed to load {}: {}",
				PROPERTY_PATH, e.getMessage());
		}

		// Check if the properties file have the 'server-role' property
		if (!properties.containsKey("server-role")) {
			properties.setProperty("server-role", "unknown");
			try (FileOutputStream output = new FileOutputStream(propertyFile)) {
				properties.store(output, "Ranked UTA Server Properties");
			} catch (IOException e) {
				LOGGER.error("Failed to update {}: {}", PROPERTY_PATH, e.getMessage());
			}
		}

		ServerLifecycleEvents.SERVER_STARTED.register(this::OnServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::OnServerStopping);

		ServerPlayConnectionEvents.JOIN.register(this::OnPlayerJoin);
		ServerPlayConnectionEvents.DISCONNECT.register(this::OnPlayerLeave);

		LOGGER.info("Mod {} initialized!", MOD_ID);
	}

	private void OnServerStarted(MinecraftServer server) {
		if (!serverRole.equals(ServerRole.unknown))
			LOGGER.info("{} server is starting...", serverRole.name());
		else
			LOGGER.warn("Server role is {}. Please set 'server-role' in {} to 'lobby' or 'match'.",
				serverRole.name(), PROPERTY_PATH);
    }

    private void OnServerStopping(MinecraftServer server) {
		if (!serverRole.equals(ServerRole.unknown))
			LOGGER.info("{} server is stopping...", serverRole.name());
    }

    private void OnPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
		ServerPlayerEntity player = handler.getPlayer();
		long lastJoin = System.currentTimeMillis();
		String playerData = new JSONObject()
			.put("name", player.getName().getString())
			.put("uuid", player.getUuid().toString())
			.put("timestamp", lastJoin).toString();
		// Here you can handle the player joining the server
		HttpResponse<String> response = HTTPClient.post("/player/connect", playerData);
		if (response != null && response.statusCode() == 200) {
		   	JSONObject jsonResponse = new JSONObject(response.body());
			String receivedMessage = jsonResponse.get("message").toString();
			LOGGER.info("Successfully received from Backend Server: {}", receivedMessage);
		} else {
			LOGGER.warn("Failed to send {} to connect server: {}",
				player.getName().getString(), response != null ? response.body() : "No response");
		}
    }

    private void OnPlayerLeave(ServerPlayNetworkHandler handler, MinecraftServer server) {
		ServerPlayerEntity player = handler.getPlayer();
		long lastJoin = System.currentTimeMillis();
		String playerData = new JSONObject()
			.put("name", player.getName().getString())
			.put("uuid", player.getUuid().toString())
			.put("timestamp", lastJoin).toString();
		// Here you can handle the player leaving the server
		HttpResponse<String> response = HTTPClient.post("/player/disconnect", playerData);
		if (response != null && response.statusCode() == 200) {
			JSONObject jsonResponse = new JSONObject(response.body());
			String receivedMessage = jsonResponse.get("message").toString();
			LOGGER.info("Successfully received from Backend Server: {}", receivedMessage);
		} else {
			LOGGER.warn("Failed to send {} to disconnect server: {}",
				player.getName().getString(), response != null ? response.body() : "No response");
		}
	}
}