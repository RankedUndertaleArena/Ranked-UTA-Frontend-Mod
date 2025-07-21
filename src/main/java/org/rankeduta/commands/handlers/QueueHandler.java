package org.rankeduta.commands.handlers;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.HTTPClient;
import org.rankeduta.defines.ResponseCode;

import java.net.http.HttpResponse;
import java.util.UUID;

public class QueueHandler {
    public record executeQueue(String mode) implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerCommandSource source = context.getSource();
            PlayerManager playerManager = source.getServer().getPlayerManager();
            try {
                ServerPlayerEntity sender = source.getPlayer();
                if (sender == null) {
                    source.sendError(Text.literal("此指令只能由玩家執行"));
                    return 0;
                }
                String body = new JSONObject()
                    .put("player", sender.getUuidAsString())
                    .put("mode", mode)
                    .toString();
                HttpResponse<String> response = HTTPClient.post("/queue", body);
                JSONObject jsonResponse = HTTPClient.receivedResponse(response);
                if (jsonResponse == null) {
                    source.sendError(Text.literal("無法連接到伺服器或伺服器回應錯誤"));
                    return 0;
                }
                ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
                switch (statusCode) {
                    case OK -> {
                        JSONObject data = jsonResponse.optJSONObject("data");
                        String mode = data.getString("mode");
                        JSONArray members = data.optJSONArray("players");
                        for (int i = 0; i < members.length(); i++) {
                            UUID uuid = UUID.fromString(members.getString(i));
                            ServerPlayerEntity member = playerManager.getPlayer(uuid);
                            if (member == null) continue;
                            if (mode.equalsIgnoreCase("leave"))
                                member.sendMessage(Text.literal("You " + mode + " queue...").setStyle(Style.EMPTY.withColor(0x55FF55)));
                            else
                                member.sendMessage(Text.literal("You joined " + mode + " queue...").setStyle(Style.EMPTY.withColor(0x55FF55)));
                        }
                        return 1;
                    }
                    case QUEUE_MISSING_PERMISSIONS -> sender.sendMessage(Text.literal("You are not party leader.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                    case QUEUE_MAX_PLAYER_LIMITED -> sender.sendMessage(Text.literal("Your party member count just out of limit.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                }
            } catch (Exception e) {
                source.sendError(Text.literal("發生預期外錯誤")
                    .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal(e.getMessage())))));
            }
            return 0;
        }
    }
}
