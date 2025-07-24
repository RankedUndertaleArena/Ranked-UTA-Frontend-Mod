package org.rankeduta.command.handler;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.command.Command;
import org.rankeduta.define.ResponseCode;
import org.rankeduta.service.BackendService;
import org.rankeduta.utils.URIBuilder;

import java.net.http.HttpResponse;
import java.util.UUID;

public class PartyHandler implements Command.IHandler {
    @Override
    public int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        PlayerManager playerManager = source.getServer().getPlayerManager();
        ServerPlayerEntity player = source.getPlayer();
        String action = StringArgumentType.getString(context, "action");
        ServerPlayerEntity target = source.getServer().getPlayerManager().getPlayer(StringArgumentType.getString(context, "target"));

        if (player == null) {
            source.sendError(Text.literal("你必須在遊戲中才能使用此命令。"));
            return 0;
        }

        if (action == null || action.isEmpty()) {
            player.sendMessage(Text.literal("動作不能為空。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }

        switch (action) {
            case "invite", "accept", "kick", "transfer" -> {
                if (target == null) {
                    player.sendMessage(Text.literal("目標玩家不存在或不在線。").setStyle(Style.EMPTY.withColor(0xFF5555)));
                    return 0;
                }
                if (player == target) {
                    player.sendMessage(Text.literal("你不能對自己執行此操作。").setStyle(Style.EMPTY.withColor(0xFF5555)));
                    return 0;
                }
                // 處理邀請、接受、踢出或轉移隊伍的邏輯
                switch (action) {
                    case "invite" -> {
                        return inviteRun(player, target);
                    }
                    case "accept" -> {
                        return acceptRun(player, target, playerManager);
                    }
                    case "kick" -> {
                        return kickRun(player, target, playerManager);
                    }
                    case "transfer" -> {
                        return transferRun(player, target, playerManager);
                    }
                }
            }
            case "list", "leave", "disband" -> {
                switch (action) {
                    case "list" -> {
                        return listRun(player, playerManager);
                    }
                    case "leave" -> {
                        return leaveRun(player, playerManager);
                    }
                    case "disband" -> {
                        return disbandRun(player, playerManager);
                    }
                }
            }
            default -> player.sendMessage(Text.literal("未知的動作：" + action).setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }

    public static int inviteRun(ServerPlayerEntity player, ServerPlayerEntity target) {
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .put("target", target.getUuidAsString())
            .put("expire_at", System.currentTimeMillis() + 60000)
            .toString();
        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/party/invite", body));
        if (jsonResponse == null) {
            player.sendMessage(Text.literal("無法連接到隊伍服務，請稍後再試。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }
        switch (ResponseCode.fromCode(jsonResponse.getInt("status"))) {
            case OK -> {
                player.sendMessage(Text.literal("已成功邀請 " + target.getName().getString() + " 加入你的隊伍，他們可以在 60 秒內接受邀請").setStyle(Style.EMPTY.withColor(0x55FF55)));
                target.sendMessage(Text.literal("你已收到來自 " + player.getName().getString() + " 的隊伍邀請，你有 60 秒的時間來使用 ").setStyle(Style.EMPTY.withColor(0x55FF55))
                    .append(Text.literal("/party accept " + player.getName().getString()).setStyle(Style.EMPTY.withColor(0xFFFF55)
                        .withClickEvent(new ClickEvent.RunCommand("/party accept " + player.getName().getString()))
                        .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊這裡接受邀請")))))
                    .append(Text.literal(" 來加入").setStyle(Style.EMPTY.withColor(0x55FF55))));
                return 1;
            }
            case PARTY_MISSING_PERMISSION ->
                player.sendMessage(Text.literal("你沒有權限邀請玩家加入隊伍。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_ALREADY_IN ->
                player.sendMessage(Text.literal(target.getName().getString() + " 已經在你的隊伍中。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_ALREADY_HAVE ->
                player.sendMessage(Text.literal(target.getName().getString() + " 已經在其他隊伍中。").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }
    public static int acceptRun(ServerPlayerEntity player, ServerPlayerEntity inviter, PlayerManager playerManager) {
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .put("inviter", inviter.getUuidAsString())
            .toString();
        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/party/accept", body));
        if (jsonResponse == null) {
            player.sendMessage(Text.literal("無法連接到隊伍服務，請稍後再試。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }
        switch (ResponseCode.fromCode(jsonResponse.getInt("status"))) {
            case OK -> {
                player.sendMessage(Text.literal("你加入了 " + inviter.getName().getString() + " 的隊伍。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player)
                        member.sendMessage(Text.literal(player.getName().getString() + " 已加入隊伍。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                return 1;
            }
            case PARTY_ALREADY_HAVE ->
                player.sendMessage(Text.literal("你已經在其他隊伍中。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_INVITE_NOT_FOUND ->
                player.sendMessage(Text.literal("找不到邀請，請確保你已被邀請加入隊伍。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_LOCKED ->
                player.sendMessage(Text.literal("正在匹配中，無法邀請玩家。").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }
    public static int kickRun(ServerPlayerEntity player, ServerPlayerEntity target, PlayerManager playerManager) {
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .put("target", target.getUuidAsString())
            .toString();
        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/party/kick", body));
        if (jsonResponse == null) {
            player.sendMessage(Text.literal("無法連接到隊伍服務，請稍後再試。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }
        switch (ResponseCode.fromCode(jsonResponse.getInt("status"))) {
            case OK -> {
                player.sendMessage(Text.literal("成功踢出 " + target.getName().getString() + " 出隊伍。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                target.sendMessage(Text.literal("你已被踢出隊伍。").setStyle(Style.EMPTY.withColor(0xFF5555)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player && member != target)
                        member.sendMessage(Text.literal(target.getName().getString() + " 已離開隊伍。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                return 1;
            }
            case PARTY_NOT_FOUND ->
                player.sendMessage(Text.literal("你不在任何隊伍中。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_MISSING_PERMISSION ->
                player.sendMessage(Text.literal("你沒有權限踢出玩家。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_NOT_FOUND_PLAYER ->
                player.sendMessage(Text.literal("找不到目標玩家。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_LOCKED ->
                player.sendMessage(Text.literal("正在匹配中，無法踢出玩家。").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }
    public static int transferRun(ServerPlayerEntity player, ServerPlayerEntity target, PlayerManager playerManager) {
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .put("target", target.getUuidAsString())
            .toString();
        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/party/transfer", body));
        if (jsonResponse == null) {
            player.sendMessage(Text.literal("無法連接到隊伍服務，請稍後再試。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }
        switch (ResponseCode.fromCode(jsonResponse.getInt("status"))) {
            case OK -> {
                player.sendMessage(Text.literal("已成功將隊伍領導權轉移給 " + target.getName().getString() + ".").setStyle(Style.EMPTY.withColor(0x55FF55)));
                target.sendMessage(Text.literal("你已成為隊伍的新領導者。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player && member != target)
                        member.sendMessage(Text.literal(player.getName().getString() + " 已將隊伍領導權轉移給 " + target.getName().getString() + ".").setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                return 1;
            }
            case PARTY_MISSING_PERMISSION ->
                player.sendMessage(Text.literal("你沒有權限轉移隊伍領導權。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_NOT_FOUND ->
                player.sendMessage(Text.literal("你不在任何隊伍中。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_LOCKED ->
                player.sendMessage(Text.literal("正在匹配中，無法轉移隊伍領導權。").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }
    public static int listRun(ServerPlayerEntity player, PlayerManager playerManager) {
        String query = new URIBuilder().addParam("player", player.getUuidAsString()).build().toString();
        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("get", "/party/list", query));
        if (jsonResponse == null) {
            player.sendMessage(Text.literal("無法連接到隊伍服務，請稍後再試。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }
        switch (ResponseCode.fromCode(jsonResponse.getInt("status"))) {
            case OK -> {
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                ServerPlayerEntity leader = playerManager.getPlayer(UUID.fromString(receivedData.getString("leader")));
                int offlineCount = 0;
                player.sendMessage(Text.literal("隊伍成員列表：").setStyle(Style.EMPTY.withColor(0x55FFFF)));
                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) {
                        offlineCount++;
                        continue;
                    }
                    if (member == player)
                        player.sendMessage(Text.literal(" - " + member.getName().getString()).setStyle(Style.EMPTY.withColor(0xFFAA00)));
                    else if (member == leader)
                        player.sendMessage(Text.literal(" - " + member.getName().getString()).setStyle(Style.EMPTY.withColor(0x5555FF)));
                    else
                        player.sendMessage(Text.literal(" - " + member.getName().getString()).setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                if (offlineCount > 0)
                    player.sendMessage(Text.literal(" - " + offlineCount + "名成員已離線").setStyle(Style.EMPTY.withColor(0xAAAAAA)));
                return 1;
            }
            case PARTY_NOT_FOUND ->
                player.sendMessage(Text.literal("你不在任何隊伍中。").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }
    public static int leaveRun(ServerPlayerEntity player, PlayerManager playerManager) {
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .toString();
        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post" , "/party/leave", body));
        if (jsonResponse == null) {
            player.sendMessage(Text.literal("無法連接到隊伍服務，請稍後再試。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }
        switch (ResponseCode.fromCode(jsonResponse.getInt("status"))) {
            case OK -> {
                player.sendMessage(Text.literal("你已成功離開隊伍。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player)
                        member.sendMessage(Text.literal(player.getName().getString() + " 已離開隊伍。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                return 1;
            }
            case PARTY_MISSING_PERMISSION ->
                player.sendMessage(Text.literal("你沒有權限離開隊伍，請使用 /party transfer 轉移隊伍領導權，或 /party disband 來解散隊伍。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_LOCKED ->
                player.sendMessage(Text.literal("正在匹配中，無法離開隊伍。").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }
    public static int disbandRun(ServerPlayerEntity player, PlayerManager playerManager) {
        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .toString();
        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post" , "/party/disband", body));
        if (jsonResponse == null) {
            player.sendMessage(Text.literal("無法連接到隊伍服務，請稍後再試。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }
        switch (ResponseCode.fromCode(jsonResponse.getInt("status"))) {
            case OK -> {
                player.sendMessage(Text.literal("你已成功解散隊伍。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                for (int i = 0; i < members.length(); i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player)
                        member.sendMessage(Text.literal(player.getName().getString() + " 已解散隊伍。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                return 1;
            }
            case PARTY_NOT_FOUND ->
                player.sendMessage(Text.literal("你不在任何隊伍中。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case PARTY_LOCKED ->
                player.sendMessage(Text.literal("正在匹配中，無法解散隊伍。").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }
}
