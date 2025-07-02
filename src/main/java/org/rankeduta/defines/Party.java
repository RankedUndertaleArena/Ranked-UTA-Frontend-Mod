package org.rankeduta.defines;

import org.bson.Document;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Party {
    private String leader;
    private Set<String> members;
    private Map<String, Long> invites;

    public Party(String leader) {
        this.leader = leader;
        this.members = new HashSet<>();
        this.invites = new HashMap<>();
        this.members.add(leader);
    }

    public String getLeader() {
        return leader;
    }
    public Set<String> getMembers() {
        return members;
    }
    public Map<String, Long> getInvites() {
        return invites;
    }
    public void setLeader(String leader) {
        this.leader = leader;
    }

    public Document toDocument() {
        return new Document("leader", leader)
            .append("members", members)
            .append("invites", invites);
    }

    public static Party fromDocument(Document doc) {
        String leader = doc.getString("leader");
        Set<String> members = new HashSet<>(doc.getList("members", String.class));
        Map<String, Long> invites = new HashMap<>();

        for (Map.Entry<String, Object> entry : doc.get("invites", Document.class).entrySet()) {
            invites.put(entry.getKey(), ((Number) entry.getValue()).longValue());
        }

        Party party = new Party(leader);
        party.members = members;
        party.invites = invites;

        return party;
    }
}
