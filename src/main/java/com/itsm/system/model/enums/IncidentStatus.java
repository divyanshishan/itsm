package com.itsm.system.model.enums;

/**
 * Defines the discrete lifecycle states of an incident in accordance with the 3-state machine:
 * OPEN (reported, awaiting assignment), IN_PROGRESS (assigned and under investigation), and CLOSED (resolved).
 */
public enum IncidentStatus {
    OPEN,
    IN_PROGRESS,
    CLOSED
}
