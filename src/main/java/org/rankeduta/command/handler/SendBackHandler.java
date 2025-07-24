package org.rankeduta.command.handler;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.json.JSONObject;
import org.rankeduta.command.Command;
import org.rankeduta.service.BackendService;

import static org.rankeduta.RankedUTA.SERVER_UUID;

public class SendBackHandler implements Command.IHandler {
    @Override
    public int execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String body = new JSONObject()
            .put("server", SERVER_UUID.toString())
            .toString();
        JSONObject jsonResponse = BackendService.receivedResponse(BackendService.sendRequest("post", "/game/return", body));
        if (jsonResponse == null) {
            source.sendError(Text.literal("無法連接到後端伺服器，請檢查伺服器是否已啟動"));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("正在返回主伺服器..."), false);
        return 1;
    }
}
