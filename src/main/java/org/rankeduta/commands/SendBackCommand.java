package org.rankeduta.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.commands.handlers.SendStorageHandler;
import org.rankeduta.events.CommandInit;

public class SendBackCommand implements CommandInit.ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 註冊 send_back 指令
        dispatcher.register(CommandManager.literal("send_back")
            .executes(new SendStorageHandler.executeSendBack())
        );
    }
}
