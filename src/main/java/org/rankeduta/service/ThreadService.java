package org.rankeduta.service;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.RankedUTA;
import org.rankeduta.utils.TextBuilder;

import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.rankeduta.RankedUTA.LOGGER;
import static org.rankeduta.RankedUTA.SERVER_UUID;

public class ThreadService {
    public static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public static void startLobby(MinecraftServer server) {
        PollingQueueAndMatch(server);
    }

    public static void startMatch(MinecraftServer server, UUID uuid) {
        PollingHandShake(server, uuid);
    }

    public static void PollingQueueAndMatch(MinecraftServer server) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                RankedUTA.LOGGER.debug("Queue Polling Task is running...");
                JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("get", "/queue", null));
                if (jsonResponse != null) {
                    JSONArray data = jsonResponse.optJSONArray("data");
                    if (data == null || data.isEmpty()) return;
                    for (int i = 0; i < data.length(); i++) {
                        try {
                            JSONObject party = data.getJSONObject(i);
                            String mode = party.optString("mode");
                            JSONArray members = party.optJSONArray("players");
                            if (members == null || members.isEmpty()) continue;
                            for (int j = 0; j < members.length(); j++) {
                                try {
                                    UUID uuid = UUID.fromString(members.getString(j));
                                    ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
                                    if (player != null) {
                                        Text actionBarMessage = Text.literal("你正在 " + mode + " 匹配隊列")
                                                .setStyle(Style.EMPTY.withColor(Formatting.YELLOW));
                                        player.networkHandler.sendPacket(new GameMessageS2CPacket(actionBarMessage, true));
                                    }
                                } catch (IllegalArgumentException e) {
                                    RankedUTA.LOGGER.warn("Invalid UUID in party data: {}", members.getString(j));
                                }
                            }
                        } catch (Exception e) {
                            RankedUTA.LOGGER.error("Handling party data failed: {}", e.getMessage());
                        }
                    }
                } else RankedUTA.LOGGER.warn("Could not connect to the queue service, please check if the service is running.");
            } catch (Exception e) {
                RankedUTA.LOGGER.error("Queue Polling Task encountered an error: {}", e.getMessage());
            }
            scheduler.schedule(() -> {
                try {
                    RankedUTA.LOGGER.debug("Match Polling Task is running...");
                    JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("get", "/match", null));
                    if (jsonResponse != null) {
                        JSONArray data = jsonResponse.optJSONArray("data");
                        if (data == null || data.isEmpty()) return;
                        for (int i = 0; i < data.length(); i++) {
                            TextBuilder boardCastBuilder = new TextBuilder()
                                .append(Text.literal("[").setStyle(Style.EMPTY.withColor(0xFFFF55).withBold(true)))
                                .append(Text.literal("匹配").setStyle(Style.EMPTY.withColor(0xFFFFFF).withBold(true)))
                                .append(Text.literal("]").setStyle(Style.EMPTY.withColor(0xFFFF55).withBold(true)))
                                .append(Text.literal(" 一場新的遊戲正在進行中，隊伍成員如下：\n").setStyle(Style.EMPTY.withColor(0xFFFFFF)));

                            JSONObject match = data.optJSONObject(i);
                            String mode = match.optString("mode");
                            JSONArray teams = match.optJSONArray("teams");

                            for (int j = 0; j < teams.length(); j++) {
                                TextBuilder partyHoverBuilder = new TextBuilder()
                                    .append(Text.literal("隊伍成員：\n").setStyle(Style.EMPTY.withColor(0xAAAAAA).withBold(true)));

                                JSONObject party = teams.getJSONObject(j);
                                JSONArray members = party.optJSONArray("members");
                                ServerPlayerEntity leader = server.getPlayerManager().getPlayer(UUID.fromString(members.getString(0)));
                                if (leader == null) continue;
                                if (!mode.equals("solo")) {
                                    for (int a = 0; a < members.length(); a++) {
                                        ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(members.getString(a)));
                                        if (member == null) continue;
                                        Text actionBarMessage = Text.literal("匹配成功，正在準備遊戲...").setStyle(Style.EMPTY.withColor(0xFFFF55));
                                        member.networkHandler.sendPacket(new GameMessageS2CPacket(actionBarMessage, true));
                                        partyHoverBuilder = partyHoverBuilder.append("\n" + member.getName().getString());
                                    }

                                    switch (j) {
                                        case 0 ->
                                            boardCastBuilder.append(Text.literal(leader.getName().getString()).setStyle(Style.EMPTY.withColor(0xFF5555)
                                                    .withHoverEvent(new HoverEvent.ShowText(partyHoverBuilder.build()))))
                                            .append(Text.literal(" VS ").setStyle(Style.EMPTY.withColor(0xAAAAAA)));
                                        case 1 ->
                                            boardCastBuilder.append(Text.literal(leader.getName().getString()).setStyle(Style.EMPTY.withColor(0x55FFFF)
                                                .withHoverEvent(new HoverEvent.ShowText(partyHoverBuilder.build()))));
                                    }
                                } else {
                                    for (int a = 0; a < members.length(); a++) {
                                        ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(members.getString(a)));
                                        if (member == null) continue;
                                        Text actionBarMessage = Text.literal(">>> 匹配成功，正在準備遊戲 <<<").setStyle(Style.EMPTY.withColor(0xFFFF55));
                                        member.networkHandler.sendPacket(new GameMessageS2CPacket(actionBarMessage, true));
                                    }

                                    switch (j) {
                                        case 0 ->
                                            boardCastBuilder.append(Text.literal(leader.getName().getString()).setStyle(Style.EMPTY.withColor(0xFF5555)))
                                            .append(Text.literal(" VS ").setStyle(Style.EMPTY.withColor(0xAAAAAA)));
                                        case 1 ->
                                            boardCastBuilder.append(Text.literal(leader.getName().getString()).setStyle(Style.EMPTY.withColor(0x55FFFF)));
                                    }
                                }
                            }

                            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                                player.sendMessage(boardCastBuilder.build());
                            }
                        }
                    } else RankedUTA.LOGGER.warn("Could not connect to the match service, please check if the service is running.");
                } catch (Exception e) {
                    RankedUTA.LOGGER.error("Match Polling Task encountered an error: {}", e.getMessage());
                }
            }, 1, TimeUnit.SECONDS);
        }, 1, 1, TimeUnit.SECONDS);
    }

    public static void PollingHandShake(MinecraftServer server, UUID uuid) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                RankedUTA.LOGGER.debug("HandShake Task is running...");
                JSONArray playersUUID = new JSONArray();
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    playersUUID.put(player.getUuidAsString());
                }
                String body = new JSONObject()
                    .put("uuid", uuid.toString())
                    .put("players", playersUUID)
                    .toString();
                HttpResponse<String> response = BackendService.sendRequest("post", "/server/game/handshake", body);
                if (response == null) RankedUTA.LOGGER.error("Failed to connect to the backend service, please check if the service is running.");
                else {
                    int statusCode = response.statusCode();
                    if (statusCode == 404) {
                        body = new JSONObject()
                            .put("uuid", SERVER_UUID.toString())
                            .put("port", server.getServerPort())
                            .toString();
                        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/server/game/register", body));
                        if (jsonResponse != null) {
                            LOGGER.info("Game server registered, UUID: {}, Port: {}", SERVER_UUID, server.getServerPort());
                        }
                    }
                }
            } catch (Exception e) {
                RankedUTA.LOGGER.error("HandShake Task encountered an error: {}", e.getMessage());
            }
        }, 0, 10, TimeUnit.SECONDS);
    }

    public static void stop() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdownNow();
            RankedUTA.LOGGER.info("All scheduled tasks have been stopped.");
        } else {
            RankedUTA.LOGGER.warn("Scheduler is already shut down.");
        }
    }
}
