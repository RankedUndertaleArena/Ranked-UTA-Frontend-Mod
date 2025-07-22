package org.rankeduta;

import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.rankeduta.defines.ServerRole;
import org.rankeduta.events.PlayerEvent;
import org.rankeduta.events.ServerEvent;
import org.rankeduta.services.ThreadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import org.rankeduta.events.CommandInit;

public class RankedUTA implements ModInitializer {
	// Define the logger for this mod
	public static final String MOD_ID = "Ranked UTA";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final UUID SERVER_UUID = UUID.randomUUID();

	public static String PROPERTY_PATH = "server.properties";
	public static ServerRole serverRole = ServerRole.unknown;
	public static Properties properties = new Properties();
	public static ThreadService threadService = new ThreadService();

	public static String CONFIG_PATH = "config/rankeduta.properties";
	public static String apiUrl = "";

	@Override
	public void onInitialize() {
        // Load the server properties
		loadProperties(new File(PROPERTY_PATH));
		loadConfig(new File(CONFIG_PATH));

		// Register the command
		CommandInit.register();

		ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
			if (apiUrl.isEmpty()) {
				LOGGER.warn("API URL 未設定，請在 {} 中設定 api-url", CONFIG_PATH);
				// CLose Server
				ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(server);
			} else {
				LOGGER.info("API URL 已設定為：{}", apiUrl);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(ServerEvent::OnStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(ServerEvent::OnStopping);

		ServerPlayConnectionEvents.JOIN.register(PlayerEvent::OnJoin);
		ServerPlayConnectionEvents.DISCONNECT.register(PlayerEvent::OnLeave);

		LOGGER.info("模組初始化完成！");
	}

	private static void loadProperties(File propertyFile) {
		try (FileInputStream input = new FileInputStream(propertyFile)) {
			properties.load(input);
			serverRole = ServerRole.fromString(properties.getProperty("server-role", "unknown").toLowerCase());
			if (!serverRole.equals(ServerRole.lobby) && !serverRole.equals(ServerRole.match)) serverRole = ServerRole.unknown;
		} catch (IOException e) {
			LOGGER.error("讀取 {} 失敗：{}", PROPERTY_PATH, e.getMessage());
		}
		if (!properties.containsKey("server-role")) {
			properties.setProperty("server-role", "unknown");
			try (FileOutputStream output = new FileOutputStream(propertyFile)) {
				properties.store(output, "Ranked UTA Server Properties");
			} catch (IOException e) {
				LOGGER.error("寫入 {} 失敗：{}", PROPERTY_PATH, e.getMessage());
			}
		}
	}

	private static void loadConfig(File propertyFile) {
		try (FileInputStream input = new FileInputStream(propertyFile)) {
			properties.load(input);
			apiUrl = properties.getProperty("api-url", "");
		} catch (IOException e) {
			LOGGER.error("讀取 {} 失敗：{}", CONFIG_PATH, e.getMessage());
		}
		if (!properties.containsKey("api-url")) {
			properties.setProperty("api-url", "");
			try (FileOutputStream output = new FileOutputStream(propertyFile)) {
				properties.store(output, "Ranked UTA Config Properties");
			} catch (IOException e) {
				LOGGER.error("寫入 {} 失敗：{}", CONFIG_PATH, e.getMessage());
			}
		}
	}
}