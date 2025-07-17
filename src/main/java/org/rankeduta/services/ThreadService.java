package org.rankeduta.services;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.HTTPClient;
import org.rankeduta.RankedUTA;

import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadService {
    public static ScheduledExecutorService queueScheduler = Executors.newScheduledThreadPool(1);
    public static ScheduledExecutorService matchScheduler = Executors.newScheduledThreadPool(1);

    public void start(MinecraftServer server) {
        startQueuePolling(server);
        startMatchPolling(server);
    }

    public void startQueuePolling (MinecraftServer server) {
        queueScheduler.scheduleAtFixedRate(() -> {
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
                        RankedUTA.LOGGER.error("處理某筆隊伍資料時出錯：", e);
                    }
                }
            } catch (Exception e) {
                RankedUTA.LOGGER.error("輪詢任務發生例外，任務將繼續執行：", e);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void startMatchPolling (MinecraftServer server) {
        matchScheduler.scheduleAtFixedRate(() -> {
            try {
                RankedUTA.LOGGER.debug("執行 match 輪詢任務");
                HttpResponse<String> response = HTTPClient.get("/match", null);
                JSONObject jsonResponse = HTTPClient.receivedResponse(response);
                if (jsonResponse == null) return;
                JSONArray data = jsonResponse.optJSONArray("data");
                if (data == null || data.isEmpty()) return;
                RankedUTA.LOGGER.info("Received Data: {}", data);
                for (int i = 0; i < data.length(); i++) {
                    StringBuilder mainBuilder = new StringBuilder();
                    String[] hoverMessage = new String[] {};
                    JSONObject match = data.optJSONObject(i);
                    String mode = match.optString("mode");
                    JSONArray teams = match.optJSONArray("teams");

                    for (int j = 0; j < teams.length(); j++) {
                        JSONObject party = teams.optJSONObject(j);
                        ServerPlayerEntity leader = server.getPlayerManager().getPlayer(UUID.fromString(party.optString("leader")));
                        JSONArray members = party.optJSONArray("members");
                        hoverMessage[j] = "Party Members/n";
                        for (int a = 0; a < members.length(); a++) {
                            ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(members.getString(a)));
                            hoverMessage[j] += member.getName().getString() + "/n";
                        }
                        mainBuilder.append(leader.getName().getString()).append(" VS ");
                        // TODO: StringBuilder
                    }
                }

                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    player.sendMessage(Text.literal(""));
                }
            } catch (Exception e) {
                RankedUTA.LOGGER.error("輪詢任務發生例外，任務將繼續執行：", e);
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        queueScheduler.shutdown();
        matchScheduler.shutdown();
    }
}
