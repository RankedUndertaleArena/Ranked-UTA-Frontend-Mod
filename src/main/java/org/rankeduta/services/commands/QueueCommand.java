package org.rankeduta.services.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.json.JSONObject;
import org.rankeduta.HTTPClient;
import org.rankeduta.RankedUTA;
import org.rankeduta.services.commands.Command.ICommand;

import java.net.http.HttpResponse;

public class QueueCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register the queue command here
        dispatcher.register(CommandManager.literal("queue")
            .then(CommandManager.literal("solo")
                .executes(new ExecuteSolo()))
            .then(CommandManager.literal("duo")
                .executes(new ExecuteDuo()))
            .then(CommandManager.literal("squad")
                .executes(new ExecuteSquad()))
            .then(CommandManager.literal("siege")
                .executes(new ExecuteSiege()))
        );
    }

    private static class ExecuteSolo implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerPlayerEntity sender = context.getSource().getPlayer();
            String queueData = new JSONObject()
                .put("player", sender.getUuid().toString())
                .put("mode", "solo").toString();
            HttpResponse<String> response = HTTPClient.post("/queue", queueData);
            if (response != null && response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String receivedMessage = jsonResponse.get("message").toString();
                RankedUTA.LOGGER.info("Successfully received from Backend Server: {}", receivedMessage);
                return 1; // Return success code
            } else {
                RankedUTA.LOGGER.error("Failed to send {} to join solo queue: {}", sender.getName().getString(), response != null ? response.body() : "No response");
                return 0; // Return failure code
            }
        }
    }

    private static class ExecuteDuo implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerPlayerEntity sender = context.getSource().getPlayer();
            String queueData = new JSONObject()
                .put("player", sender.getUuid().toString())
                .put("mode", "duo").toString();
            HttpResponse<String> response = HTTPClient.post("/queue", queueData);
            if (response != null && response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String receivedMessage = jsonResponse.get("message").toString();
                RankedUTA.LOGGER.info("Successfully received from Backend Server: {}", receivedMessage);
                return 1; // Return success code
            } else {
                RankedUTA.LOGGER.error("Failed to send {} to join duo queue: {}", sender.getName().getString(), response != null ? response.body() : "No response");
                return 0; // Return failure code
            }
        }
    }

    private static class ExecuteSquad implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerPlayerEntity sender = context.getSource().getPlayer();
            String queueData = new JSONObject()
                .put("player", sender.getUuid().toString())
                .put("mode", "squad").toString();
            HttpResponse<String> response = HTTPClient.post("/queue", queueData);
            if (response != null && response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String receivedMessage = jsonResponse.get("message").toString();
                RankedUTA.LOGGER.info("Successfully received from Backend Server: {}", receivedMessage);
                return 1; // Return success code
            } else {
                RankedUTA.LOGGER.error("Failed to send {} to join squad queue: {}", sender.getName().getString(), response != null ? response.body() : "No response");
                return 0; // Return failure code
            }
        }
    }

    private static class ExecuteSiege implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerPlayerEntity sender = context.getSource().getPlayer();
            String queueData = new JSONObject()
                .put("player", sender.getUuid().toString())
                .put("mode", "siege").toString();
            HttpResponse<String> response = HTTPClient.post("/queue", queueData);
            if (response != null && response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                String receivedMessage = jsonResponse.get("message").toString();
                RankedUTA.LOGGER.info("Successfully received from Backend Server: {}", receivedMessage);
                return 1; // Return success code
            } else {
                RankedUTA.LOGGER.error("Failed to send {} to join siege queue: {}", sender.getName().getString(), response != null ? response.body() : "No response");
                return 0; // Return failure code
            }
        }
    }
}
