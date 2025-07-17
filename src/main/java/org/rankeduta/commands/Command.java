package org.rankeduta.commands;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

public class Command {
    public interface ICommand {
        void register(CommandDispatcher<ServerCommandSource> dispatcher);
    }

    private static final List<ICommand> commands = new ArrayList<>();

    static {
        commands.add(new PartyCommand());
        commands.add(new QueueCommand());
    }

    public static void register(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            for(ICommand command : commands) {
                command.register(dispatcher);
            }
        });
    }
}