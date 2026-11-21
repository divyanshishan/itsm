package com.itsm.system.exception;

/**
 * Thrown when assigning an incident would exceed a technician's maximum concurrent workload capacity.
 */
public class TechnicianWorkloadExceededException extends ItsmDomainException {

    public TechnicianWorkloadExceededException(String message) {
        super(message);
    }

    public TechnicianWorkloadExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
