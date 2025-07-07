package org.rankeduta.features.services;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.bson.Document;
import org.rankeduta.RankedUTA;
import org.rankeduta.defines.Party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PartyService {
    private final MongoDatabase mongoDatabase;

    public PartyService(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    public Party getPartyByLeader(String leader) {
        Document partyDoc = mongoDatabase.getCollection("parties")
            .find(new Document("leader", leader))
            .first();
        return partyDoc != null ? Party.fromDocument(partyDoc) : null;
    }

    public Party getPartyByMember(String member) {
        Document partyDoc = mongoDatabase.getCollection("parties")
            .find(new Document("members", member))
            .first();
        return partyDoc != null ? Party.fromDocument(partyDoc) : null;
    }

    public List<Party> getAllParties() {
        List<Document> partyDocs = mongoDatabase.getCollection("parties").find().into(new ArrayList<>());
        return partyDocs.stream().map(Party::fromDocument).collect(Collectors.toList());
    }

    public void removeExpiredInvites(Party party, MinecraftServer server) {
        long currentTime = System.currentTimeMillis();
        if (party == null || party.getInvites().isEmpty()) return;
        party.getInvites().entrySet().removeIf(entry -> {
            if (entry.getValue() < currentTime) {
                ServerPlayerEntity inviter = server.getPlayerManager().getPlayer(UUID.fromString(party.getLeader()));
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(UUID.fromString(entry.getKey()));
                if (player != null) {
                    player.sendMessage(Text.literal(inviter.getName().getString() + " 的隊伍邀請已過期。").withColor(0xFF5555));
                    RankedUTA.LOGGER.info("Removed expired invites for party ({})", party.getLeader());
                }
                return true;
            }
            return false;
        });

        // Save the updated party back to the database
        saveParty(party);
    }

    public void handleLeaderOffline(ServerPlayerEntity player, PartyService partyService) {
        MinecraftServer server = player.getServer();
        Party party = partyService.getPartyByMember(player.getUuidAsString());

        if (party == null) return;
        if (!party.getLeader().equals(player.getUuidAsString())) return;
        int memberCount = party.getMembers().size();
        if (memberCount == 1) {
            // If the party has only one member, delete the party
            partyService.deleteParty(player.getUuidAsString());
            return;
        }
        // If the leader is offline, transfer leadership to another member
        for (String memberUUID : party.getMembers()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(UUID.fromString(memberUUID));
            if (member != null && member != player) {
                party.setLeader(memberUUID);
                partyService.saveParty(party);
                member.sendMessage(Text.literal("隊長已離線，你已成為新的隊長！").withColor(0x55FF55));
                for (String partyMemberUUID : party.getMembers()) {
                    ServerPlayerEntity partyMember = server.getPlayerManager().getPlayer(UUID.fromString(partyMemberUUID));
                    if (partyMember != null && !partyMember.equals(member)) {
                        partyMember.sendMessage(Text.literal("隊長已離線，" + member.getName().getString() + " 現在是新的隊長！").withColor(0x55FF55));
                    }
                }
                return;
            }
        }
    }

    public void saveParty(Party party) {
        mongoDatabase.getCollection("parties").replaceOne(
            new Document("leader", party.getLeader()),
            party.toDocument(),
            new ReplaceOptions().upsert(true));
    }

    public void deleteParty(String leader) {
        mongoDatabase.getCollection("parties").deleteOne(new Document("leader", leader));
    }
}
