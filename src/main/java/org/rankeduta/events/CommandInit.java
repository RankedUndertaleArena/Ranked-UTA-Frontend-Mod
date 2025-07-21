package org.rankeduta.events;

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.RankedUTA;
import org.rankeduta.commands.PartyCommand;
import org.rankeduta.commands.QueueCommand;
import org.rankeduta.commands.SendStorageCommand;

import static org.rankeduta.RankedUTA.serverRole;

public class CommandInit {
    public interface ICommand {
        void register(CommandDispatcher<ServerCommandSource> dispatcher);
    }

    private static final List<ICommand> commands = new ArrayList<>();

    static {
        commands.add(new PartyCommand());
        commands.add(new QueueCommand());
        commands.add(new SendStorageCommand());
    }

    public static void register(){
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            switch (serverRole) {
                case lobby -> {
                    for (ICommand command : commands) {
                        command.register(dispatcher);
                    }
                }
                case match -> new SendStorageCommand().register(dispatcher);
                default -> RankedUTA.LOGGER.warn("未知的伺服器角色：{}，無法註冊指令", serverRole);
            }
        });
    }
}