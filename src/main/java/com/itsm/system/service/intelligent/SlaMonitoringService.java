package com.itsm.system.service.intelligent;

import com.itsm.system.model.entity.AuditLog;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.Role;
import com.itsm.system.model.enums.SlaStatus;
import com.itsm.system.pattern.observer.notification.NotificationPublisherService;
import com.itsm.system.repository.AuditLogRepository;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Intelligent Feature: Continuous SLA Monitoring and Deadline Enforcement Service.
 * Periodically assesses all active incidents against Response and Resolution SLA targets,
 * detects SLA breaches and pre-warning (AT_RISK) thresholds, records audit trail entries,
 * and broadcasts notifications via the NotificationPublisherService Observer pattern.
 */
@Service
@Transactional
public class SlaMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitoringService.class);

    private final IncidentRepository incidentRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final NotificationPublisherService notificationPublisherService;

    public SlaMonitoringService(IncidentRepository incidentRepository,
                                AuditLogRepository auditLogRepository,
                                UserRepository userRepository,
                                NotificationPublisherService notificationPublisherService) {
        this.incidentRepository = incidentRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.notificationPublisherService = notificationPublisherService;
    }

    /**
     * Background scheduled evaluation running every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    public void evaluateActiveSlas() {
        evaluateActiveSlasInternal();
    }

    /**
     * Synchronous evaluation entry point for manual triggering, immediate execution, and tests.
     */
    public void runSlaCheckNow() {
        evaluateActiveSlasInternal();
    }

    /**
     * Core evaluation logic across all active (non-CLOSED) incidents.
     */
    private void evaluateActiveSlasInternal() {
        List<Incident> activeIncidents = incidentRepository.findByStatusNot(IncidentStatus.CLOSED);
        if (activeIncidents.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        log.debug("Evaluating SLAs for {} active incidents at {}", activeIncidents.size(), now);

        for (Incident incident : activeIncidents) {
            boolean modified = false;

            // A. Response SLA Evaluation (when status == IncidentStatus.OPEN and acknowledgedAt == null)
            if (incident.getStatus() == IncidentStatus.OPEN && incident.getAcknowledgedAt() == null) {
                LocalDateTime responseDeadline = incident.getResponseDeadline();
                if (responseDeadline != null) {
                    if (now.isAfter(responseDeadline)) {
                        // Breach Check
                        if (incident.getSlaStatus() != SlaStatus.RESPONSE_VIOLATED) {
                            incident.setSlaViolated(true);
                            incident.setSlaStatus(SlaStatus.RESPONSE_VIOLATED);

                            AuditLog audit = new AuditLog(
                                    incident,
                                    now,
                                    "SYSTEM",
                                    "SLA_VIOLATED",
                                    "OPEN",
                                    "OPEN",
                                    "[SLA BREACH] Response SLA target missed. Deadline was " + responseDeadline
                            );
                            incident.addAuditLog(audit);
                            auditLogRepository.save(audit);

                            User recipient = determineResponseRecipient(incident);
                            if (recipient != null) {
                                notificationPublisherService.notifySlaAlert(incident, recipient, true);
                            }
                            modified = true;
                            log.warn("Incident {} Response SLA breached! Deadline was {}", incident.getIncidentCode(), responseDeadline);
                        }
                    } else if (incident.getCreatedAt() != null) {
                        // Pre-Warning Check (AT_RISK)
                        long totalAllowedSeconds = Duration.between(incident.getCreatedAt(), responseDeadline).getSeconds();
                        long elapsedSeconds = Duration.between(incident.getCreatedAt(), now).getSeconds();

                        if (totalAllowedSeconds > 0 && elapsedSeconds >= (totalAllowedSeconds * 0.75) && incident.getSlaStatus() == SlaStatus.ON_TRACK) {
                            incident.setSlaStatus(SlaStatus.AT_RISK);

                            AuditLog audit = new AuditLog(
                                    incident,
                                    now,
                                    "SYSTEM",
                                    "SLA_WARNING",
                                    "OPEN",
                                    "OPEN",
                                    "[SLA WARNING] >=75% of Response SLA window has elapsed. Deadline: " + responseDeadline
                            );
                            incident.addAuditLog(audit);
                            auditLogRepository.save(audit);

                            User recipient = determineResponseRecipient(incident);
                            if (recipient != null) {
                                notificationPublisherService.notifySlaAlert(incident, recipient, false);
                            }
                            modified = true;
                            log.info("Incident {} Response SLA AT_RISK warning triggered. Deadline is {}", incident.getIncidentCode(), responseDeadline);
                        }
                    }
                }
            }
            // B. Resolution SLA Evaluation (when status == IncidentStatus.IN_PROGRESS)
            else if (incident.getStatus() == IncidentStatus.IN_PROGRESS) {
                LocalDateTime resolutionDeadline = incident.getResolutionDeadline();
                if (resolutionDeadline != null) {
                    if (now.isAfter(resolutionDeadline)) {
                        // Breach Check
                        if (incident.getSlaStatus() != SlaStatus.RESOLUTION_VIOLATED) {
                            incident.setSlaViolated(true);
                            incident.setSlaStatus(SlaStatus.RESOLUTION_VIOLATED);

                            AuditLog audit = new AuditLog(
                                    incident,
                                    now,
                                    "SYSTEM",
                                    "SLA_VIOLATED",
                                    "IN_PROGRESS",
                                    "IN_PROGRESS",
                                    "[SLA BREACH] Resolution SLA target missed. Deadline was " + resolutionDeadline
                            );
                            incident.addAuditLog(audit);
                            auditLogRepository.save(audit);

                            User recipient = incident.getAssignedTechnician() != null
                                    ? incident.getAssignedTechnician()
                                    : incident.getReporter();

                            if (recipient != null) {
                                notificationPublisherService.notifySlaAlert(incident, recipient, true);
                            }
                            modified = true;
                            log.warn("Incident {} Resolution SLA breached! Deadline was {}", incident.getIncidentCode(), resolutionDeadline);
                        }
                    } else if (incident.getCreatedAt() != null) {
                        // Pre-Warning Check (AT_RISK)
                        long totalAllowedSeconds = Duration.between(incident.getCreatedAt(), resolutionDeadline).getSeconds();
                        long elapsedSeconds = Duration.between(incident.getCreatedAt(), now).getSeconds();

                        if (totalAllowedSeconds > 0 && elapsedSeconds >= (totalAllowedSeconds * 0.75) && incident.getSlaStatus() == SlaStatus.ON_TRACK) {
                            incident.setSlaStatus(SlaStatus.AT_RISK);

                            AuditLog audit = new AuditLog(
                                    incident,
                                    now,
                                    "SYSTEM",
                                    "SLA_WARNING",
                                    "IN_PROGRESS",
                                    "IN_PROGRESS",
                                    "[SLA WARNING] >=75% of Resolution SLA window has elapsed. Deadline: " + resolutionDeadline
                            );
                            incident.addAuditLog(audit);
                            auditLogRepository.save(audit);

                            User recipient = incident.getAssignedTechnician() != null
                                    ? incident.getAssignedTechnician()
                                    : incident.getReporter();

                            if (recipient != null) {
                                notificationPublisherService.notifySlaAlert(incident, recipient, false);
                            }
                            modified = true;
                            log.info("Incident {} Resolution SLA AT_RISK warning triggered. Deadline is {}", incident.getIncidentCode(), resolutionDeadline);
                        }
                    }
                }
            }

            if (modified) {
                incidentRepository.save(incident);
            }
        }
    }

    /**
     * Resolves the stakeholder recipient for an unacknowledged OPEN incident:
     * 1. Assigned technician (if pre-assigned)
     * 2. Original reporter
     * 3. First available Administrator
     */
    private User determineResponseRecipient(Incident incident) {
        if (incident.getAssignedTechnician() != null) {
            return incident.getAssignedTechnician();
        }
        if (incident.getReporter() != null) {
            return incident.getReporter();
        }
        return userRepository.findByRole(Role.ADMINISTRATOR).stream().findFirst().orElse(null);
    }
}
