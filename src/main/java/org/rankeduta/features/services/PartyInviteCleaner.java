package org.rankeduta.features.services;

import net.minecraft.server.MinecraftServer;
import org.rankeduta.RankedUTA;
import org.rankeduta.defines.Party;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PartyInviteCleaner {
    private final PartyService partyService;
    private final MinecraftServer minecraftServer;
    private final ScheduledExecutorService scheduler;

    public PartyInviteCleaner(PartyService partyService, MinecraftServer minecraftServer) {
        this.partyService = partyService;
        this.minecraftServer = minecraftServer;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void scheduleCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                for (Party party : partyService.getAllParties()) {
                    if (party != null || !party.getInvites().isEmpty()) {
                        partyService.removeExpiredInvites(party, minecraftServer);
                    }
                }
            } catch (Exception e) {
                RankedUTA.LOGGER.error("Error while cleaning expired party invites: {}", e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void stopCleanup() {
        if (!scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }
}
