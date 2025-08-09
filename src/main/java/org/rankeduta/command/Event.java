package org.rankeduta.command;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.define.ServerRole;
import org.rankeduta.service.BackendService;
import org.rankeduta.service.ThreadService;
import org.rankeduta.utils.URIBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.rankeduta.RankedUTA.*;
import static org.rankeduta.service.ThreadService.scheduler;

public class Event {
    public static void ServerStarted(MinecraftServer server) {
        switch (serverRole) {
            case ServerRole.LOBBY -> {
                ThreadService.startLobby(server);
                LOGGER.info("Lobby server started successfully.");
            }
            case ServerRole.MATCH -> {
                String body = new JSONObject()
                    .put("uuid", SERVER_UUID.toString())
                    .put("port", server.getServerPort())
                    .toString();
                JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/server/game/register", body));
                if (jsonResponse != null) {
                    LOGGER.info("Game server registered, UUID: {}, Port: {}", SERVER_UUID, server.getServerPort());
                } else LOGGER.warn("Failed to register game server, please check if the API URL is set correctly in {}", PROPERTY_PATH);
                ThreadService.startMatch(server, SERVER_UUID);
            }
        }
    }

    public static void ServerStopping(MinecraftServer server) {
        if (serverRole == ServerRole.LOBBY || serverRole == ServerRole.MATCH) ThreadService.stop();
    }

    public static void PlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        switch (serverRole) {
            case ServerRole.LOBBY -> {
                ServerPlayerEntity player = handler.getPlayer();
                long lastJoin = System.currentTimeMillis();

                String body = new JSONObject()
                    .put("name", player.getName().getString())
                    .put("uuid", player.getUuidAsString())
                    .put("timestamp", lastJoin)
                    .toString();
                JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/player/connect", body));
                if (jsonResponse == null) LOGGER.error("Failed to connect player to lobby, please check if the backend service is running.");
            }
            case ServerRole.MATCH ->
                scheduler.schedule(() -> {
                    int rankAvg = 0, requiredPlayers;
                    ServerCommandSource source = server.getCommandSource();
                    String query = new URIBuilder().addParam("server", SERVER_UUID.toString()).build().toString();
                    JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("get", "/game", query));
                    if (jsonResponse != null) {
                        JSONObject data = jsonResponse.optJSONObject("data");
                        JSONArray teams = data.getJSONArray("teams");
                        String mode = data.getString("mode");
                        requiredPlayers = switch (mode) {
                            case "siege" -> 8;
                            case "squad" -> 6;
                            case "duo" -> 4;
                            default -> 2; // solo
                        };
                        for (int i = 0; i < teams.length(); i++) {
                            JSONObject team = teams.getJSONObject(i);
                            rankAvg += team.getInt("rank");
                            JSONArray members = team.optJSONArray("members");
                            if (members == null || members.isEmpty()) continue;
                            for (int j = 0; j < members.length(); j++) {
                                UUID uuid = UUID.fromString(members.getString(j));
                                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                                if (player != null) {
                                    ServerScoreboard scoreboardManager = server.getScoreboard();
                                    ScoreHolder holder = ScoreHolder.fromName(player.getName().getString());
                                    ScoreboardObjective objective = scoreboardManager.getObjectives().stream()
                                        .filter(obj -> obj.getName().equals("tid")).findFirst().orElse(null);
                                    if (holder != null && objective != null)
                                        scoreboardManager.getOrCreateScore(holder, objective, true).setScore(i + 1); // Set team ID based on index
                                }
                            }
                        }
                        rankAvg /= requiredPlayers;
                        String command = "function ranked:wait_for_join {" +
                            "gameMode:" + (mode.equals("siege") ? 7 : 4) + "," + // 4 for solo/duo/squad, 7 for siege
                            "gameRound:" + (rankAvg > 2200 ? 5 : rankAvg > 1800 ? 4 : rankAvg > 1400 ? 3 : 2) + ", " + // 5 for 2200+, 4 for 1800-2200, 3 for 1400-1800, 2 for <1400
                            "banRuleType:" + (mode.equals("solo") ? 2 : 1 ) + "," + // 2 for solo, 1 for duo/squad/siege
                            "enableSameChar:0," +
                            "enableNewWillSystem:0," +
                            "enableNewSkill:0," +
                            "requiredPlayers:" + requiredPlayers +
                            "}";
                        server.getCommandManager().executeWithPrefix(source, command);
                        LOGGER.debug("Starting game with command: {}", command);
                    } else LOGGER.error("Could not connect to the game server, please check if the server is running");
                }, 1, TimeUnit.SECONDS);
        }
    }

    public static void PlayerLeave(ServerPlayNetworkHandler handler, MinecraftServer server) {
        if (serverRole == ServerRole.LOBBY) {
            ServerPlayerEntity player = handler.getPlayer();
            long lastJoin = System.currentTimeMillis();

            String body = new JSONObject()
                .put("name", player.getName().getString())
                .put("uuid", player.getUuidAsString())
                .put("timestamp", lastJoin)
                .toString();

            JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/player/disconnect", body));
            if (jsonResponse == null) LOGGER.error("Failed to disconnect player from lobby, please check if the backend service is running.");
        }
    }
}
