package org.rankeduta.services;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.HTTPClient;
import org.rankeduta.defines.ResponseCode;
import org.rankeduta.defines.TextStyles;

import java.net.http.HttpResponse;
import java.util.UUID;

public class PartyService {
    public static int invite(ServerCommandSource source, ServerPlayerEntity player, ServerPlayerEntity target) {
        if (player == target) {
            player.sendMessage(Text.literal("你不能邀請自己加入隊伍").setStyle(TextStyles.ERROR));
            return 0;
        }
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .put("target", target.getUuidAsString())
            .put("expire_at", System.currentTimeMillis() + 60000)
            .toString();
        HttpResponse<String> response = HTTPClient.post("/party/invite", body);
        JSONObject jsonResponse = HTTPClient.receivedResponse(response);
        if (jsonResponse == null) {
            source.sendError(Text.literal("無法連接到伺服器或伺服器回應錯誤"));
            return 0;
        }
        switch (ResponseCode.fromCode(jsonResponse.getInt("status"))) {
            case OK -> {
                player.sendMessage(Text.literal("已成功邀請 " + target.getName().getString() + " 加入你的隊伍，他們可以在 60 秒內接受邀請")
                    .setStyle(TextStyles.SUCCESS));
                target.sendMessage(Text.literal("你已收到來自 " + player.getName().getString() + " 的隊伍邀請，你有 60 秒的時間來使用 ")
                    .setStyle(TextStyles.SUCCESS)
                    .append(Text.literal("/party accept " + player.getName().getString())
                        .setStyle(TextStyles.INFO
                            .withClickEvent(new ClickEvent.RunCommand("/party accept " + player.getName().getString()))
                            .withHoverEvent(new HoverEvent.ShowText(Text.literal("Click me to accept invitation.")))))
                    .append(Text.literal(" 來加入")
                        .setStyle(TextStyles.SUCCESS)));
                return 1;
            }
            case PARTY_MISSING_PERMISSIONS ->
                player.sendMessage(Text.literal("You are not party leader.")
                    .setStyle(TextStyles.ERROR));
            case PARTY_ALREADY_IN ->
                player.sendMessage(Text.literal(target.getName().getString() + " already in your party.")
                    .setStyle(TextStyles.ERROR));
            case PARTY_ALREADY_HAVE ->
                player.sendMessage(Text.literal(target.getName().getString() + " already have a party")
                    .setStyle(TextStyles.ERROR));
        }
        return 0;
    }

    public static int accept(ServerCommandSource source, ServerPlayerEntity player, ServerPlayerEntity inviter, PlayerManager playerManager) {
        if (player == inviter) {
            player.sendMessage(Text.literal("你不能邀請自己加入隊伍").setStyle(TextStyles.ERROR));
            return 0;
        }
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .put("inviter", inviter.getUuidAsString())
            .toString();
        HttpResponse<String> response = HTTPClient.post("/party/accept", body);
        JSONObject jsonResponse = HTTPClient.receivedResponse(response);
        if (jsonResponse == null) {
            source.sendError(Text.literal("無法連接到伺服器或伺服器回應錯誤"));
            return 0;
        }
        ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
        switch (statusCode) {
            case OK -> {
                player.sendMessage(Text.literal("You have joined " + inviter.getName().getString() + "'s party.")
                    .setStyle(TextStyles.SUCCESS));

                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");

                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player)
                        member.sendMessage(Text.literal(player.getName().getString() + " joined the party.")
                            .setStyle(TextStyles.INFO));
                }
                return 1;
            }
            case PARTY_ALREADY_HAVE ->
                player.sendMessage(Text.literal("You already have a party.")
                    .setStyle(TextStyles.ERROR));
            case PARTY_NOT_FOUND_INVITE ->
                player.sendMessage(Text.literal( inviter.getName().getString() + "'s Party invitation is not found.")
                    .setStyle(TextStyles.ERROR));
        }
        return 0;
    }

    public static int kick(ServerCommandSource source, ServerPlayerEntity player, ServerPlayerEntity target, PlayerManager playerManager) {
        if (player == target) {
            player.sendMessage(Text.literal("You cannot kick yourself to a party.").setStyle(TextStyles.ERROR));
            return 0;
        }
        // Prepare the data to send to the backend server
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .put("target", target.getUuidAsString())
            .toString();
        HttpResponse<String> response = HTTPClient.post("/party/kick", body);
        JSONObject jsonResponse = HTTPClient.receivedResponse(response);
        if (jsonResponse == null) {
            source.sendError(Text.literal("無法連接到伺服器或伺服器回應錯誤"));
            return 0;
        }
        ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
        switch (statusCode) {
            case OK -> {
                player.sendMessage(Text.literal("You kicked " + target.getName().getString() + ".")
                    .setStyle(TextStyles.INFO));
                target.sendMessage(Text.literal("You have been kicked " + player.getName().getString() + "'s party.")
                    .setStyle(TextStyles.INFO));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");

                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player && member != target)
                        member.sendMessage(Text.literal(target.getName().getString() + " leave the party.")
                            .setStyle(TextStyles.INFO));
                }
                return 1;
            }
            case PARTY_NOT_FOUND ->
                player.sendMessage(Text.literal("You are not in party.")
                    .setStyle(TextStyles.ERROR));
            case PARTY_MISSING_PERMISSIONS ->
                player.sendMessage(Text.literal("You are not party leader.")
                    .setStyle(TextStyles.ERROR));
            case PARTY_NOT_FOUND_PLAYER ->
                player.sendMessage(Text.literal("Player is not in your party.")
                    .setStyle(TextStyles.ERROR));
            case PARTY_COMMAND_LOCKED ->
                player.sendMessage(Text.literal("You are in queue.")
                    .setStyle(TextStyles.ERROR));
        }
        return 0;
    }

    public static int transfer(ServerCommandSource source, ServerPlayerEntity player, ServerPlayerEntity target, PlayerManager playerManager) {
        if (player == target) {
            player.sendMessage(Text.literal("You cannot transfer party leadership to yourself.")
                .setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .put("target", target.getUuidAsString())
            .toString();
        HttpResponse<String> response = HTTPClient.post("/party/transfer", body);
        JSONObject jsonResponse = HTTPClient.receivedResponse(response);
        if (jsonResponse == null) {
            source.sendError(Text.literal("無法連接到伺服器或伺服器回應錯誤"));
            return 0;
        }
        ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
        switch (statusCode) {
            case OK -> {
                player.sendMessage(Text.literal("Party Leader transfer to " + target.getName().getString())
                    .setStyle(Style.EMPTY.withColor(0x55FF55)));
                target.sendMessage(Text.literal(player.getName().getString() + " transfer leader to you.")
                    .setStyle(Style.EMPTY.withColor(0x55FF55)));

                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");

                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player && member != target)
                        member.sendMessage(Text.literal(player.getName().getString() + "transfer leader to" + target.getName().getString() + ".")
                            .setStyle(TextStyles.INFO));
                }
                return 1;
            }
            case PARTY_MISSING_PERMISSIONS -> player.sendMessage(Text.literal("You already have a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_NOT_FOUND -> player.sendMessage(Text.literal( target.getName().getString() + "'s Party invitation is not found.").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_COMMAND_LOCKED -> player.sendMessage(Text.literal("You are in queue").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }

    public static int leave(ServerCommandSource source, ServerPlayerEntity player, PlayerManager playerManager) {
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .toString();
        HttpResponse<String> response = HTTPClient.post("/party/leave", body);
        JSONObject jsonResponse = HTTPClient.receivedResponse(response);
        if (jsonResponse == null) {
            source.sendError(Text.literal("無法連接到伺服器或伺服器回應錯誤"));
            return 0;
        }
        ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
        switch (statusCode) {
            case OK -> {
                player.sendMessage(Text.literal("You have left the party.").setStyle(Style.EMPTY.withColor(0x55FF55)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");

                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player)
                        member.sendMessage(Text.literal(player.getName().getString() + " leave the party.")
                            .setStyle(TextStyles.INFO));
                }
                return 1;
            }
            case PARTY_MISSING_PERMISSIONS -> player.sendMessage(Text.literal("You already have a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_COMMAND_LOCKED -> player.sendMessage(Text.literal("You are in queue").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }

    public static int list(ServerCommandSource source, ServerPlayerEntity player, PlayerManager playerManager) {
        String body = new HTTPClient.URIBuilder().addParam("player", player.getUuidAsString()).toString();
        HttpResponse<String> response = HTTPClient.get("/party/list", body);
        JSONObject jsonResponse = HTTPClient.receivedResponse(response);
        if (jsonResponse == null) {
            source.sendError(Text.literal("無法連接到伺服器或伺服器回應錯誤"));
            return 0;
        }
        ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
        switch (statusCode) {
            case OK -> {
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                ServerPlayerEntity leader = playerManager.getPlayer(UUID.fromString(receivedData.getString("leader")));
                int offlineCount = 0;

                player.sendMessage(Text.literal("Party members: ").setStyle(Style.EMPTY.withColor(0x55FF55)));

                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) {
                        offlineCount++;
                        continue;
                    }
                    if (member == leader)
                        player.sendMessage(Text.literal(member.getName().getString()).setStyle(Style.EMPTY.withColor(0xFFFF55)));
                    else
                        player.sendMessage(Text.literal(member.getName().getString()).setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                if (offlineCount>0) player.sendMessage(Text.literal("- " + offlineCount + " offline member").setStyle(Style.EMPTY.withColor(0x555555)));
                return 1;
            }
            case PARTY_NOT_FOUND ->
                player.sendMessage(Text.literal("You are not in a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }

    public static int disband(ServerCommandSource source, ServerPlayerEntity player, PlayerManager playerManager) {
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .toString();
        HttpResponse<String> response = HTTPClient.post("/party/disband", body);
        JSONObject jsonResponse = HTTPClient.receivedResponse(response);
        if (jsonResponse == null) {
            source.sendError(Text.literal("無法連接到伺服器或伺服器回應錯誤"));
            return 0;
        }
        ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
        switch (statusCode) {
            case OK -> {
                player.sendMessage(Text.literal("Party disbanded successfully.").setStyle(Style.EMPTY.withColor(0x55FF55)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");

                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player)
                        member.sendMessage(Text.literal(player.getName().getString() + " disbanded the party.")
                            .setStyle(TextStyles.INFO));
                }
                return 1;
            }
            case PARTY_NOT_FOUND -> player.sendMessage(Text.literal("You already have a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_COMMAND_LOCKED -> player.sendMessage(Text.literal("You are in queue").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }
}
