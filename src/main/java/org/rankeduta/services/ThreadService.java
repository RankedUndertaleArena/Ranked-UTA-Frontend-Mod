package org.rankeduta.services;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.HTTPClient;
import org.rankeduta.RankedUTA;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadService {
    public static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);

    public void startLobby(MinecraftServer server) {
        startQueuePolling(server);
        startMatchPolling(server);
    }

    public void startMatch(MinecraftServer server, UUID uuid) {
        startHandShakePolling(server, uuid);
    }

    public void startQueuePolling (MinecraftServer server) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                RankedUTA.LOGGER.debug("執行 queue 輪詢任務");
                HttpResponse<String> response = HTTPClient.get("/queue", null);
                JSONObject jsonResponse = HTTPClient.receivedResponse(response);
                if (jsonResponse == null) return;
                JSONArray data = jsonResponse.optJSONArray("data");
                if (data == null || data.isEmpty()) return;
                for (int i = 0; i < data.length(); i++) {
                    try {
                        JSONObject party = data.getJSONObject(i);
                        String mode = party.optString("mode");
                        JSONArray members = party.optJSONArray("players");

                        if (members == null || members.isEmpty()) {
                            RankedUTA.LOGGER.warn("跳過無效隊伍資料：{}", party);
                            continue;
                        }

                        for (int j = 0; j < members.length(); j++) {
                            try {
                                UUID uuid = UUID.fromString(members.getString(j));
                                ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);

                                if (player != null) {
                                    player.networkHandler.sendPacket(new GameMessageS2CPacket(Text.literal("你正在 " + mode + " 隊列"), true));
                                    RankedUTA.LOGGER.debug("已發送 ActionBar 訊息給 {}", player.getName().getString());
                                } else {
                                    RankedUTA.LOGGER.debug("玩家不在線上：{}", uuid);
                                }
                            } catch (IllegalArgumentException e) {
                                RankedUTA.LOGGER.warn("無效 UUID：{}", members.get(j));
                            }
                        }
                    } catch (Exception e) {
                        RankedUTA.LOGGER.error("處理某筆隊伍資料時出錯：{}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                RankedUTA.LOGGER.error("queue 輪詢任務發生例外，任務將繼續執行：{}", e.getMessage());
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    public void startMatchPolling (MinecraftServer server) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                RankedUTA.LOGGER.debug("執行 match 輪詢任務");
                HttpResponse<String> response = HTTPClient.get("/match", null);
                JSONObject jsonResponse = HTTPClient.receivedResponse(response);
                if (jsonResponse == null) return;
                JSONArray data = jsonResponse.optJSONArray("data");
                if (data == null || data.isEmpty()) return;
                RankedUTA.LOGGER.info(data.toString());
                for (int i = 0; i < data.length(); i++) {
                    List<Text> mainMessage = new ArrayList<>();
                    JSONObject match = data.optJSONObject(i);
                    String mode = match.optString("mode");
                    JSONArray teams = match.optJSONArray("teams");

                    for (int j = 0; j < teams.length(); j++) {
                        StringBuilder hoverMessage = new StringBuilder();
                        JSONObject party = teams.getJSONObject(j);
                        JSONArray members = party.optJSONArray("members");
                        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(UUID.fromString(members.getString(0)));
                        if (leader == null) continue;
                        hoverMessage.append("Party Members\n");
                        for (int a = 0; a < members.length(); a++) {
                            ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(members.getString(a)));
                            if (member == null) continue;
                            hoverMessage.append(member.getName().getString()).append("\n");
                        }
                        hoverMessage.delete(hoverMessage.length()-1,hoverMessage.length());
                        switch (j) {
                            case 0 -> mainMessage.add(Text.literal(leader.getName().getString())
                                .setStyle(Style.EMPTY.withColor(0xFF5555).withHoverEvent(new HoverEvent.ShowText(Text.literal(hoverMessage.toString())))));
                            case 1 -> mainMessage.add(Text.literal(leader.getName().getString())
                                .setStyle(Style.EMPTY.withColor(0x55FFFF).withHoverEvent(new HoverEvent.ShowText(Text.literal(hoverMessage.toString())))));
                            default -> mainMessage.add(Text.literal(leader.getName().getString())
                                .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(hoverMessage.toString())))));
                        }
                    }

                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        player.sendMessage(
                            Text.literal("[Match] ")
                                .setStyle(Style.EMPTY.withColor(0xFFFF55).withBold(true))
                                .append(Text.literal("A new " + mode + " game will start\n").setStyle(Style.EMPTY.withColor(0xFFFFFF)))
                                .append(mainMessage.getFirst())
                                .append(Text.literal(" VS ").setStyle(Style.EMPTY.withColor(0xAAAAAA)))
                                .append(mainMessage.getLast())
                        );
                    }
                }
            } catch (Exception e) {
                RankedUTA.LOGGER.error("match 輪詢任務發生例外，任務將繼續執行：{}", e.getMessage());
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void startHandShakePolling (MinecraftServer server, UUID uuid) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                RankedUTA.LOGGER.debug("執行 handshake 任務");
                JSONArray playersUUID = new JSONArray();
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    playersUUID.put(player.getUuidAsString());
                }
                String body = new JSONObject()
                    .put("uuid", uuid.toString())
                    .put("players", playersUUID)
                    .toString();
                HttpResponse<String> response = HTTPClient.post("/server/game/handshake", body);
                JSONObject jsonResponse = HTTPClient.receivedResponse(response);
                if (jsonResponse == null) return;
            } catch (Exception e) {
                RankedUTA.LOGGER.error("handshake 輪詢任務發生例外，任務將繼續執行：{}", e.getMessage());
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public void stop() {
        if (!scheduler.isShutdown()) scheduler.shutdown();
    }
}
