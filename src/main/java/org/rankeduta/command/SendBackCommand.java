package org.rankeduta.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.command.handler.SendBackHandler;

public class SendBackCommand implements Command.ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        SendBackHandler handler = new SendBackHandler();
        dispatcher.register(CommandManager.literal("sendback")
                .requires(source -> source.hasPermissionLevel(2)) // 需要管理員權限
                .executes(handler::execute)
        );
    }
}
