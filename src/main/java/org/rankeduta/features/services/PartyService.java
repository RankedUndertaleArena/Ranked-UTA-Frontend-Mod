package org.rankeduta.features.services;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.rankeduta.defines.Party;

public class PartyService {
    private final MongoDatabase mongoDatabase;

    public PartyService(MongoDatabase mongoDatabase) {
        this.mongoDatabase = mongoDatabase;
    }

    public Party getPartyByLeader(String leader) {
        Document partyDoc = mongoDatabase.getCollection("Party")
            .find(new Document("leader", leader))
            .first();
        return partyDoc != null ? Party.fromDocument(partyDoc) : null;
    }

    public Party getPartyByMember(String member) {
        Document partyDoc = mongoDatabase.getCollection("Party")
            .find(new Document("members", member))
            .first();
        return partyDoc != null ? Party.fromDocument(partyDoc) : null;
    }

    public void saveParty(Party party) {
        mongoDatabase.getCollection("Party").replaceOne(
            new Document("leader", party.getLeader()),
            party.toDocument(),
            new ReplaceOptions().upsert(true));
    }

    public void deleteParty(String leader) {
        mongoDatabase.getCollection("Party").deleteOne(new Document("leader", leader));
    }
}
