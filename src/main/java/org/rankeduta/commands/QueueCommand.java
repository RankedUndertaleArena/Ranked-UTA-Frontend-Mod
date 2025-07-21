package org.rankeduta.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.commands.handlers.QueueHandler;
import org.rankeduta.events.CommandInit.ICommand;

public class QueueCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 註冊 queue 指令
        dispatcher.register(CommandManager.literal("queue")
            .then(CommandManager.literal("solo")
                .executes(new QueueHandler.executeQueue("solo")))
            .then(CommandManager.literal("duo")
                .executes(new QueueHandler.executeQueue("duo")))
            .then(CommandManager.literal("squad")
                .executes(new QueueHandler.executeQueue("squad")))
            .then(CommandManager.literal("siege")
                .executes(new QueueHandler.executeQueue("siege")))
        );
    }
}
