package com.itsm.system.exception;

/**
 * Thrown when an incident payload or operation violates domain integrity constraints.
 */
public class InvalidIncidentException extends ItsmDomainException {

    public InvalidIncidentException(String message) {
        super(message);
    }

    public InvalidIncidentException(String message, Throwable cause) {
        super(message, cause);
    }
}
