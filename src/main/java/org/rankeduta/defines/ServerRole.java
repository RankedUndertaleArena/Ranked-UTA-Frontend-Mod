package org.rankeduta.defines;

public enum ServerRole {
    UNKNOWN("unknown"),
    LOBBY("lobby"),
    MATCH("match");

    private final String SERVER_ROLE;

    ServerRole(String serverRole) {
        this.SERVER_ROLE = serverRole;
    }

    public String getServerRole() {
        return SERVER_ROLE;
    }

    public static ServerRole fromString(String role) {
        for (ServerRole serverRole : ServerRole.values()) {
            if (serverRole.SERVER_ROLE.equalsIgnoreCase(role)) {
                return serverRole;
            }
        }
        return UNKNOWN;
    }
}
