package org.rankeduta.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.DataCommandStorage;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.commands.handlers.SendStorageHandler;
import org.rankeduta.events.CommandInit.ICommand;

public class SendStorageCommand implements ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 註冊 send_storage 指令
        dispatcher.register(CommandManager.literal("send_storage")
            .then(CommandManager.argument("storage", NbtCompoundArgumentType.nbtCompound())
                .suggests((context, builder) -> {
                    DataCommandStorage server = context.getSource().getServer().getDataCommandStorage();
                    server.getIds().forEach(id -> {
                        String namespace = id.getNamespace();
                        server.get(id).forEach((string, nbt) ->
                            builder.suggest(namespace + ":" + string));
                    });
                    return builder.buildFuture();
                })
                .executes(new SendStorageHandler.executeSendStorage())
                .then(CommandManager.argument("path", StringArgumentType.string())
                    .executes(new SendStorageHandler.executeSendStorage()))
            )
        );
    }
}