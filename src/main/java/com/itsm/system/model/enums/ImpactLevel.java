package com.itsm.system.model.enums;

/**
 * Represents the scope and severity of business disruption caused by an incident.
 */
public enum ImpactLevel {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    ImpactLevel(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
