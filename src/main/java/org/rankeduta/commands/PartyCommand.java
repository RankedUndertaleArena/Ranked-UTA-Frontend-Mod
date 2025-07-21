package org.rankeduta.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.commands.handlers.PartyHandler;
import org.rankeduta.events.CommandInit.ICommand;

public class PartyCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 註冊 party 指令
        dispatcher.register(CommandManager.literal("party")
            .then(CommandManager.literal("invite")
                .then(CommandManager.argument("player", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        // 建議玩家名稱供邀請指令使用
                        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            if (player == context.getSource().getPlayer()) return; // Skip self
                            builder.suggest(player.getName().getString());
                        });
                        return builder.buildFuture();
                    })
                    .executes(new PartyHandler.executeInvite())))
            .then(CommandManager.literal("accept")
                .then(CommandManager.argument("player", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        // 建議玩家名稱供接受邀請指令使用
                        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            if (player == context.getSource().getPlayer()) return; // Skip self
                            builder.suggest(player.getName().getString());
                        });
                        return builder.buildFuture();
                    })
                    .executes(new PartyHandler.executeAccept())))
            .then(CommandManager.literal("kick")
                .then(CommandManager.argument("player", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        // 建議玩家名稱供踢出指令使用
                        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            if (player == context.getSource().getPlayer()) return; // Skip self
                            builder.suggest(player.getName().getString());
                        });
                        return builder.buildFuture();
                    })
                    .executes(new PartyHandler.executeKick())))
            .then(CommandManager.literal("transfer")
                .then(CommandManager.argument("player", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        // 建議玩家名稱供轉移指令使用
                        context.getSource().getServer().getPlayerManager().getPlayerList().forEach(player -> {
                            if (player == context.getSource().getPlayer()) return; // Skip self
                            builder.suggest(player.getName().getString());
                        });
                        return builder.buildFuture();
                    })
                    .executes(new PartyHandler.executeTransfer())))
            .then(CommandManager.literal("leave")
                .executes(new PartyHandler.executeLeave()))
            .then(CommandManager.literal("list")
                .executes(new PartyHandler.executeList()))
            .then(CommandManager.literal("disband")
                .executes(new PartyHandler.executeDisband()))
        );
    }
}
