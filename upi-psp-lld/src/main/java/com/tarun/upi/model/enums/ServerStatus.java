package com.tarun.upi.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Server status: UP or DOWN. Maps to boolean where UP=true and DOWN=false.
 */
public enum ServerStatus {
    UP(true, "UP"),
    DOWN(false, "DOWN");

    private final boolean status;
    private final String value;

    ServerStatus(boolean status, String value) {
        this.status = status;
        this.value = value;
    }

    public boolean isUp() {
        return status;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static ServerStatus fromStatus(boolean status) {
        return status ? UP : DOWN;
    }
}

