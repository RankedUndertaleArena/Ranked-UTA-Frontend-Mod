package org.rankeduta.features.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.rankeduta.features.commands.handlers.PartyCommandHandler;
import org.rankeduta.features.services.PartyService;

public class PartyCommand {
    private final PartyService partyService;

    public PartyCommand(PartyService partyService) {
        this.partyService = partyService;
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register party-related commands here
        dispatcher.register(CommandManager.literal("party")
            .then(CommandManager.literal("invite")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                    .executes((context) -> {
                        ServerPlayerEntity sender = context.getSource().getPlayer();
                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");

                        PartyCommandHandler.invite(sender, target, partyService);
                        return 1;
                    })
                )
            )
            .then(CommandManager.literal("accept")
                .then(CommandManager.argument("inviter", EntityArgumentType.player())
                    .executes((context) -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        ServerPlayerEntity inviter = EntityArgumentType.getPlayer(context, "inviter");

                        PartyCommandHandler.accept(player, inviter, partyService);
                        return 1;
                    })
                )
            )
            .then(CommandManager.literal("kick")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                    .executes((context) -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");

                        PartyCommandHandler.kick(player, target, partyService);
                        return 1;
                    })
                )
            )
            .then(CommandManager.literal("transfer")
                .then(CommandManager.argument("target", EntityArgumentType.player())
                    .executes((context) -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");

                        PartyCommandHandler.transfer(player, target, partyService);
                        return 1;
                    })
                )
            )
            .then(CommandManager.literal("list")
                .executes((context) -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();

                    PartyCommandHandler.list(player, partyService);
                    return 1;
                })
            )
            .then(CommandManager.literal("leave")
                .executes((context) -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();

                    PartyCommandHandler.leave(player, partyService);
                    return 1;
                })
            )
            .then(CommandManager.literal("disband")
                .executes((context) -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();

                    PartyCommandHandler.disband(player, partyService);
                    return 1;
                })
            )
        );
    }
}
