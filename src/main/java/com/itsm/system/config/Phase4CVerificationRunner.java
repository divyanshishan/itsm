package com.itsm.system.config;

import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.SlaStatus;
import com.itsm.system.model.enums.UrgencyLevel;
import com.itsm.system.repository.EmployeeRepository;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.service.IncidentManagementService;
import com.itsm.system.service.intelligent.SlaMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Startup verification runner for Phase 4C:
 * Continuous SLA Monitoring and Deadline Enforcement.
 * Validates:
 * 1. Response SLA Breach Detection (OPEN -> RESPONSE_VIOLATED)
 * 2. Resolution SLA Pre-warning (IN_PROGRESS -> AT_RISK at >= 75% elapsed)
 * 3. Resolution SLA Breach Detection (IN_PROGRESS -> RESOLUTION_VIOLATED)
 */
@Component
@Order(6)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "itsm.verification.runners.enabled", havingValue = "true", matchIfMissing = false)
public class Phase4CVerificationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Phase4CVerificationRunner.class);

    private final SlaMonitoringService slaMonitoringService;
    private final IncidentManagementService incidentManagementService;
    private final IncidentRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final ItServiceRepository itServiceRepository;

    public Phase4CVerificationRunner(SlaMonitoringService slaMonitoringService,
                                    IncidentManagementService incidentManagementService,
                                    IncidentRepository incidentRepository,
                                    EmployeeRepository employeeRepository,
                                    ItServiceRepository itServiceRepository) {
        this.slaMonitoringService = slaMonitoringService;
        this.incidentManagementService = incidentManagementService;
        this.incidentRepository = incidentRepository;
        this.employeeRepository = employeeRepository;
        this.itServiceRepository = itServiceRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=========================================================================================");
        System.out.println("                 PHASE 4C: CONTINUOUS SLA MONITORING & DEADLINE ENFORCEMENT");
        System.out.println("=========================================================================================");

        Employee reporter = employeeRepository.findByUserIdCode("E101")
                .orElseThrow(() -> new IllegalStateException("Reporter E101 missing from seed data"));
        ItService authService = itServiceRepository.findByServiceCode("SVC-AUTH")
                .orElseThrow(() -> new IllegalStateException("Auth service missing from seed data"));
        ItService cloudService = itServiceRepository.findByServiceCode("SVC-CLOUD")
                .orElseThrow(() -> new IllegalStateException("Cloud service missing from seed data"));

        LocalDateTime now = LocalDateTime.now();

        // =====================================================================================
        // PART 1: RESPONSE SLA BREACH TEST (OPEN incident past response deadline)
        // =====================================================================================
        System.out.println("\n--- [TEST 1] Response SLA Breach Detection ---");

        Incident inc1 = incidentManagementService.reportIncident(
                "Active Directory Kerberos ticket issuance failures across branch",
                "Domain controllers refusing tickets for remote workers, impacting SSO authentication.",
                reporter.getId(),
                authService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        System.out.printf("Created incident %s in state %s. Initial Response Deadline: %s%n",
                inc1.getIncidentCode(), inc1.getStatus(), inc1.getResponseDeadline());

        // Backdate createdAt = now - 30m, responseDeadline = now - 10m
        inc1.setCreatedAt(now.minusMinutes(30));
        inc1.setResponseDeadline(now.minusMinutes(10));
        incidentRepository.save(inc1);

        System.out.println("Backdated incident responseDeadline to 10 mins ago. Triggering SLA check...");
        slaMonitoringService.runSlaCheckNow();

        Incident reloadedInc1 = incidentRepository.findByIncidentCode(inc1.getIncidentCode())
                .orElseThrow(() -> new IllegalStateException("Incident 1 not found"));

        System.out.printf("Reloaded Incident %s: Status=%s | SlaStatus=%s | SlaViolated=%s%n",
                reloadedInc1.getIncidentCode(),
                reloadedInc1.getStatus(),
                reloadedInc1.getSlaStatus(),
                reloadedInc1.isSlaViolated());

        boolean test1Passed = reloadedInc1.isSlaViolated() && reloadedInc1.getSlaStatus() == SlaStatus.RESPONSE_VIOLATED;
        if (test1Passed) {
            System.out.println(">> [SUCCESS] Response SLA breach correctly identified and flagged!");
        } else {
            System.err.println(">> [FAILURE] Response SLA breach was not flagged as expected!");
        }

        // =====================================================================================
        // PART 2: RESOLUTION SLA WARNING (AT_RISK) & BREACH TEST (IN_PROGRESS incident)
        // =====================================================================================
        System.out.println("\n--- [TEST 2] Resolution SLA Warning (AT_RISK) & Breach Detection ---");

        Incident inc2 = incidentManagementService.reportIncident(
                "Kubernetes production ingress pod evictions due to OOMKilled",
                "Multiple ingress-nginx pods restarting frequently under elevated network traffic.",
                reporter.getId(),
                cloudService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        // Auto-assign to move to IN_PROGRESS
        inc2 = incidentManagementService.autoAssignIncident(inc2.getIncidentCode(), reporter);
        System.out.printf("Created and assigned incident %s: Status=%s | Tech=%s (%s)%n",
                inc2.getIncidentCode(),
                inc2.getStatus(),
                inc2.getAssignedTechnician().getName(),
                inc2.getAssignedTechnician().getUserIdCode());

        // 2A. Set createdAt = now - 80m, resolutionDeadline = now + 20m (total 100m, 80m elapsed = 80% >= 75%)
        inc2.setCreatedAt(now.minusMinutes(80));
        inc2.setResolutionDeadline(now.plusMinutes(20));
        incidentRepository.save(inc2);

        System.out.println("Adjusted resolution timeline: 80% of window elapsed. Triggering SLA check...");
        slaMonitoringService.runSlaCheckNow();

        Incident reloadedInc2AtRisk = incidentRepository.findByIncidentCode(inc2.getIncidentCode())
                .orElseThrow(() -> new IllegalStateException("Incident 2 not found"));

        System.out.printf("Reloaded Incident %s (Warning Check): Status=%s | SlaStatus=%s | SlaViolated=%s%n",
                reloadedInc2AtRisk.getIncidentCode(),
                reloadedInc2AtRisk.getStatus(),
                reloadedInc2AtRisk.getSlaStatus(),
                reloadedInc2AtRisk.isSlaViolated());

        boolean warningPassed = reloadedInc2AtRisk.getSlaStatus() == SlaStatus.AT_RISK && !reloadedInc2AtRisk.isSlaViolated();
        if (warningPassed) {
            System.out.println(">> [SUCCESS] Resolution SLA AT_RISK pre-warning correctly triggered at 80% elapsed!");
        } else {
            System.err.println(">> [FAILURE] Resolution SLA AT_RISK pre-warning was not triggered!");
        }

        // 2B. Set resolutionDeadline = now - 5m (breach)
        reloadedInc2AtRisk.setResolutionDeadline(now.minusMinutes(5));
        incidentRepository.save(reloadedInc2AtRisk);

        System.out.println("Adjusted resolutionDeadline to 5 mins ago. Triggering SLA check...");
        slaMonitoringService.runSlaCheckNow();

        Incident reloadedInc2Breached = incidentRepository.findByIncidentCode(inc2.getIncidentCode())
                .orElseThrow(() -> new IllegalStateException("Incident 2 not found"));

        System.out.printf("Reloaded Incident %s (Breach Check): Status=%s | SlaStatus=%s | SlaViolated=%s%n",
                reloadedInc2Breached.getIncidentCode(),
                reloadedInc2Breached.getStatus(),
                reloadedInc2Breached.getSlaStatus(),
                reloadedInc2Breached.isSlaViolated());

        boolean breachPassed = reloadedInc2Breached.isSlaViolated() && reloadedInc2Breached.getSlaStatus() == SlaStatus.RESOLUTION_VIOLATED;
        if (breachPassed) {
            System.out.println(">> [SUCCESS] Resolution SLA breach correctly identified and flagged!");
        } else {
            System.err.println(">> [FAILURE] Resolution SLA breach was not flagged as expected!");
        }

        if (test1Passed && warningPassed && breachPassed) {
            System.out.println("\n[PHASE 4C TEST PASSED] Continuous SLA Monitoring (AT_RISK warnings, Response breaches, and Resolution breaches) verified.");
            log.info("[PHASE 4C TEST PASSED] Continuous SLA Monitoring (AT_RISK warnings, Response breaches, and Resolution breaches) verified.");
        } else {
            throw new IllegalStateException("Phase 4C verification failed!");
        }

        System.out.println("=========================================================================================\n");
    }
}
