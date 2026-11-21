package com.itsm.system.exception;

/**
 * Thrown when an incident cannot be assigned because the technician is marked unavailable.
 */
public class TechnicianUnavailableException extends ItsmDomainException {

    public TechnicianUnavailableException(String message) {
        super(message);
    }

    public TechnicianUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
