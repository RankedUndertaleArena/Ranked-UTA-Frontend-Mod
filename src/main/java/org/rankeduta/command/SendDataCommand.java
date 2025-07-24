package org.rankeduta.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.DataCommandStorage;
import net.minecraft.command.argument.NbtCompoundArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.command.handler.SendDataHandler;

import java.util.ArrayList;
import java.util.List;

public class SendDataCommand implements Command.ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register the /send_data command
        SendDataHandler handler = new SendDataHandler();
        dispatcher.register(CommandManager.literal("senddata")
            .then(CommandManager.argument("type", StringArgumentType.string())
                .suggests((context, builder) -> {
                    List<String> types = new ArrayList<>();
                    types.add("player_setting");
                    types.add("game_stats");

                    types.forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .then(CommandManager.argument("nbt", NbtCompoundArgumentType.nbtCompound())
                    .executes(handler::execute)
                )
                .then(CommandManager.argument("storage", StringArgumentType.string())
                    .suggests((context, builder) -> {
                        DataCommandStorage server = context.getSource().getServer().getDataCommandStorage();
                        server.getIds().forEach(id -> {
                            String namespace = id.getNamespace();
                            server.get(id).forEach((string, nbt) ->
                                builder.suggest(namespace + ":" + string));
                        });
                        return builder.buildFuture();
                    })
                    .executes(handler::execute)
                    .then(CommandManager.argument("key", StringArgumentType.string())
                        .executes(handler::execute)
                    )
                )
            )
        );
    }
}
