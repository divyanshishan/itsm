package com.itsm.system.exception;

/**
 * Thrown when an IT service cannot be located by its identifier or service code.
 */
public class ServiceNotFoundException extends ItsmDomainException {

    public ServiceNotFoundException(String message) {
        super(message);
    }

    public ServiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
