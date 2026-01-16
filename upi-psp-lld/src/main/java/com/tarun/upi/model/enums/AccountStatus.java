package com.tarun.upi.model.enums;

/**
 * Status of a user account.
 */
public enum AccountStatus {
    ACTIVE("ACTIVE"),
    DEACTIVATED("DEACTIVATED");

    private final String value;

    AccountStatus(String value) {
        this.value = value;
    }

    /**
     * Convenience: true if this status represents an active account.
     */
    public boolean isActive() {
        return this == ACTIVE;
    }


}
