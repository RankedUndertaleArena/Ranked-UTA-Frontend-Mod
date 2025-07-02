package org.rankeduta;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.bson.Document;
import org.rankeduta.defines.ServerRole;
import org.rankeduta.defines.ServerStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class RankedUTA implements ModInitializer {
	// Define the logger for this mod
	public static final String MOD_ID = "rankeduta";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static String PROPERTY_PATH = "server.properties";
	public static ServerRole SERVER_ROLE = ServerRole.UNKNOWN;
	public static ServerStatus SERVER_STATUS = ServerStatus.UNKNOWN;
	public MongoClient mongoClient;
	public MongoDatabase mongoDatabase;

	@Override
	public void onInitialize() {
		File propertyFile = new File(PROPERTY_PATH);
		Properties properties = new Properties();
		// Check if the properties file have the 'server-role' property
		if (!properties.containsKey("server-role")) {
			properties.setProperty("server-role", "unknown");
			try (FileOutputStream output = new FileOutputStream(propertyFile)) {
				properties.store(output, "Ranked UTA Server Properties");
			} catch (IOException e) {
				LOGGER.error("Failed to update {}: {}", PROPERTY_PATH, e.getMessage());
			}
		}
		// Load the properties file
		try (FileInputStream input = new FileInputStream(propertyFile)) {
			properties.load(input);
			SERVER_ROLE = ServerRole.fromString(properties.getProperty("server-role", "unknown").toLowerCase());
			if (!SERVER_ROLE.equals(ServerRole.LOBBY)
				&& !SERVER_ROLE.equals(ServerRole.MATCH)) {
				LOGGER.warn("Server role is {}. Please set 'server-role' in {} to 'lobby' or 'match'.",
					SERVER_ROLE.getServerRole(), PROPERTY_PATH);
				SERVER_ROLE = ServerRole.UNKNOWN;
			}
		} catch (IOException e) {
			LOGGER.error("Failed to load {}: {}",
				PROPERTY_PATH, e.getMessage());
		}
		// Connect to MongoDB
		mongoClient = MongoClients.create("mongodb+srv://owner:OAit6yunwFuN7b1q@player.ow5nhhq.mongodb.net/?retryWrites=true&w=majority&appName=Player");
		mongoDatabase = mongoClient.getDatabase("Stats");
		LOGGER.info("Connected to MongoDB database: {}", mongoDatabase.getName());

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			String IP = "localhost";
			if (!server.getServerIp().isEmpty()) IP = server.getServerIp();
			String port = String.valueOf(server.getServerPort());
			SERVER_STATUS = switch (SERVER_ROLE) {
				case LOBBY -> ServerStatus.LOBBY;
				case MATCH -> ServerStatus.IDLE;
				default -> ServerStatus.UNKNOWN;
			};

			if (!SERVER_ROLE.equals(ServerRole.UNKNOWN)) {
				String IP_PORT = IP + ":" + port;
				Document newServer = new Document("ip", IP_PORT)
					.append("role", SERVER_ROLE.getServerRole())
					.append("status", SERVER_STATUS.getStatus());
				mongoDatabase.getCollection("Server").insertOne(newServer);
				LOGGER.info("Added new {} server data ({})",
					SERVER_ROLE.getServerRole(), IP_PORT);
			}
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			String IP = "localhost";
			if (!server.getServerIp().isEmpty()) IP = server.getServerIp();
			String port = String.valueOf(server.getServerPort());
			String IP_PORT = IP + ":" + port;

			mongoDatabase.getCollection("Server").deleteOne(Filters.eq("ip", IP_PORT));
			LOGGER.info("Removed {} server data ({})",
				SERVER_ROLE.getServerRole(), IP_PORT);

			if (mongoClient != null) {
				mongoClient.close();
				LOGGER.info("Closed MongoDB connection.");
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			String playerName = player.getName().getString();
			String playerUUID = player.getUuidAsString();
			long lastLogin = System.currentTimeMillis();

			Document playerData = mongoDatabase.getCollection("Player").find(Filters.eq("uuid", playerUUID)).first();
			if (playerData != null) {
				Document newPlayer = new Document("uuid", playerUUID)
					.append("name", playerName)
					.append("lastLogin", lastLogin)
					.append("rankSolo", 1000)
					.append("rankDuo", 1000)
					.append("rankSquad", 1000)
					.append("rankSiege", 1000);
				mongoDatabase.getCollection("Player").insertOne(newPlayer);
				LOGGER.info("Added new player data for {} ({})", playerName, playerUUID);
			} else {
				mongoDatabase.getCollection("Player").updateOne(Filters.eq("uuid", playerUUID),
					new Document("$set", new Document("lastLogin", lastLogin)));

				boolean isUpdated = false;
				if (!playerData.getString("name").equals(playerName)) {
					isUpdated = true;
					String oldName = playerData.getString("name");
					mongoDatabase.getCollection("Player").updateOne(Filters.eq("uuid", playerUUID),
						new Document("$set", new Document("name", playerName)));
					LOGGER.info("Updated player name from {} to {} ({})", oldName, playerName, playerUUID);
				}
				if (!isUpdated) LOGGER.info("Loaded player data for {} ({})", playerName, playerUUID);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			String playerUUID = player.getUuidAsString();
			long lastLogin = System.currentTimeMillis();

			mongoDatabase.getCollection("Player").updateOne(Filters.eq("uuid", playerUUID),
				new Document("$set", new Document("lastLogin", lastLogin)));
			LOGGER.info("Updated last login for player {} ({})", player.getName().getString(), playerUUID);
		});

		LOGGER.info("Mod {} initialized!",
			MOD_ID);
	}
}