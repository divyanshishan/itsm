package com.itsm.system.exception;

/**
 * Thrown when an actor attempts an action beyond their designated role or assignment.
 */
public class UnauthorizedOperationException extends ItsmDomainException {

    public UnauthorizedOperationException(String message) {
        super(message);
    }

    public UnauthorizedOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
