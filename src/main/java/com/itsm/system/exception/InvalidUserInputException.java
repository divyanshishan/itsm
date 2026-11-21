package com.itsm.system.exception;

/**
 * Thrown when client or user input fails business validation checks.
 */
public class InvalidUserInputException extends ItsmDomainException {

    public InvalidUserInputException(String message) {
        super(message);
    }

    public InvalidUserInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
