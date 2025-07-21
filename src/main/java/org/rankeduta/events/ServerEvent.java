package org.rankeduta.events;

import net.minecraft.server.MinecraftServer;
import org.json.JSONObject;
import org.rankeduta.HTTPClient;
import org.rankeduta.defines.ServerRole;

import java.net.http.HttpResponse;

import static org.rankeduta.RankedUTA.*;

public class ServerEvent {
    public static void OnStarted(MinecraftServer server) {
        switch (serverRole) {
            case lobby -> threadService.startLobby(server);
            case match -> {
                int port = server.getServerPort();
                String body = new JSONObject()
                    .put("uuid", SERVER_UUID.toString())
                    .put("port", port)
                    .toString();
                HttpResponse<String> response = HTTPClient.post("/server/game/register", body);
                JSONObject jsonResponse = HTTPClient.receivedResponse(response);
                if (jsonResponse != null) {
                    threadService.startMatch(server, SERVER_UUID);
                    LOGGER.info("遊戲伺服器已註冊，UUID: {}, 端口：{}", SERVER_UUID, port);
                }
            }
            default -> LOGGER.warn("伺服器角色未設定或未知，請把 server.properties 中的 server-role 設定成 lobby 或 match");
        }
    }

    public static void OnStopping(MinecraftServer server) {
        if (serverRole.equals(ServerRole.lobby) || serverRole.equals(ServerRole.match)) {
            threadService.stop();
            LOGGER.info("伺服器正在關閉，停止所有任務");
        }
    }
}
