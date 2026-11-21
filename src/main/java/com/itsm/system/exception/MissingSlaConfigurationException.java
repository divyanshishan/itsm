package com.itsm.system.exception;

/**
 * Thrown when an SLA policy cannot be resolved for a specific priority level.
 */
public class MissingSlaConfigurationException extends ItsmDomainException {

    public MissingSlaConfigurationException(String message) {
        super(message);
    }

    public MissingSlaConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
