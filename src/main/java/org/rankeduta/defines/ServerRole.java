package org.rankeduta.defines;

public enum ServerRole {
    unknown,
    lobby,
    match;
    
    public static ServerRole fromString(String value) {
        for(ServerRole role : ServerRole.values()) {
            if(role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        return unknown;
    }
  
}
