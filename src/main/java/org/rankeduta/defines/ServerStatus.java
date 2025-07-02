package org.rankeduta.defines;

public enum ServerStatus {
    UNKNOWN("unknown"),
    LOBBY("lobby"),
    IDLE("idle"),
    PENDING("pending"),
    MATCH("match"),
    GAME("game");

    private final String SERVER_STATUS;

    ServerStatus(String status) {
        this.SERVER_STATUS = status;
    }

    public String getStatus() {
        return SERVER_STATUS;
    }

    public static ServerStatus fromString(String status) {
        for (ServerStatus serverStatus : ServerStatus.values()) {
            if (serverStatus.SERVER_STATUS.equalsIgnoreCase(status)) {
                return serverStatus;
            }
        }
        return UNKNOWN;
    }
}
