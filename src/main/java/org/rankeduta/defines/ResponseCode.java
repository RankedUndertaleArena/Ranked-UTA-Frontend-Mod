package org.rankeduta.defines;

public enum ResponseCode {
    OK(0),
    ERROR(1),
    PARTY_NOT_FOUND(100),
    PARTY_MISSING_PERMISSIONS(101),
    PARTY_ALREADY_IN(102),
    PARTY_ALREADY_HAVE(103),
    PARTY_INVITE_NOT_FOUND(104);

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
