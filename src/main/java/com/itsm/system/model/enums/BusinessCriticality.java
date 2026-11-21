package com.itsm.system.model.enums;

/**
 * Designates the operational importance of an IT service to business continuity.
 */
public enum BusinessCriticality {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4),
    MISSION_CRITICAL(4);

    private final int weight;

    BusinessCriticality(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
