package com.itsm.system.model.enums;

/**
 * Enumeration of event types that trigger notifications across registered channels.
 */
public enum NotificationEventType {
    INCIDENT_REPORTED,
    TECHNICIAN_ASSIGNED,
    PRIORITY_CHANGED,
    SLA_APPROACHING,
    SLA_VIOLATED,
    INCIDENT_RESOLVED_CLOSED,
    INCIDENT_REOPENED
}
