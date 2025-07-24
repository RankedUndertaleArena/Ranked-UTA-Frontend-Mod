package org.rankeduta.define;

public enum ServerRole {
    UNKNOWN("unknown"),
    LOBBY("lobby"),
    MATCH("match");

    private final String role;

    ServerRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role.toLowerCase();
    }

    public static ServerRole fromString(String role) {
        for (ServerRole serverRole : ServerRole.values()) {
            if (serverRole.getRole().equalsIgnoreCase(role)) {
                return serverRole;
            }
        }
        return UNKNOWN;
    }
}
