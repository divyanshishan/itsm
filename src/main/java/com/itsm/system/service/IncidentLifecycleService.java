package com.itsm.system.service;

import com.itsm.system.exception.InvalidIncidentException;
import com.itsm.system.exception.InvalidStateTransitionException;
import com.itsm.system.exception.InvalidUserInputException;
import com.itsm.system.exception.UnauthorizedOperationException;
import com.itsm.system.model.entity.AuditLog;
import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.pattern.state.IncidentState;
import com.itsm.system.pattern.state.IncidentStateFactory;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.TechnicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service managing incident lifecycle state transitions, audits, notes, and feedback.
 * Coordinates the 3-state machine: OPEN -> IN_PROGRESS -> CLOSED.
 */
@Service
@Transactional
public class IncidentLifecycleService {

    private static final Logger log = LoggerFactory.getLogger(IncidentLifecycleService.class);

    private final IncidentRepository incidentRepository;
    private final TechnicianRepository technicianRepository;
    private final IncidentStateFactory stateFactory;
    private final com.itsm.system.pattern.observer.notification.NotificationPublisherService notificationPublisherService;

    public IncidentLifecycleService(IncidentRepository incidentRepository,
                                    TechnicianRepository technicianRepository,
                                    IncidentStateFactory stateFactory,
                                    com.itsm.system.pattern.observer.notification.NotificationPublisherService notificationPublisherService) {
        this.incidentRepository = incidentRepository;
        this.technicianRepository = technicianRepository;
        this.stateFactory = stateFactory;
        this.notificationPublisherService = notificationPublisherService;
    }

    /**
     * Assigns a technician to an OPEN incident, transitioning it to IN_PROGRESS.
     */
    public Incident assignTechnicianToIncident(String incidentCode, Technician technician, User actor) {
        Incident incident = getIncidentOrThrow(incidentCode);
        IncidentStatus previousStatus = incident.getStatus();

        IncidentState state = stateFactory.getState(incident);
        state.assignTechnician(incident, technician, actor);

        String actorCode = (actor != null) ? actor.getUserIdCode() : "SYSTEM";
        recordAuditLog(
                incident,
                actorCode,
                "TECHNICIAN_ASSIGNED",
                previousStatus.name(),
                incident.getStatus().name(),
                "Assigned to technician " + technician.getName() + " (" + technician.getUserIdCode() + ")"
        );

        technicianRepository.save(technician);
        Incident savedIncident = incidentRepository.save(incident);
        notificationPublisherService.notifyTechnicianAssigned(savedIncident);
        return savedIncident;
    }

    /**
     * Resolves and closes an IN_PROGRESS incident with resolution details.
     * Also cascades closure to all active duplicate incidents in the same cluster.
     */
    public Incident resolveAndCloseIncident(String incidentCode, User actor, String resolutionDetails) {
        Incident incident = getIncidentOrThrow(incidentCode);
        IncidentStatus previousStatus = incident.getStatus();

        IncidentState state = stateFactory.getState(incident);
        state.resolveAndClose(incident, actor, resolutionDetails);

        recordAuditLog(
                incident,
                actor.getUserIdCode(),
                "RESOLVE_AND_CLOSE",
                previousStatus.name(),
                incident.getStatus().name(),
                "Incident resolved and closed: " + resolutionDetails.trim()
        );

        saveAssociatedTechnician(incident);
        Incident savedIncident = incidentRepository.save(incident);
        notificationPublisherService.notifyIncidentResolvedAndClosed(savedIncident);

        // Feature A: Cascading closure of all active duplicate incidents in the cluster
        cascadeCloseDuplicateCluster(savedIncident, actor, resolutionDetails);

        return savedIncident;
    }

    /**
     * Cascading closure for all active incidents linked to the same duplicate cluster.
     */
    private void cascadeCloseDuplicateCluster(Incident primaryIncident, User actor, String resolutionDetails) {
        String masterCode = primaryIncident.getDuplicateOfIncidentCode() != null
                ? primaryIncident.getDuplicateOfIncidentCode()
                : primaryIncident.getIncidentCode();

        List<Incident> activeClusterIncidents = incidentRepository.findActiveByClusterMasterCode(masterCode);
        LocalDateTime now = LocalDateTime.now();

        for (Incident linked : activeClusterIncidents) {
            if (linked.getIncidentCode().equals(primaryIncident.getIncidentCode())) {
                continue;
            }

            IncidentStatus prevStatus = linked.getStatus();
            linked.setStatus(IncidentStatus.CLOSED);
            linked.setResolvedAt(now);
            linked.setClosedAt(now);
            linked.setResolutionDetails("Resolved together with duplicate incident " + primaryIncident.getIncidentCode() + ": " + resolutionDetails.trim());

            AuditLog cascadeAudit = new AuditLog(
                    linked,
                    now,
                    actor != null ? actor.getUserIdCode() : "SYSTEM",
                    "CASCADE_CLOSED",
                    prevStatus.name(),
                    IncidentStatus.CLOSED.name(),
                    "[CASCADE CLOSED] Automatically closed alongside duplicate incident " + primaryIncident.getIncidentCode()
            );
            linked.addAuditLog(cascadeAudit);

            Incident savedLinked = incidentRepository.save(linked);
            notificationPublisherService.notifyIncidentResolvedAndClosed(savedLinked);
            log.info("[CASCADE CLOSED] Closed linked duplicate incident '{}' alongside primary incident '{}'",
                    linked.getIncidentCode(), primaryIncident.getIncidentCode());
        }
    }

    /**
     * Intelligent Feature G convenience method:
     * Applies a selected historical incident's resolution to an active incident and closes it.
     */
    public Incident applyHistoricalResolutionAndClose(String activeIncidentCode, String selectedHistoricalIncidentCode, User technicianActor) {
        if (selectedHistoricalIncidentCode == null || selectedHistoricalIncidentCode.trim().isEmpty()) {
            throw new InvalidUserInputException("Selected historical incident code cannot be null or empty.");
        }

        Incident historicalIncident = getIncidentOrThrow(selectedHistoricalIncidentCode);
        String historicalResolution = historicalIncident.getResolutionDetails();
        if (historicalResolution == null || historicalResolution.trim().isEmpty()) {
            throw new InvalidUserInputException("Historical incident " + selectedHistoricalIncidentCode + " has no documented resolution details.");
        }

        log.info("Applying historical resolution from '{}' to active incident '{}'",
                selectedHistoricalIncidentCode, activeIncidentCode);

        return resolveAndCloseIncident(activeIncidentCode, technicianActor, historicalResolution);
    }

    /**
     * Reopens a previously CLOSED incident back to IN_PROGRESS.
     */
    public Incident reopenIncident(String incidentCode, User actor, String reason) {
        Incident incident = getIncidentOrThrow(incidentCode);
        IncidentStatus previousStatus = incident.getStatus();

        IncidentState state = stateFactory.getState(incident);
        state.reopen(incident, actor, reason);

        recordAuditLog(
                incident,
                actor.getUserIdCode(),
                "REOPEN",
                previousStatus.name(),
                incident.getStatus().name(),
                "Incident reopened: " + (reason != null && !reason.trim().isEmpty() ? reason.trim() : "No reason provided")
        );

        saveAssociatedTechnician(incident);
        return incidentRepository.save(incident);
    }

    /**
     * Adds an investigation note to an active IN_PROGRESS incident.
     */
    public Incident addInvestigationNote(String incidentCode, User actor, String note) {
        Incident incident = getIncidentOrThrow(incidentCode);

        if (incident.getStatus() != IncidentStatus.IN_PROGRESS) {
            throw new InvalidStateTransitionException(
                    "Investigation notes can only be appended to IN_PROGRESS incidents. Current state: " + incident.getStatus()
            );
        }

        if (note == null || note.trim().isEmpty()) {
            throw new InvalidUserInputException("Investigation note cannot be empty.");
        }

        LocalDateTime now = LocalDateTime.now();
        String formattedNote = "[" + now + " - " + actor.getUserIdCode() + "] " + note.trim();
        String currentNotes = incident.getInvestigationNotes();
        incident.setInvestigationNotes(currentNotes == null || currentNotes.isEmpty() ? formattedNote : currentNotes + "\n" + formattedNote);

        recordAuditLog(
                incident,
                actor.getUserIdCode(),
                "NOTE_ADDED",
                incident.getStatus().name(),
                incident.getStatus().name(),
                "Appended investigation note: " + note.trim()
        );

        return incidentRepository.save(incident);
    }

    /**
     * Allows the original reporting employee to submit customer satisfaction feedback on a CLOSED incident.
     */
    public Incident submitCustomerFeedback(String incidentCode, Employee reporter, String feedback, Integer rating) {
        Incident incident = getIncidentOrThrow(incidentCode);

        if (incident.getStatus() != IncidentStatus.CLOSED) {
            throw new InvalidStateTransitionException(
                    "Customer feedback can only be submitted for CLOSED incidents. Current state: " + incident.getStatus()
            );
        }

        if (reporter != null && incident.getReporter() != null &&
                !incident.getReporter().getUserIdCode().equals(reporter.getUserIdCode())) {
            throw new UnauthorizedOperationException(
                    "Only the original reporter (" + incident.getReporter().getUserIdCode() + ") can submit feedback for this incident."
            );
        }

        if (rating == null || rating < 1 || rating > 5) {
            throw new InvalidUserInputException("Feedback rating must be an integer between 1 and 5.");
        }

        incident.setCustomerFeedback(feedback != null ? feedback.trim() : "");
        incident.setFeedbackRating(rating);

        recordAuditLog(
                incident,
                reporter != null ? reporter.getUserIdCode() : "CUSTOMER",
                "FEEDBACK_SUBMITTED",
                IncidentStatus.CLOSED.name(),
                IncidentStatus.CLOSED.name(),
                "Customer rating: " + rating + "/5" + (feedback != null && !feedback.trim().isEmpty() ? " - " + feedback.trim() : "")
        );

        return incidentRepository.save(incident);
    }

    private Incident getIncidentOrThrow(String incidentCode) {
        if (incidentCode == null || incidentCode.trim().isEmpty()) {
            throw new InvalidIncidentException("Incident code cannot be null or empty.");
        }
        return incidentRepository.findByIncidentCode(incidentCode.trim())
                .orElseThrow(() -> new InvalidIncidentException("Incident not found with code: " + incidentCode));
    }

    private void recordAuditLog(Incident incident, String userCode, String action,
                                String previousState, String newState, String description) {
        AuditLog auditLog = new AuditLog(
                incident,
                LocalDateTime.now(),
                userCode,
                action,
                previousState,
                newState,
                description
        );
        incident.addAuditLog(auditLog);
    }

    private void saveAssociatedTechnician(Incident incident) {
        if (incident.getAssignedTechnician() != null) {
            technicianRepository.save(incident.getAssignedTechnician());
        }
    }
}
