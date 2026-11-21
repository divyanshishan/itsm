package com.itsm.system.exception;

/**
 * Thrown when an actor attempts an unauthorized action or fails credentials authentication.
 */
public class UnauthorizedActionException extends UnauthorizedOperationException {

    public UnauthorizedActionException(String message) {
        super(message);
    }

    public UnauthorizedActionException(String message, Throwable cause) {
        super(message, cause);
    }
}
