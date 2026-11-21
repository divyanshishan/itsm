package com.itsm.system.service;

import com.itsm.system.exception.InvalidIncidentException;
import com.itsm.system.exception.InvalidUserInputException;
import com.itsm.system.exception.MissingSlaConfigurationException;
import com.itsm.system.exception.ServiceNotFoundException;
import com.itsm.system.model.entity.AuditLog;
import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.IncidentCategory;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.entity.SlaPolicy;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.entity.User;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.PriorityLevel;
import com.itsm.system.model.enums.SlaStatus;
import com.itsm.system.model.enums.UrgencyLevel;
import com.itsm.system.pattern.observer.notification.NotificationPublisherService;
import com.itsm.system.pattern.strategy.assignment.TechnicianAssignmentStrategy;
import com.itsm.system.pattern.strategy.priority.PriorityCalculationStrategy;
import com.itsm.system.repository.EmployeeRepository;
import com.itsm.system.repository.IncidentCategoryRepository;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.repository.SlaPolicyRepository;
import com.itsm.system.repository.TechnicianRepository;
import com.itsm.system.service.intelligent.DuplicateDetectionService;
import com.itsm.system.service.intelligent.IncidentClassificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service orchestrating incident reporting, prioritization, SLA target mapping,
 * and automated technician assignment via strategy patterns.
 */
@Service
@Transactional
public class IncidentManagementService {

    private static final Logger log = LoggerFactory.getLogger(IncidentManagementService.class);

    private final IncidentRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final TechnicianRepository technicianRepository;
    private final ItServiceRepository itServiceRepository;
    private final IncidentCategoryRepository incidentCategoryRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final IncidentLifecycleService incidentLifecycleService;
    private final NotificationPublisherService notificationPublisherService;
    private final DuplicateDetectionService duplicateDetectionService;
    private final IncidentClassificationService incidentClassificationService;

    private PriorityCalculationStrategy priorityCalculationStrategy;
    private TechnicianAssignmentStrategy technicianAssignmentStrategy;

    public IncidentManagementService(IncidentRepository incidentRepository,
                                     EmployeeRepository employeeRepository,
                                     TechnicianRepository technicianRepository,
                                     ItServiceRepository itServiceRepository,
                                     IncidentCategoryRepository incidentCategoryRepository,
                                     SlaPolicyRepository slaPolicyRepository,
                                     IncidentLifecycleService incidentLifecycleService,
                                     NotificationPublisherService notificationPublisherService,
                                     DuplicateDetectionService duplicateDetectionService,
                                     IncidentClassificationService incidentClassificationService,
                                     PriorityCalculationStrategy priorityCalculationStrategy,
                                     TechnicianAssignmentStrategy technicianAssignmentStrategy) {
        this.incidentRepository = incidentRepository;
        this.employeeRepository = employeeRepository;
        this.technicianRepository = technicianRepository;
        this.itServiceRepository = itServiceRepository;
        this.incidentCategoryRepository = incidentCategoryRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.incidentLifecycleService = incidentLifecycleService;
        this.notificationPublisherService = notificationPublisherService;
        this.duplicateDetectionService = duplicateDetectionService;
        this.incidentClassificationService = incidentClassificationService;
        this.priorityCalculationStrategy = priorityCalculationStrategy;
        this.technicianAssignmentStrategy = technicianAssignmentStrategy;
    }

    /**
     * Pluggable setter allowing dynamic replacement of technician assignment strategy at runtime.
     */
    public void setAssignmentStrategy(TechnicianAssignmentStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Assignment strategy cannot be null.");
        }
        this.technicianAssignmentStrategy = strategy;
    }

    public TechnicianAssignmentStrategy getAssignmentStrategy() {
        return technicianAssignmentStrategy;
    }

    public void setPriorityCalculationStrategy(PriorityCalculationStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Priority calculation strategy cannot be null.");
        }
        this.priorityCalculationStrategy = strategy;
    }

    public PriorityCalculationStrategy getPriorityCalculationStrategy() {
        return priorityCalculationStrategy;
    }

    /**
     * Reports an incident: validates inputs, prioritizes, maps SLAs, sets status to OPEN (unassigned).
     * If impact or urgency is not provided (null), automatically classifies them via IncidentClassificationService.
     */
    public Incident reportIncident(String title,
                                   String description,
                                   Long reporterId,
                                   Long serviceId,
                                   Long categoryId,
                                   ImpactLevel impact,
                                   UrgencyLevel urgency) {
        // 1. Input Validation (impact and urgency are optional/auto-classified)
        validateInputs(title, description, reporterId, serviceId);

        // 2. Fetch Associations
        Employee reporter = employeeRepository.findById(reporterId)
                .orElseThrow(() -> new InvalidIncidentException("Reporter not found with ID: " + reporterId));

        if (!reporter.isActive()) {
            throw new InvalidIncidentException("Reporter account is inactive: " + reporter.getUserIdCode());
        }

        ItService service = itServiceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException("IT Service not found with ID: " + serviceId));

        IncidentCategory category;
        if (categoryId != null) {
            category = incidentCategoryRepository.findById(categoryId)
                    .orElseThrow(() -> new InvalidIncidentException("Incident category not found with ID: " + categoryId));
        } else {
            category = service.getDefaultCategory();
            if (category == null) {
                throw new InvalidIncidentException("No category specified and no default category configured for service: " + service.getServiceName());
            }
        }

        // 3. Auto-classify Impact and/or Urgency if omitted
        boolean autoClassified = false;
        ImpactLevel finalImpact = impact;
        UrgencyLevel finalUrgency = urgency;

        if (finalImpact == null) {
            finalImpact = incidentClassificationService.classifyImpact(title, description, service);
            autoClassified = true;
        }

        if (finalUrgency == null) {
            finalUrgency = incidentClassificationService.classifyUrgency(title, description);
            autoClassified = true;
        }

        // 4. Calculate Priority via Strategy
        double priorityScore = priorityCalculationStrategy.calculatePriorityScore(finalImpact, finalUrgency, service.getCriticality());
        PriorityLevel priorityLevel = priorityCalculationStrategy.determinePriorityLevel(priorityScore);
        log.info("Incident '{}' prioritized: Score={}, Level={} [Impact={}, Urgency={}]",
                title, priorityScore, priorityLevel, finalImpact, finalUrgency);

        // 5. Resolve SLA Policy
        SlaPolicy slaPolicy = slaPolicyRepository.findByPriorityLevel(priorityLevel)
                .orElseThrow(() -> new MissingSlaConfigurationException("No SLA Policy configured for priority level: " + priorityLevel));

        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime responseDeadline = createdAt.plusMinutes(slaPolicy.getResponseTargetMinutes());
        LocalDateTime resolutionDeadline = createdAt.plusMinutes(slaPolicy.getResolutionTargetMinutes());

        // 6. Construct OPEN Incident (assignedTechnician = null)
        String incidentCode = generateUniqueIncidentCode();
        Incident incident = new Incident(
                incidentCode,
                title.trim(),
                description.trim(),
                reporter,
                category,
                service,
                finalImpact,
                finalUrgency,
                priorityScore,
                priorityLevel,
                slaPolicy,
                responseDeadline,
                resolutionDeadline
        );
        incident.setCreatedAt(createdAt);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSlaStatus(SlaStatus.ON_TRACK);
        incident.setAssignedTechnician(null);

        // 7. Record Initial Audit Logs
        String reportAuditDesc = "Incident reported: " + title.trim();
        if (autoClassified) {
            reportAuditDesc += String.format(" [Auto-classified Impact=%s, Urgency=%s based on service %s and symptom analysis]",
                    finalImpact, finalUrgency, service.getServiceCode());
        }

        AuditLog reportAudit = new AuditLog(
                incident,
                createdAt,
                reporter.getUserIdCode(),
                "INCIDENT_REPORTED",
                null,
                IncidentStatus.OPEN.name(),
                reportAuditDesc
        );
        incident.addAuditLog(reportAudit);

        if (autoClassified) {
            AuditLog autoClassifyAudit = new AuditLog(
                    incident,
                    createdAt,
                    "SYSTEM",
                    "AUTO_CLASSIFICATION",
                    IncidentStatus.OPEN.name(),
                    IncidentStatus.OPEN.name(),
                    String.format("Auto-classified Impact=%s, Urgency=%s based on service %s and symptom analysis",
                            finalImpact, finalUrgency, service.getServiceCode())
            );
            incident.addAuditLog(autoClassifyAudit);
        }

        AuditLog priorityAudit = new AuditLog(
                incident,
                createdAt,
                "SYSTEM",
                "PRIORITY_CALCULATED",
                IncidentStatus.OPEN.name(),
                IncidentStatus.OPEN.name(),
                String.format("Calculated Priority: %s (Score: %.2f) [Impact: %s, Urgency: %s, Service Criticality: %s] | SLA Response: %dm, Resolution: %dm",
                        priorityLevel, priorityScore, finalImpact, finalUrgency, service.getCriticality(),
                        slaPolicy.getResponseTargetMinutes(), slaPolicy.getResolutionTargetMinutes())
        );
        incident.addAuditLog(priorityAudit);

        Incident savedIncident = incidentRepository.save(incident);
        notificationPublisherService.notifyIncidentReported(savedIncident);
        return savedIncident;
    }

    /**
     * Pipeline method implementing Automatic Assignment upon Incident Submission (5B Choice 1):
     * 1. Reports the incident (strictly creates it in OPEN state, priority scored, SLA deadlines set, audit log recorded).
     * 2. Evaluates Intelligent Feature A (Duplicate Incident Detection):
     *    - If an active duplicate with an assigned technician is detected on the same service,
     *      it co-assigns to that technician, links to the master cluster, and moves to IN_PROGRESS.
     *    - Otherwise, invokes autoAssignIncident using SmartCascadingAssignmentStrategy to assign a technician.
     * 3. Returns the resulting IN_PROGRESS incident.
     */
    public Incident reportAndAutoAssignIncident(String title, String description, Long reporterId,
                                               Long serviceId, Long categoryId, ImpactLevel impact,
                                               UrgencyLevel urgency) {
        Incident incident = reportIncident(title, description, reporterId, serviceId, categoryId, impact, urgency);

        // Intelligent Feature A: Duplicate Incident Detection & Cluster Co-Assignment
        Optional<Incident> duplicateMatchOpt = duplicateDetectionService.findActiveDuplicate(
                title, description, serviceId, incident.getIncidentCode());

        if (duplicateMatchOpt.isPresent() && duplicateMatchOpt.get().getAssignedTechnician() != null) {
            Incident existingMatch = duplicateMatchOpt.get();
            String masterCode = existingMatch.getDuplicateOfIncidentCode() != null
                    ? existingMatch.getDuplicateOfIncidentCode()
                    : existingMatch.getIncidentCode();

            incident.setDuplicateOfIncidentCode(masterCode);
            incident.setAssignedTechnician(existingMatch.getAssignedTechnician());
            incident.setStatus(IncidentStatus.IN_PROGRESS);
            incident.setAcknowledgedAt(LocalDateTime.now());

            AuditLog dupAudit = new AuditLog(
                    incident,
                    incident.getCreatedAt(),
                    "SYSTEM",
                    "DUPLICATE_LINKED",
                    IncidentStatus.OPEN.name(),
                    IncidentStatus.IN_PROGRESS.name(),
                    "[DUPLICATE LINKED] Identified as duplicate of active incident " + masterCode +
                            ". Co-assigned to technician " + existingMatch.getAssignedTechnician().getUserIdCode() +
                            " and moved to IN_PROGRESS."
            );
            incident.addAuditLog(dupAudit);

            Incident savedIncident = incidentRepository.save(incident);
            notificationPublisherService.notifyTechnicianAssigned(savedIncident);
            log.info("Incident '{}' detected as duplicate of master '{}' and co-assigned to technician '{}'",
                    incident.getIncidentCode(), masterCode, existingMatch.getAssignedTechnician().getUserIdCode());
            return savedIncident;
        } else if (duplicateMatchOpt.isPresent()) {
            Incident existingMatch = duplicateMatchOpt.get();
            String masterCode = existingMatch.getDuplicateOfIncidentCode() != null
                    ? existingMatch.getDuplicateOfIncidentCode()
                    : existingMatch.getIncidentCode();
            incident.setDuplicateOfIncidentCode(masterCode);
            incident = incidentRepository.save(incident);
        }

        Employee reporter = incident.getReporter();
        return autoAssignIncident(incident.getIncidentCode(), reporter);
    }

    /**
     * Automatically assigns an OPEN incident using the configured TechnicianAssignmentStrategy,
     * transitioning the incident to IN_PROGRESS.
     */
    public Incident autoAssignIncident(String incidentCode, User actor) {
        if (incidentCode == null || incidentCode.trim().isEmpty()) {
            throw new InvalidIncidentException("Incident code cannot be null or empty.");
        }

        Incident incident = incidentRepository.findByIncidentCode(incidentCode.trim())
                .orElseThrow(() -> new InvalidIncidentException("Incident not found with code: " + incidentCode));

        // If already co-assigned via duplicate detection to an active cluster technician
        if (incident.getDuplicateOfIncidentCode() != null && incident.getAssignedTechnician() != null) {
            log.info("Incident '{}' is already co-assigned to duplicate cluster '{}' (technician: {})",
                    incidentCode, incident.getDuplicateOfIncidentCode(), incident.getAssignedTechnician().getUserIdCode());
            return incident;
        }

        List<Technician> allTechnicians = technicianRepository.findAll();
        Technician selectedTechnician = technicianAssignmentStrategy.selectTechnician(incident, allTechnicians);

        log.info("Auto-assigning incident '{}' to technician '{}' ({})",
                incidentCode, selectedTechnician.getName(), selectedTechnician.getUserIdCode());

        return incidentLifecycleService.assignTechnicianToIncident(incidentCode, selectedTechnician, actor);
    }

    /**
     * Convenience method combining reporting and auto-assignment.
     */
    public Incident createAndProcessIncident(String title,
                                             String description,
                                             Long reporterId,
                                             Long serviceId,
                                             Long categoryId,
                                             ImpactLevel impact,
                                             UrgencyLevel urgency) {
        return reportAndAutoAssignIncident(title, description, reporterId, serviceId, categoryId, impact, urgency);
    }

    private void validateInputs(String title, String description, Long reporterId, Long serviceId) {
        if (title == null || title.trim().isEmpty()) {
            throw new InvalidUserInputException("Incident title is required and cannot be empty.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new InvalidUserInputException("Incident description is required and cannot be empty.");
        }
        if (reporterId == null) {
            throw new InvalidUserInputException("Reporter ID is required.");
        }
        if (serviceId == null) {
            throw new InvalidUserInputException("Affected service ID is required.");
        }
    }

    private String generateUniqueIncidentCode() {
        String code;
        do {
            int randomNum = ThreadLocalRandom.current().nextInt(1000, 9999);
            code = "INC-" + randomNum;
        } while (incidentRepository.existsByIncidentCode(code));
        return code;
    }
}
