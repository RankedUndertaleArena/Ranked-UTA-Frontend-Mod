package org.rankeduta.services.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import org.json.JSONObject;
import org.json.JSONArray;

import org.rankeduta.HTTPClient;
import org.rankeduta.HTTPClient.URIBuilder;
import org.rankeduta.RankedUTA;
import org.rankeduta.defines.ResponseCode;
import org.rankeduta.services.commands.Command.ICommand;
import com.mojang.brigadier.Command;

import java.net.http.HttpResponse;
import java.util.UUID;

public class PartyCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register the party command here
        dispatcher.register(CommandManager.literal("party")
            .then(CommandManager.literal("invite")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(new ExecuteInvite())))
            .then(CommandManager.literal("accept")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(new ExecuteAccept())))
            .then(CommandManager.literal("kick")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(new ExecuteKick())))
            .then(CommandManager.literal("transfer")
                .then(CommandManager.argument("player", EntityArgumentType.player())
                    .executes(new ExecuteTransfer())))
            .then(CommandManager.literal("leave")
                .executes(new ExecuteLeave()))
            .then(CommandManager.literal("list")
                .executes(new ExecuteList()))
            .then(CommandManager.literal("disband")
                .executes(new ExecuteDisband()))
        );
    }

    private static class ExecuteInvite implements Command<ServerCommandSource>{
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                if (sender == null) return 0;
                if (sender == target) {
                    sender.sendMessage(Text.literal("你不能邀請自己加入隊伍").setStyle(Style.EMPTY.withColor(0xFF5555)));
                    return 0;
                }
                String bodyData = new JSONObject()
                    .put("player", sender.getUuidAsString())
                    .put("target", target.getUuidAsString())
                    .put("expire_at", System.currentTimeMillis() + 60000)
                    .toString();
                HttpResponse<String> response = HTTPClient.post("/party/invite", bodyData);

                if (response != null) {
                    if (response.statusCode() >= 500) RankedUTA.LOGGER.error("Failed to connect the backend server: {}", response.body());
                    else {
                        JSONObject jsonResponse = new JSONObject(response.body());
                        ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
                        switch (statusCode) {
                            case OK:
                                sender.sendMessage(Text.literal("已成功邀請 " + target.getName().getString() + " 加入你的隊伍，他們可以在 60 秒內接受邀請。")
                                    .setStyle(Style.EMPTY.withColor(0x55FF55)));
                                target.sendMessage(Text.literal("你已收到來自 " + sender.getName().getString() + " 的隊伍邀請。你有 60 秒的時間來使用 /party accept " + sender.getName().getString() + " 來加入。")
                                    .setStyle(Style.EMPTY.withColor(0x55FF55).withClickEvent(new ClickEvent.RunCommand("/party accept " + sender.getName().getString()))));
                                return 1;
                            case PARTY_MISSING_PERMISSIONS: sender.sendMessage(Text.literal("You are not party leader.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                            case PARTY_ALREADY_IN: sender.sendMessage(Text.literal(target.getName().getString() + " already in your party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                            case PARTY_ALREADY_HAVE: sender.sendMessage(Text.literal(target.getName().getString() + " already have a party").setStyle(Style.EMPTY.withColor(0xFF5555)));
                        }
                    }
                }
                else RankedUTA.LOGGER.error("No response from the backend server.");
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
            }
            return 0;
        }
    }

    private static class ExecuteAccept implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                if (sender == null) return 0;
                if (sender == target) {
                    sender.sendMessage(Text.literal("You cannot invite yourself to a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                    return 0;
                }
                // Prepare the data to send to the backend server
                String bodyData = new JSONObject()
                    .put("player", sender.getUuidAsString())
                    .put("inviter", target.getUuidAsString())
                    .toString();
                HttpResponse<String> response = HTTPClient.post("/party/accept", bodyData);

                if (response != null) {
                    if (response.statusCode() >= 500) RankedUTA.LOGGER.error("Failed to connect the backend server: {}", response.body());
                    else {
                        JSONObject jsonResponse = new JSONObject(response.body());
                        ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
                        switch (statusCode) {
                            case OK:
                                sender.sendMessage(Text.literal("You have joined " + target.getName().getString() + "'s party.").setStyle(Style.EMPTY.withColor(0x55FF55)));
                                JSONObject receivedData = jsonResponse.getJSONObject("data");
                                RankedUTA.LOGGER.info("The backend server sent data: {}", receivedData.get("members").toString());
                                return 1;
                            case PARTY_ALREADY_HAVE: sender.sendMessage(Text.literal("You already have a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                            case PARTY_INVITE_NOT_FOUND: sender.sendMessage(Text.literal( target.getName().getString() + "'s Party invitation is not found.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                        }
                    }
                }
                else RankedUTA.LOGGER.error("No response from the backend server.");
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
            }
            return 0;
        }
    }

    private static class ExecuteKick implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
            }
            return 0;
        }
    }

    private static class ExecuteTransfer implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                if (sender == null) return 0;
                if (sender == target) {
                    sender.sendMessage(Text.literal("You cannot transfer party leadership to yourself.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                    return 0;
                }
                String partyData = new JSONObject()
                    .put("player", sender.getUuidAsString())
                    .put("target", target.getUuidAsString())
                    .toString();
                HttpResponse<String> response = HTTPClient.post("/party/transfer", partyData);

                if (response != null) {
                    if (response.statusCode() >= 500) RankedUTA.LOGGER.error("Failed to connect the backend server: {}", response.body());
                    else {
                        JSONObject jsonResponse = new JSONObject(response.body());
                        ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
                        switch (statusCode) {
                            case OK:
                                sender.sendMessage(Text.literal("Party Leader transfer to " + target.getName().getString()).setStyle(Style.EMPTY.withColor(0x55FF55)));
                                return 1;
                            case PARTY_MISSING_PERMISSIONS: sender.sendMessage(Text.literal("You already have a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                            case PARTY_NOT_FOUND: sender.sendMessage(Text.literal( target.getName().getString() + "'s Party invitation is not found.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                        }
                    }
                }
                else RankedUTA.LOGGER.error("No response from the backend server.");
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
            }
            return 0;
        }
    }

    private static class ExecuteLeave implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerPlayerEntity sender = context.getSource().getPlayer();
            if (sender == null) return 0;
            String leaderData = new JSONObject()
                .put("player", sender.getUuidAsString())
                .toString();
            HttpResponse<String> response = HTTPClient.post("/party/leave", leaderData);

            if (response != null) {
                if (response.statusCode() >= 500) RankedUTA.LOGGER.error("Failed to connect the backend server: {}", response.body());
                else {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
                    switch (statusCode) {
                        case OK:
                            sender.sendMessage(Text.literal("You have left the party.").setStyle(Style.EMPTY.withColor(0x55FF55)));
                            JSONObject receivedData = jsonResponse.getJSONObject("data");
                            RankedUTA.LOGGER.info("The backend server sent data: {}", receivedData.get("members").toString());
                            return 1;
                        case PARTY_MISSING_PERMISSIONS: sender.sendMessage(Text.literal("You already have a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                    }
                }
            }
            else RankedUTA.LOGGER.error("No response from the backend server.");
            return 0;
        }
    }

    private static class ExecuteList implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerPlayerEntity sender = context.getSource().getPlayer();
            if (sender == null) return 0;
            URIBuilder uriBuilder = new URIBuilder().addParam("player", String.valueOf(sender.getUuidAsString()));
            HttpResponse<String> response = HTTPClient.get("/party/list", uriBuilder);

            if (response != null) {
                if (response.statusCode() >= 500) RankedUTA.LOGGER.error("Failed to connect the backend server: {}", response.body());
                else {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
                    switch (statusCode) {
                        case OK:
                            JSONObject receivedData = jsonResponse.getJSONObject("data");
                            PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
                            ServerPlayerEntity leader = playerManager.getPlayer(UUID.fromString(receivedData.getString("leader")));
                            JSONArray members = receivedData.getJSONArray("members");

                            sender.sendMessage(Text.literal("Party members: ").setStyle(Style.EMPTY.withColor(0x55FF55)));
                            int offlineCount = 0;
                            for (Object m : members.toList()) {
                                ServerPlayerEntity member = playerManager.getPlayer(UUID.fromString(String.valueOf(m)));
                                if (member == null) {
                                    offlineCount++;
                                    continue;
                                }
                                if (member == leader) {
                                    sender.sendMessage(Text.literal(member.getName().getString() + " (Leader)").setStyle(Style.EMPTY.withColor(0xFFFF55)));
                                } else {
                                    sender.sendMessage(Text.literal(member.getName().getString()).setStyle(Style.EMPTY.withColor(0x55FF55)));
                                }
                            }
                            sender.sendMessage(Text.literal(offlineCount + "offline member").setStyle(Style.EMPTY.withColor(0x555555)));
                            return 1;
                        case PARTY_MISSING_PERMISSIONS: sender.sendMessage(Text.literal("You already have a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                    }
                }
            }
            else RankedUTA.LOGGER.error("No response from the backend server.");
            return 0;
        }
    }

    private static class ExecuteDisband implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            ServerPlayerEntity sender = context.getSource().getPlayer();
            if (sender == null) return 0;
            String leaderData = new JSONObject()
                .put("player", sender.getUuidAsString())
                .toString();
            HttpResponse<String> response = HTTPClient.post("/party/disband", leaderData);

            if (response != null) {
                if (response.statusCode() >= 500) RankedUTA.LOGGER.error("Failed to connect the backend server: {}", response.body());
                else {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    ResponseCode statusCode = ResponseCode.fromCode(jsonResponse.getInt("status"));
                    switch (statusCode) {
                        case OK:
                            sender.sendMessage(Text.literal("Party disbanded successfully.").setStyle(Style.EMPTY.withColor(0x55FF55)));
                            JSONObject receivedData = jsonResponse.getJSONObject("data");
                            RankedUTA.LOGGER.info("The backend server sent data: {}", receivedData.get("members").toString());
                            return 1;
                        case PARTY_NOT_FOUND: sender.sendMessage(Text.literal("You already have a party.").setStyle(Style.EMPTY.withColor(0xFF5555)));
                    }
                }
            }
            else RankedUTA.LOGGER.error("No response from the backend server.");
            return 0;
        }
    }
}
