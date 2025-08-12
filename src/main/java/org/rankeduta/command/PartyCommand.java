package org.rankeduta.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.rankeduta.command.handler.PartyHandler;

import java.util.List;

public class PartyCommand implements Command.ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        String[] suggestions = {"invite", "accept", "kick", "transfer", "disband", "leave", "list"};
        // Register the /party command
        PartyHandler handler = new PartyHandler();
        dispatcher.register(CommandManager.literal("party")
            .then(CommandManager.argument("action", StringArgumentType.string())
                .suggests((context, builder) -> {
                    for (String action : suggestions)
                        builder.suggest(action);
                    return builder.buildFuture();
                })
                .executes(handler::execute)
                .then(CommandManager.argument("target", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        String action = StringArgumentType.getString(context, "action");
                        if (action.equals("invite") || action.equals("accept") || action.equals("kick") || action.equals("transfer")) {
                            ServerCommandSource source = context.getSource();
                            ServerPlayerEntity sender = source.getPlayer();
                            List<ServerPlayerEntity> players = source.getServer().getPlayerManager().getPlayerList();

                            players.forEach(player -> {
                                if (sender == null || player == null) return;
                                if (!player.getUuid().equals(sender.getUuid())) {
                                    builder.suggest(player.getName().getString());
                                }
                            });
                        }
                        return builder.buildFuture();
                    })
                    .executes(handler::execute)
                )
            )
        );
    }
}
