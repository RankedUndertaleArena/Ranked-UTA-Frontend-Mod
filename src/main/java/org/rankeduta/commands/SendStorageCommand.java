package org.rankeduta.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.commands.handlers.SendStorageHandler;
import org.rankeduta.events.CommandInit.ICommand;

public class SendStorageCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 註冊 send_storage 指令
        dispatcher.register(CommandManager.literal("send_storage")
            .then(CommandManager.argument("storage", StringArgumentType.string())
                .suggests((context, builder) -> {
                    // 提供存儲名稱的建議
                    MinecraftServer server = context.getSource().getServer();
                    server.getDataCommandStorage().getIds().forEach(id -> builder.suggest(id.getPath()));
                    return builder.buildFuture();
                })
                .then(CommandManager.argument("path", StringArgumentType.string())
                    .executes(new SendStorageHandler.executeSendStorage()))
                .executes(new SendStorageHandler.executeSendStorage()))
        );
    }
}
