package org.rankeduta.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import org.rankeduta.command.handler.QueueHandler;

public class QueueCommand implements Command.ICommand {
    @Override
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        String[] modes = {"solo","duo","squad","siege"};
        // Register the /queue command
        QueueHandler handler = new QueueHandler();
        dispatcher.register(CommandManager.literal("queue")
            .then(CommandManager.argument("mode", StringArgumentType.string())
                .suggests((context, builder) -> {
                    for (String mode : modes)
                        builder.suggest(mode);
                    return builder.buildFuture();
                })
                .executes(handler::execute)
            )
        );
    }
}
