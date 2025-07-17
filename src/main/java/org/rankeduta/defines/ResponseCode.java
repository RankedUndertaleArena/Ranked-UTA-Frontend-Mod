package org.rankeduta.defines;

import org.json.JSONObject;
import org.rankeduta.RankedUTA;

import java.net.http.HttpResponse;

public enum ResponseCode {
    OK(0),
    ERROR(1),
    PARTY_NOT_FOUND(100),
    PARTY_MISSING_PERMISSIONS(101),
    PARTY_ALREADY_IN(102),
    PARTY_ALREADY_HAVE(103),
    PARTY_NOT_FOUND_INVITE(104),
    PARTY_NOT_FOUND_PLAYER(105),
    PARTY_MAX_PLAYER_LIMITED(106),
    PARTY_COMMAND_LOCKED(107),
    QUEUE_MISSING_PERMISSIONS(201),
    QUEUE_MAX_PLAYER_LIMITED(202);

    private final int code;
    ResponseCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ResponseCode fromCode(int code) {
        for (ResponseCode responseCode : ResponseCode.values()) {
            if (responseCode.getCode() == code) {
                return responseCode;
            }
        }
        return ERROR; // Default to ERROR if no match found
    }
}
