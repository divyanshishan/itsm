package com.itsm.system.config;

import com.itsm.system.exception.InvalidStateTransitionException;
import com.itsm.system.model.entity.AuditLog;
import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.IncidentCategory;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.UrgencyLevel;
import com.itsm.system.repository.AuditLogRepository;
import com.itsm.system.repository.EmployeeRepository;
import com.itsm.system.repository.IncidentCategoryRepository;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.service.IncidentLifecycleService;
import com.itsm.system.service.IncidentManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup verification runner for Phase 2:
 * Demonstrates 3-state lifecycle machine (OPEN -> IN_PROGRESS -> CLOSED),
 * automated assignment, note logging, resolution, and customer feedback.
 */
@Component
@Order(2)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "itsm.verification.runners.enabled", havingValue = "true", matchIfMissing = false)
public class Phase2VerificationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Phase2VerificationRunner.class);

    private final IncidentManagementService incidentManagementService;
    private final IncidentLifecycleService lifecycleService;
    private final IncidentRepository incidentRepository;
    private final AuditLogRepository auditLogRepository;
    private final EmployeeRepository employeeRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final ItServiceRepository itServiceRepository;

    public Phase2VerificationRunner(IncidentManagementService incidentManagementService,
                                  IncidentLifecycleService lifecycleService,
                                  IncidentRepository incidentRepository,
                                  AuditLogRepository auditLogRepository,
                                  EmployeeRepository employeeRepository,
                                  IncidentCategoryRepository categoryRepository,
                                  ItServiceRepository itServiceRepository) {
        this.incidentManagementService = incidentManagementService;
        this.lifecycleService = lifecycleService;
        this.incidentRepository = incidentRepository;
        this.auditLogRepository = auditLogRepository;
        this.employeeRepository = employeeRepository;
        this.categoryRepository = categoryRepository;
        this.itServiceRepository = itServiceRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Phase 2 Incident Lifecycle & 3-State Pattern Verification...");

        Employee reporter = employeeRepository.findByUserIdCode("E101")
                .orElseThrow(() -> new IllegalStateException("Reporter E101 missing from seed data"));
        IncidentCategory category = categoryRepository.findByName("NETWORK")
                .orElseThrow(() -> new IllegalStateException("NETWORK category missing from seed data"));
        ItService service = itServiceRepository.findByServiceCode("SVC-NET")
                .orElseThrow(() -> new IllegalStateException("SVC-NET service missing from seed data"));

        // 1. Report an incident (starts in OPEN with assignedTechnician = null)
        Incident reported = incidentManagementService.reportIncident(
                "VPN gateway dropping connections intermittently",
                "Multiple remote staff report intermittent SSL-VPN disconnects every 10 minutes.",
                reporter.getId(),
                service.getId(),
                category.getId(),
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        String testCode = reported.getIncidentCode();
        log.info("Reported test incident {} in state {} (assigned technician: {})",
                testCode, reported.getStatus(), reported.getAssignedTechnician());

        if (reported.getStatus() != IncidentStatus.OPEN || reported.getAssignedTechnician() != null) {
            throw new IllegalStateException("Reported incident must start in OPEN state with no assigned technician.");
        }

        // 2. Attempt illegal transition directly from OPEN -> CLOSED
        try {
            lifecycleService.resolveAndCloseIncident(testCode, reporter, "Attempting direct resolution without assignment");
            log.error("[PHASE 2 TEST FAILED] Did not block illegal transition OPEN -> CLOSED");
        } catch (InvalidStateTransitionException ex) {
            System.out.println("[PHASE 2 TEST PASSED] Blocked illegal transition OPEN -> CLOSED");
            log.info("[PHASE 2 TEST PASSED] Blocked illegal transition OPEN -> CLOSED: {}", ex.getMessage());
        }

        // 3. Auto-assign the technician: transitions OPEN -> IN_PROGRESS
        Incident inProgressIncident = incidentManagementService.autoAssignIncident(testCode, reporter);
        log.info("Incident {} transitioned to {} with assigned technician {}",
                testCode, inProgressIncident.getStatus(), inProgressIncident.getAssignedTechnician().getUserIdCode());

        // 4. Add investigation note and resolve/close: transitions IN_PROGRESS -> CLOSED
        lifecycleService.addInvestigationNote(testCode, inProgressIncident.getAssignedTechnician(),
                "Identified duplicate IP address pool lease exhaustion.");
        lifecycleService.resolveAndCloseIncident(testCode, inProgressIncident.getAssignedTechnician(),
                "Expanded DHCP subnet pool for remote VPN and flushed stale leases.");

        // 5. Submit customer feedback
        lifecycleService.submitCustomerFeedback(testCode, reporter,
                "VPN is now completely stable. Thank you for the quick resolution!", 5);

        // 6. Print generated AuditLog entries
        List<AuditLog> auditLogs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(testCode);
        System.out.println("\n========== PHASE 2 AUDIT LOG ENTRIES FOR " + testCode + " ==========");
        for (AuditLog audit : auditLogs) {
            System.out.printf("Action: %-22s | From: %-12s | To: %-12s | User: %-6s | Details: %s%n",
                    audit.getAction(),
                    audit.getPreviousState(),
                    audit.getNewState(),
                    audit.getPerformedByUserCode(),
                    audit.getDescription());
        }
        System.out.println("========================================================================================\n");
    }
}
