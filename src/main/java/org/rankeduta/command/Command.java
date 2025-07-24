package org.rankeduta.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.RankedUTA;
import org.rankeduta.define.ServerRole;

import java.util.ArrayList;
import java.util.List;

import static org.rankeduta.RankedUTA.LOGGER;
import static org.rankeduta.RankedUTA.serverRole;

public class Command {
    public interface ICommand {
        void register(CommandDispatcher<ServerCommandSource> dispatcher);
    }
    public interface IHandler {
        int execute(CommandContext<ServerCommandSource> context);
    }

    private static final List<ICommand> GENERAL_COMMANDS = new ArrayList<>();
    private static final List<ICommand> LOBBY_COMMANDS = new ArrayList<>();
    private static final List<ICommand> MATCH_COMMANDS = new ArrayList<>();

    static {
        LOBBY_COMMANDS.add(new PartyCommand());
        LOBBY_COMMANDS.add(new QueueCommand());
        GENERAL_COMMANDS.add(new SendDataCommand());
        MATCH_COMMANDS.add(new SendBackCommand());
    }

    public static void register(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            if (serverRole.equals(ServerRole.LOBBY)) {
                for (ICommand command : GENERAL_COMMANDS) {
                    command.register(dispatcher);
                }
                for (ICommand command : LOBBY_COMMANDS) {
                    command.register(dispatcher);
                }
            } else if (serverRole.equals(ServerRole.MATCH)) {
                for (ICommand command : GENERAL_COMMANDS) {
                    command.register(dispatcher);
                }
                for (ICommand command : MATCH_COMMANDS) {
                    command.register(dispatcher);
                }
            } else {
                RankedUTA.LOGGER.warn("Server role is not set or unknown, cannot register commands");
            }
        });
    }
}
