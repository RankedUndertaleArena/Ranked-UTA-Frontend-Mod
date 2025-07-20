package org.rankeduta;

import org.json.*;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.rankeduta.defines.ServerRole;
import org.rankeduta.services.ThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.UUID;

import org.rankeduta.commands.Command;

public class RankedUTA implements ModInitializer {
	// Define the logger for this mod
	public static final String MOD_ID = "Ranked UTA";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ThreadService threadService = new ThreadService();
	public static final UUID SERVER_UUID = UUID.randomUUID();

	public static String PROPERTY_PATH = "server.properties";
	public static ServerRole serverRole = ServerRole.unknown;
	public static Properties properties = new Properties();

	@Override
	public void onInitialize() {
        // Load the server properties
		File propertyFile = new File(PROPERTY_PATH);
		try (FileInputStream input = new FileInputStream(propertyFile)) {
			properties.load(input);
			serverRole = ServerRole.fromString(properties.getProperty("server-role", "unknown").toLowerCase());
			if (!serverRole.equals(ServerRole.lobby) && !serverRole.equals(ServerRole.match)) serverRole = ServerRole.unknown;
		} catch (IOException e) {
			LOGGER.error("Failed to load {}: {}", PROPERTY_PATH, e.getMessage());
		}
		if (!properties.containsKey("server-role")) {
			properties.setProperty("server-role", "unknown");
			try (FileOutputStream output = new FileOutputStream(propertyFile)) {
				properties.store(output, "Ranked UTA Server Properties");
			} catch (IOException e) {
				LOGGER.error("Failed to update {}: {}", PROPERTY_PATH, e.getMessage());
			}
		}

		// Register the command
		Command.register();

		ServerLifecycleEvents.SERVER_STARTED.register(this::OnServerStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::OnServerStopping);

		ServerPlayConnectionEvents.JOIN.register(this::OnPlayerJoin);
		ServerPlayConnectionEvents.DISCONNECT.register(this::OnPlayerLeave);

		LOGGER.info("Mod {} initialized!", MOD_ID);
	}

	private void OnServerStarted(MinecraftServer server) {
		switch (serverRole) {
            case lobby -> threadService.startAtLobby(server);
            case match -> {
				int port = server.getServerPort();
				String body = new JSONObject()
					.put("uuid", SERVER_UUID.toString())
					.put("port", port)
					.toString();
				HttpResponse<String> response = HTTPClient.post("/server/game/register", body);
				JSONObject jsonResponse = HTTPClient.receivedResponse(response);
				if (jsonResponse != null && jsonResponse.getInt("status") == 200) threadService.startAtMatch(server, SERVER_UUID);
			}
			default -> LOGGER.warn("Server role is {}. Please set 'server-role' in {} to 'lobby' or 'match'.", serverRole.name(), PROPERTY_PATH);
		}
    }

    private void OnServerStopping(MinecraftServer server) {
		threadService.stop();
    }

    private void OnPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
		switch (serverRole) {
			case lobby -> {
				ServerPlayerEntity player = handler.getPlayer();
				long lastJoin = System.currentTimeMillis();

				String body = new JSONObject()
					.put("name", player.getName().getString())
					.put("uuid", player.getUuidAsString())
					.put("timestamp", lastJoin)
					.toString();

				HTTPClient.post("/player/connect", body);
			}
			case match -> {}
		}
    }

    private void OnPlayerLeave(ServerPlayNetworkHandler handler, MinecraftServer server) {
		switch (serverRole) {
			case lobby -> {
				ServerPlayerEntity player = handler.getPlayer();
				long lastJoin = System.currentTimeMillis();

				String body = new JSONObject()
					.put("name", player.getName().getString())
					.put("uuid", player.getUuidAsString())
					.put("timestamp", lastJoin)
					.toString();

				HTTPClient.post("/player/disconnect", body);
			}
			case match -> {}
		}
	}
}