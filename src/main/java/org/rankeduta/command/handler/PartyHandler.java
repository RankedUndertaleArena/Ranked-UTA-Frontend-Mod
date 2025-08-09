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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PartyHandler implements Command.IHandler {

    private final Map<String, RunAction> runActionMap = new HashMap<>();

    public PartyHandler()
    {
        runActionMap.put("invite", new InviteAction());
        runActionMap.put("accept", new AcceptAction());
        runActionMap.put("kick", new KickAction());
        runActionMap.put("transfer", new TransferAction());
        runActionMap.put("list", new ListAction());
        runActionMap.put("leave", new LeaveAction());
        runActionMap.put("disband", new DisbandAction());
    }

    @Override
    public int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        PlayerManager playerManager = source.getServer().getPlayerManager();
        ServerPlayerEntity player = source.getPlayer();
        String action = StringArgumentType.getString(context, "action");
        ServerPlayerEntity target = null;
        String targetName;
        try {
            targetName = StringArgumentType.getString(context, "target");
            target = playerManager.getPlayer(targetName);
        } catch (IllegalArgumentException ignored) {}

        if (player == null) {
            source.sendError(Text.literal("你必須在遊戲中才能使用此命令。"));
            return 0;
        }

        if (action == null || action.isEmpty()) {
            player.sendMessage(Text.literal("動作不能為空。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }

        RunAction runAction = runActionMap.get(action); //尋找對應的處理物件
        if (runAction != null)
            return runAction.run(player, target, playerManager);

        player.sendMessage(Text.literal("未知的動作：" + action).setStyle(Style.EMPTY.withColor(0xFF5555)));
        return 0;
    }

    private interface ResponseAction
    {
        int checkResponse(ServerPlayerEntity player, ServerPlayerEntity target, PlayerManager playerManager, JSONObject jsonResponse); //可用 lambda

        ResponseAction EMPTY = (p, t, pM, jR) -> 0; //responseActionMap 內找不到時用
    }

    private record ErrorResponseAction(String errorMessage) implements ResponseAction //專為 ResponseCode 不是 OK 時使用
    {
        @Override
        public int checkResponse(ServerPlayerEntity player, ServerPlayerEntity target, PlayerManager playerManager, JSONObject jsonResponse)
        {
            player.sendMessage(Text.literal(errorMessage).setStyle(RunAction.ERROR_MESSAGE_STYLE)); //傳送固定的訊息
            return 0;
        }
    }

    private static abstract class RunAction
    {
        private final String url; //sendRequest 會用到
        private final String method = this instanceof ListAction ? "get" : "post"; //只有/list 用的是 get 其他都用 post
        protected final JSONObject modifiableBody = new JSONObject(); //createBody 內可以修改
        public static final Style ERROR_MESSAGE_STYLE = Style.EMPTY.withColor(0xFF5555);

        protected RunAction(String url)
        {
            this.url = url;
        }

        protected int run(ServerPlayerEntity player, ServerPlayerEntity target, PlayerManager playerManager)
        {
            String body = createBody(player, target); //各個子類別自行實作 json body
            JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest(method, "/party/" + url, body));
            if (jsonResponse == null) {
                player.sendMessage(Text.literal("無法連接到隊伍服務，請稍後再試。").setStyle(ERROR_MESSAGE_STYLE));
                return 0;
            }

            return responseActionMap.getOrDefault(ResponseCode.fromCode(jsonResponse.getInt("status")), ResponseAction.EMPTY)
                    .checkResponse(player, target, playerManager, jsonResponse); //根據回傳狀態做出動作
        }

        protected abstract String createBody(ServerPlayerEntity player, ServerPlayerEntity target);

        protected final Map<ResponseCode, ResponseAction> responseActionMap = new EnumMap<>(ResponseCode.class);
    }

    private static abstract class NeedPlayerAction extends RunAction
    {
        protected NeedPlayerAction(String url) //處理邀請、接受、踢出或轉移隊伍
        {
            super(url);
        }

        @Override
        protected int run(ServerPlayerEntity player, ServerPlayerEntity target, PlayerManager playerManager)
        {
            //這個 class 是需要做 player 和 target 的 null check 的
            if (target == null) {
                player.sendMessage(Text.literal("目標玩家不存在或不在線上。").setStyle(ERROR_MESSAGE_STYLE));
                return 0;
            }
            if (player == target) {
                player.sendMessage(Text.literal("你不能對自己執行此操作。").setStyle(ERROR_MESSAGE_STYLE));
                return 0;
            }

            return super.run(player, target, playerManager); //通過了 null 檢查後再執行親類別的
        }
    }

    private static class InviteAction extends NeedPlayerAction
    {
        private InviteAction()
        {
            super("invite");
            responseActionMap.put(ResponseCode.OK, (player, target, playerManager, jsonResponse) ->
            {
                player.sendMessage(Text.literal("已成功邀請 " + target.getName().getString() + " 加入你的隊伍，他們可以在 60 秒內接受邀請")
                        .setStyle(Style.EMPTY.withColor(0x55FF55)));
                String playerName = player.getName().getString();
                target.sendMessage(Text.literal("你已收到來自 " + playerName + " 的隊伍邀請，你有 60 秒的時間來使用 ")
                        .setStyle(Style.EMPTY.withColor(0x55FF55))
                        .append(Text.literal("/party accept " + playerName).setStyle(Style.EMPTY.withColor(0xFFFF55)
                                .withClickEvent(new ClickEvent.RunCommand("/party accept " + playerName))
                                .withHoverEvent(new HoverEvent.ShowText(Text.literal("點擊這裡接受邀請")))))
                        .append(Text.literal(" 來加入").setStyle(Style.EMPTY.withColor(0x55FF55))));
                return 1;
            });
            responseActionMap.put(ResponseCode.PARTY_MISSING_PERMISSION, new ErrorResponseAction("你沒有權限邀請玩家加入隊伍。"));
            responseActionMap.put(ResponseCode.PARTY_ALREADY_IN, (player, target, playerManager, jsonResponse) ->
            {
                player.sendMessage(Text.literal(target.getName().getString() + " 已經在你的隊伍中。").setStyle(ERROR_MESSAGE_STYLE));
                return 0; //使用 player 參數 因此不能用 ErrorResponseAction
            });
            responseActionMap.put(ResponseCode.PARTY_ALREADY_HAVE, (player, target, playerManager, jsonResponse) ->
            {
                player.sendMessage(Text.literal(target.getName().getString() + " 已經在其他隊伍中。").setStyle(ERROR_MESSAGE_STYLE));
                return 0; //使用 player 參數 因此不能用 ErrorResponseAction
            });
            responseActionMap.put(ResponseCode.PARTY_LOCKED, new ErrorResponseAction("正在匹配中，無法邀請玩家。"));
        }

        @Override
        protected String createBody(ServerPlayerEntity player, ServerPlayerEntity target)
        {
            return modifiableBody.put("player", player.getUuidAsString())
                    .put("target", target.getUuidAsString())
                    .put("expire_at", System.currentTimeMillis() + 60000)
                    .toString();
        }
    }

    private static class AcceptAction extends NeedPlayerAction
    {
        private AcceptAction()
        {
            super("accept");
            responseActionMap.put(ResponseCode.OK, (player, inviter, playerManager, jsonResponse) ->
            {
                player.sendMessage(Text.literal("你加入了 " + inviter.getName().getString() + " 的隊伍。")
                        .setStyle(Style.EMPTY.withColor(0x55FF55)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                for (int i = 0, len = members.length(); i < len; i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player)
                        member.sendMessage(Text.literal(player.getName().getString() + " 已加入隊伍。")
                                .setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                return 1;
            });
            responseActionMap.put(ResponseCode.PARTY_ALREADY_HAVE, new ErrorResponseAction("你已經在其他隊伍中。"));
            responseActionMap.put(ResponseCode.PARTY_INVITE_NOT_FOUND, new ErrorResponseAction("找不到邀請，請確保你已被邀請加入隊伍。"));
        }

        @Override
        protected String createBody(ServerPlayerEntity player, ServerPlayerEntity inviter)
        {
            return modifiableBody.put("player", player.getUuidAsString())
                    .put("inviter", inviter.getUuidAsString())
                    .toString();
        }
    }

    private static class KickAction extends NeedPlayerAction
    {
        private KickAction()
        {
            super("kick");
            responseActionMap.put(ResponseCode.OK, (player, target, playerManager, jsonResponse) ->
            {
                player.sendMessage(Text.literal("成功踢出 " + target.getName().getString() + " 出隊伍。")
                        .setStyle(Style.EMPTY.withColor(0x55FF55)));
                target.sendMessage(Text.literal("你已被踢出隊伍。").setStyle(Style.EMPTY.withColor(0xFF5555)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                for (int i = 0, len = members.length(); i < len; i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player && member != target)
                        member.sendMessage(Text.literal(target.getName().getString() + " 已離開隊伍。")
                                .setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                return 1;
            });
            responseActionMap.put(ResponseCode.PARTY_NOT_FOUND, new ErrorResponseAction("你不在任何隊伍中。"));
            responseActionMap.put(ResponseCode.PARTY_MISSING_PERMISSION, new ErrorResponseAction("你沒有權限踢出玩家。"));
            responseActionMap.put(ResponseCode.PARTY_NOT_FOUND_PLAYER, new ErrorResponseAction("找不到目標玩家。"));
            responseActionMap.put(ResponseCode.PARTY_LOCKED, new ErrorResponseAction("正在匹配中，無法踢出玩家。"));
        }

        @Override
        protected String createBody(ServerPlayerEntity player, ServerPlayerEntity target)
        {
            return modifiableBody.put("player", player.getUuidAsString())
                    .put("target", target.getUuidAsString())
                    .toString();
        }
    }

    private static class TransferAction extends NeedPlayerAction
    {
        private TransferAction()
        {
            super("transfer");
            responseActionMap.put(ResponseCode.OK, (player, target, playerManager, jsonResponse) ->
            {
                String targetName = target.getName().getString();
                player.sendMessage(Text.literal("已成功將隊伍領導權轉移給 " + targetName + ".").setStyle(Style.EMPTY.withColor(0x55FF55)));
                target.sendMessage(Text.literal("你已成為隊伍的新領導者。").setStyle(Style.EMPTY.withColor(0x55FF55)));
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                for (int i = 0, len = members.length(); i < len; i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null) continue;
                    if (member != player && member != target)
                        member.sendMessage(Text.literal(player.getName().getString() + " 已將隊伍領導權轉移給 " + targetName + ".")
                                .setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                return 1;
            });
            responseActionMap.put(ResponseCode.PARTY_MISSING_PERMISSION, new ErrorResponseAction("你沒有權限轉移隊伍領導權。"));
            responseActionMap.put(ResponseCode.PARTY_NOT_FOUND, new ErrorResponseAction("你不在任何隊伍中。"));
            responseActionMap.put(ResponseCode.PARTY_LOCKED, new ErrorResponseAction("正在匹配中，無法轉移隊伍領導權。"));
        }

        @Override
        protected String createBody(ServerPlayerEntity player, ServerPlayerEntity target)
        {
            return modifiableBody.put("player", player.getUuidAsString())
                    .put("target", target.getUuidAsString())
                    .toString();
        }
    }

    private static class ListAction extends RunAction
    {
        private ListAction()
        {
            super("list");
            responseActionMap.put(ResponseCode.OK, (player, target, playerManager, jsonResponse) ->
            {
                JSONObject receivedData = jsonResponse.getJSONObject("data");
                JSONArray members = receivedData.getJSONArray("members");
                ServerPlayerEntity leader = playerManager.getPlayer(UUID.fromString(receivedData.getString("leader")));
                int offlineCount = 0;
                player.sendMessage(Text.literal("隊伍成員列表：").setStyle(Style.EMPTY.withColor(0x55FFFF)));
                for (int i = 0, len = members.length(); i < len; i++) {
                    ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(members.getString(i)));
                    if (member == null)
                        offlineCount++;
                    else if (member == player)
                        player.sendMessage(Text.literal(" - " + member.getName().getString()).setStyle(Style.EMPTY.withColor(0xFFAA00)));
                    else if (member == leader)
                        player.sendMessage(Text.literal(" - " + member.getName().getString()).setStyle(Style.EMPTY.withColor(0x5555FF)));
                    else
                        player.sendMessage(Text.literal(" - " + member.getName().getString()).setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                if (offlineCount > 0)
                    player.sendMessage(Text.literal(" - " + offlineCount + "名成員已離線").setStyle(Style.EMPTY.withColor(0xAAAAAA)));
                return 1;
            });
            responseActionMap.put(ResponseCode.PARTY_NOT_FOUND, new ErrorResponseAction("你不在任何隊伍中。"));
        }

        @Override
        protected String createBody(ServerPlayerEntity player, ServerPlayerEntity target)
        {
            return new URIBuilder()
                    .addParam("player", player.getUuidAsString())
                    .build()
                    .toString();
        }
    }

    private static class LeaveAction extends RunAction
    {
        private LeaveAction()
        {
            super("leave");
            responseActionMap.put(ResponseCode.OK, (player, target, playerManager, jsonResponse) ->
            {
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
            });
            responseActionMap.put(ResponseCode.PARTY_MISSING_PERMISSION, new ErrorResponseAction("你沒有權限離開隊伍，請使用 /party transfer 轉移隊伍領導權，或 /party disband 來解散隊伍。"));
            responseActionMap.put(ResponseCode.PARTY_LOCKED, new ErrorResponseAction("正在匹配中，無法離開隊伍。"));
        }

        @Override
        protected String createBody(ServerPlayerEntity player, ServerPlayerEntity target)
        {
            return modifiableBody.put("player", player.getUuidAsString()).toString();
        }
    }

    private static final class DisbandAction extends RunAction
    {
        public DisbandAction()
        {
            super("disband");
            responseActionMap.put(ResponseCode.OK, (player, target, playerManager, jsonResponse) ->
            {
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
            });
            responseActionMap.put(ResponseCode.PARTY_NOT_FOUND, new ErrorResponseAction("你不在任何隊伍中。"));
            responseActionMap.put(ResponseCode.PARTY_LOCKED, new ErrorResponseAction("正在匹配中，無法解散隊伍。"));
        }

        @Override
        protected String createBody(ServerPlayerEntity player, ServerPlayerEntity target)
        {
            return modifiableBody.put("player", player.getUuidAsString()).toString();
        }
    }
}
