package com.itsm.system.exception;

/**
 * Thrown when an illegal lifecycle state transition is attempted on an incident.
 */
public class InvalidStateTransitionException extends ItsmDomainException {

    public InvalidStateTransitionException(String message) {
        super(message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
