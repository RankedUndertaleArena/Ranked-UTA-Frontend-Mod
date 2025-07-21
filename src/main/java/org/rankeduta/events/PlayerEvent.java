package org.rankeduta.events;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.HTTPClient;

import java.net.http.HttpResponse;
import java.util.UUID;

import static org.rankeduta.RankedUTA.*;

public class PlayerEvent {
    public static void OnJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
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
            case match -> {
                boolean isAllOnline = true;
                ServerCommandSource source = server.getCommandSource();
                String body = new HTTPClient.URIBuilder().addParam("server", SERVER_UUID.toString()).toString();
                HttpResponse<String> response = HTTPClient.get("/game", body);
                JSONObject jsonResponse = HTTPClient.receivedResponse(response);
                if (jsonResponse == null) {
                    LOGGER.error("無法連接到遊戲伺服器，請檢查伺服器是否已啟動");
                    return;
                }
                JSONObject data = jsonResponse.optJSONObject("data");
                JSONArray teams = data.getJSONArray("teams");
                String mode = data.getString("mode");
                for (int i = 0; i < teams.length(); i++) {
                    JSONArray members = teams.getJSONObject(i).optJSONArray("members");
                    if (members == null || members.isEmpty()) {
                        LOGGER.warn("隊伍 {} 的成員列表為空，跳過此隊伍", i);
                        continue;
                    }
                    for (int j = 0; j < members.length(); j++) {
                        UUID uuid = UUID.fromString(members.getString(j));
                        ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                        if (player == null) {
                            isAllOnline = false;
                            break;
                        }
                    }
                    if (!isAllOnline) break;
                }

                if (isAllOnline) {
                    int rankAvg = 0;
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
                                if (holder != null && objective != null) scoreboardManager.getOrCreateScore(holder, objective).setScore(i+1);
                            }
                        }
                    }
                    rankAvg /= mode.equals("siege") ? 8 : mode.equals("squad") ? 6 : mode.equals("duo") ? 4 : 2; // siege:8, squad:6, duo:4, solo:2
                    // function ranked:start_game {gameMode:4,gameRound:2,banRuleType:1,enableSameChar:1,enableNewWillSystem:0,enableNewSkill:0}
                    String command = "function ranked:start_game {gameMode:" +
                        (mode.equals("siege") ? 7 : 4) + // 4 for solo/duo/squad, 7 for siege;
                        ",gameRound:" + (rankAvg > 2200 ? 5 : rankAvg > 1800 ? 4 : rankAvg > 1400 ? 3 : 2) + // 5 for 2200+, 4 for 1800-2200, 3 for 1400-1800, 2 for <1400
                        ",banRuleType:1,enableSameChar:1,enableNewWillSystem:0,enableNewSkill:0}";
                    LOGGER.info("所有隊伍成員都在線上，開始遊戲");
                    server.getCommandManager().executeWithPrefix(source, command);
                    // TODO: Send a message to tell backend server to start the game
                } else {
                    LOGGER.warn("有玩家不在線上，無法開始遊戲");
                }
            }
        }
    }

    public static void OnLeave(ServerPlayNetworkHandler handler, MinecraftServer server) {
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
