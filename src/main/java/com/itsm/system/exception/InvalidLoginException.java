package com.itsm.system.exception;

/**
 * Thrown when authentication credentials or token are invalid or user account is disabled.
 */
public class InvalidLoginException extends ItsmDomainException {

    public InvalidLoginException(String message) {
        super(message);
    }

    public InvalidLoginException(String message, Throwable cause) {
        super(message, cause);
    }
}
