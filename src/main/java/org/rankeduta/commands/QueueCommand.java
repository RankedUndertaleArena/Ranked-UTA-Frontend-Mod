package org.rankeduta.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.json.JSONArray;
import org.json.JSONObject;
import org.rankeduta.HTTPClient;
import org.rankeduta.defines.ResponseCode;
import org.rankeduta.commands.Command.ICommand;

import java.net.http.HttpResponse;
import java.util.UUID;

public class QueueCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register the queue command here
        dispatcher.register(CommandManager.literal("queue")
            .then(CommandManager.literal("solo")
                .executes(new ExecuteQueue("solo")))
            .then(CommandManager.literal("duo")
                .executes(new ExecuteQueue("duo")))
            .then(CommandManager.literal("squad")
                .executes(new ExecuteQueue("squad")))
            .then(CommandManager.literal("siege")
                .executes(new ExecuteQueue("siege")))
        );
    }

    private record ExecuteQueue(String mode) implements Command<ServerCommandSource> {
        @Override
            public int run(CommandContext<ServerCommandSource> context) {
                try {
                    PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
                    ServerPlayerEntity sender = context.getSource().getPlayer();
                    if (sender == null) return 0;
                    String body = new JSONObject()
                        .put("player", sender.getUuidAsString())
                        .put("mode", mode)
                        .toString();
                    HttpResponse<String> response = HTTPClient.post("/queue", body);
                    JSONObject jsonResponse = HTTPClient.receivedResponse(response);
                    if (jsonResponse == null) return 0;
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
                    context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
                }
                return 0;
            }
        }
}
