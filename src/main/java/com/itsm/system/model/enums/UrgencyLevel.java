package com.itsm.system.model.enums;

/**
 * Indicates how quickly an incident requires resolution to avoid catastrophic business impact.
 */
public enum UrgencyLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    UrgencyLevel(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
