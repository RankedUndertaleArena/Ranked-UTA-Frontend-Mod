package org.rankeduta.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import org.json.JSONObject;
import org.json.JSONArray;

import org.rankeduta.HTTPClient;
import org.rankeduta.HTTPClient.URIBuilder;
import org.rankeduta.RankedUTA;
import org.rankeduta.defines.ResponseCode;
import org.rankeduta.commands.Command.ICommand;
import com.mojang.brigadier.Command;
import org.rankeduta.defines.TextStyles;
import org.rankeduta.services.PartyService;

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
                return PartyService.invite(sender, target);
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
                return 0;
            }
        }
    }

    private static class ExecuteAccept implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                if (sender == null) return 0;
                PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
                return PartyService.accept(sender, target, playerManager);
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
                return 0;
            }
        }
    }

    private static class ExecuteKick implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                if (sender == null) return 0;
                PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
                return PartyService.kick(sender, target, playerManager);
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage.")
                    .setStyle(TextStyles.ERROR.withHoverEvent(new HoverEvent.ShowText(Text.literal(e.getMessage())))));
                return 0;
            }
        }
    }

    private static class ExecuteTransfer implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
                if (sender == null) return 0;
                PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
                return PartyService.transfer(sender, target, playerManager);
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
                return 0;
            }
        }
    }

    private static class ExecuteLeave implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                if (sender == null) return 0;
                PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
                return PartyService.leave(sender, playerManager);
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
                return 0;
            }
        }
    }

    private static class ExecuteList implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                if (sender == null) return 0;
                PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
                return PartyService.list(sender, playerManager);
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
                return 0;
            }
        }
    }

    private static class ExecuteDisband implements Command<ServerCommandSource> {
        @Override
        public int run(CommandContext<ServerCommandSource> context) {
            try {
                ServerPlayerEntity sender = context.getSource().getPlayer();
                if (sender == null) return 0;
                PlayerManager playerManager = context.getSource().getServer().getPlayerManager();
                return PartyService.disband(sender, playerManager);
            } catch (Exception e) {
                context.getSource().sendError(Text.literal("Player not found or invalid command usage."));
                return 0;
            }
        }
    }
}
