package org.rankeduta.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.command.handler.QueueHandler;

import java.util.ArrayList;
import java.util.List;

public class QueueCommand implements Command.ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // Register the /queue command
        QueueHandler handler = new QueueHandler();
        dispatcher.register(CommandManager.literal("queue")
            .then(CommandManager.argument("mode", StringArgumentType.string())
                .suggests((context, builder) -> {
                    List<String> modes = new ArrayList<>();
                    modes.add("solo");
                    modes.add("duo");
                    modes.add("squad");
                    modes.add("siege");
                    modes.forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(handler::execute)
            )
        );
    }
}
