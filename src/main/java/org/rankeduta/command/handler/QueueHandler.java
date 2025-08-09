package org.rankeduta.command.handler;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.command.Command;
import org.rankeduta.define.ResponseCode;
import org.rankeduta.service.BackendService;

import java.util.UUID;

public class QueueHandler implements Command.IHandler {
    @Override
    public int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        PlayerManager playerManager = source.getServer().getPlayerManager();
        String mode = StringArgumentType.getString(context, "mode");

        if (player == null) {
            source.sendError(Text.literal("你必須在遊戲中才能使用此命令。"));
            return 0;
        }

        if (mode == null || mode.isEmpty()) {
            player.sendMessage(Text.literal("請指定匹配隊列模式。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }

        String body = new JSONObject()
            .put("player", player.getUuidAsString())
            .put("mode", mode)
            .toString();

        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/queue", body));
        if (jsonResponse == null) {
            player.sendMessage(Text.literal("無法連接到後端服務器，請稍後再試。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            return 0;
        }
        switch (ResponseCode.fromCode(jsonResponse.getInt("status"))) {
            case OK -> {
                JSONObject data = jsonResponse.optJSONObject("data");
                JSONArray members = data.optJSONArray("players");
                mode = data.getString("mode");
                for (int i = 0; i < members.length(); i++) {
                    UUID uuid = UUID.fromString(members.getString(i));
                    ServerPlayerEntity member = playerManager.getPlayer(uuid);
                    if (member == null) continue;
                    if (mode.equalsIgnoreCase("leave"))
                        member.sendMessage(Text.literal("你已離開匹配隊列...").setStyle(Style.EMPTY.withColor(0x55FF55)));
                    else
                        member.sendMessage(Text.literal("你已加入 " + mode + " 匹配隊列").setStyle(Style.EMPTY.withColor(0x55FF55)));
                }
                return 1;
            }
            case QUEUE_MISSING_PERMISSION ->
                player.sendMessage(Text.literal("你沒有權限使用此命令。").setStyle(Style.EMPTY.withColor(0xFF5555)));
            case QUEUE_MAX_PLAYER_LIMITED ->
                player.sendMessage(Text.literal("超過匹配隊列人數限制。").setStyle(Style.EMPTY.withColor(0xFF5555)));
        }
        return 0;
    }
}
