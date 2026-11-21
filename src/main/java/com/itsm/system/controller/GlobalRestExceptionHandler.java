package com.itsm.system.controller;

import com.itsm.system.dto.ApiResponse;
import com.itsm.system.exception.InvalidIncidentException;
import com.itsm.system.exception.InvalidLoginException;
import com.itsm.system.exception.InvalidStateTransitionException;
import com.itsm.system.exception.InvalidUserInputException;
import com.itsm.system.exception.ItsmBaseException;
import com.itsm.system.exception.ItsmDomainException;
import com.itsm.system.exception.MissingSlaConfigurationException;
import com.itsm.system.exception.ServiceNotFoundException;
import com.itsm.system.exception.TechnicianUnavailableException;
import com.itsm.system.exception.TechnicianWorkloadExceededException;
import com.itsm.system.exception.UnauthorizedActionException;
import com.itsm.system.exception.UnauthorizedOperationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Centralized REST exception handler providing uniform error payloads via ApiResponse.
 */
@RestControllerAdvice
public class GlobalRestExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalRestExceptionHandler.class);

    @ExceptionHandler({UnauthorizedActionException.class, UnauthorizedOperationException.class, InvalidLoginException.class})
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(RuntimeException ex) {
        log.warn("Authentication/Authorization error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({ServiceNotFoundException.class, MissingSlaConfigurationException.class})
    public ResponseEntity<ApiResponse<Void>> handleNotFound(RuntimeException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(InvalidIncidentException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidIncident(InvalidIncidentException ex) {
        log.warn("Invalid incident error: {}", ex.getMessage());
        HttpStatus status = (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found"))
                ? HttpStatus.NOT_FOUND
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
            InvalidStateTransitionException.class,
            TechnicianUnavailableException.class,
            TechnicianWorkloadExceededException.class,
            InvalidUserInputException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        log.warn("Client bad request error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({ItsmBaseException.class, ItsmDomainException.class})
    public ResponseEntity<ApiResponse<Void>> handleItsmDomainException(ItsmDomainException ex) {
        log.warn("ITSM domain rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unhandled server exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal Server Error: " + ex.getMessage()));
    }
}
