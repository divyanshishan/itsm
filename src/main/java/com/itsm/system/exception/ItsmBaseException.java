package com.itsm.system.exception;

/**
 * Base runtime exception for all ITSM domain and REST API business logic errors.
 */
public class ItsmBaseException extends ItsmDomainException {

    public ItsmBaseException(String message) {
        super(message);
    }

    public ItsmBaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
