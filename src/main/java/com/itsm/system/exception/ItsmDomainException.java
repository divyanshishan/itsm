package com.itsm.system.exception;

/**
 * Base abstract runtime exception for all business domain errors in the ITSM system.
 */
public abstract class ItsmDomainException extends RuntimeException {

    public ItsmDomainException(String message) {
        super(message);
    }

    public ItsmDomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
