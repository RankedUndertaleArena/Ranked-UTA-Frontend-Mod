package org.rankeduta.features.commands;

import com.mongodb.client.MongoDatabase;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.rankeduta.RankedUTA;
import org.rankeduta.features.services.PartyService;

public class CommandInit {
    public static void registerCommands(MongoDatabase database) {
        PartyService partyService = new PartyService(database);
        PartyCommand partyCommand = new PartyCommand(partyService);

        // Register commands here
        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> {
                // Register your commands here
                partyCommand.register(dispatcher);
            }
        );
        RankedUTA.LOGGER.info("Commands have been registered successfully.");
    }
}
