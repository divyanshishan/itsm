package com.itsm.system.exception;

/**
 * Thrown when an incident is detected as a duplicate of an existing unresolved incident.
 */
public class DuplicateIncidentException extends ItsmDomainException {

    public DuplicateIncidentException(String message) {
        super(message);
    }

    public DuplicateIncidentException(String message, Throwable cause) {
        super(message, cause);
    }
}
