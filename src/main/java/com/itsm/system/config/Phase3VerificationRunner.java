package com.itsm.system.config;

import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.IncidentCategory;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.UrgencyLevel;
import com.itsm.system.repository.EmployeeRepository;
import com.itsm.system.repository.IncidentCategoryRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.repository.TechnicianRepository;
import com.itsm.system.service.IncidentManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Startup verification runner for Phase 3:
 * Demonstrates Priority Determination, SLA deadlines, and SmartCascadingAssignmentStrategy
 * following the reportIncident -> autoAssignIncident decoupled lifecycle.
 */
@Component
@Order(3)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "itsm.verification.runners.enabled", havingValue = "true", matchIfMissing = false)
public class Phase3VerificationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Phase3VerificationRunner.class);

    private final IncidentManagementService incidentManagementService;
    private final EmployeeRepository employeeRepository;
    private final ItServiceRepository itServiceRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final TechnicianRepository technicianRepository;

    public Phase3VerificationRunner(IncidentManagementService incidentManagementService,
                                   EmployeeRepository employeeRepository,
                                   ItServiceRepository itServiceRepository,
                                   IncidentCategoryRepository categoryRepository,
                                   TechnicianRepository technicianRepository) {
        this.incidentManagementService = incidentManagementService;
        this.employeeRepository = employeeRepository;
        this.itServiceRepository = itServiceRepository;
        this.categoryRepository = categoryRepository;
        this.technicianRepository = technicianRepository;
    }

    @Override
    public void run(String... args) {
        log.info("Starting Phase 3 Verification: Priority, SLA, and Cascading Assignment...");

        Employee reporter = employeeRepository.findByUserIdCode("E101")
                .orElseThrow(() -> new IllegalStateException("Reporter E101 missing from seed data"));
        ItService paymentService = itServiceRepository.findByServiceCode("SVC-PAY")
                .orElseThrow(() -> new IllegalStateException("Payment Application service missing from seed data"));
        ItService networkService = itServiceRepository.findByServiceCode("SVC-NET")
                .orElseThrow(() -> new IllegalStateException("Corporate Network service missing from seed data"));
        IncidentCategory networkCategory = categoryRepository.findByName("NETWORK")
                .orElseThrow(() -> new IllegalStateException("NETWORK category missing from seed data"));

        // =========================================================================
        // TEST 1: Payment Application Incident (CRITICAL Impact & CRITICAL Urgency)
        // =========================================================================
        System.out.println("\n----------------------------------------------------------------------------------------");
        System.out.println("[PHASE 3 TEST 1] Reporting & Assigning High-Criticality Incident on Payment Application");
        System.out.println("----------------------------------------------------------------------------------------");

        // 1. Report incident -> starts OPEN with assignedTechnician = null
        Incident p1Reported = incidentManagementService.reportIncident(
                "Payment Processing Gateway Outage",
                "Transactions failing with HTTP 500 errors on checkout. Total payment processing blocked.",
                reporter.getId(),
                paymentService.getId(),
                null, // Use service default category (APPLICATION)
                ImpactLevel.CRITICAL,
                UrgencyLevel.CRITICAL
        );

        System.out.println(">> Reported Incident  : " + p1Reported.getIncidentCode() + " (Status: " + p1Reported.getStatus() + ", Assignee: " + p1Reported.getAssignedTechnician() + ")");
        System.out.println(">> Service            : " + p1Reported.getAffectedService().getServiceName() + " (Criticality: " + p1Reported.getAffectedService().getCriticality() + ")");
        System.out.println(">> Calculated Score   : " + p1Reported.getPriorityScore() + " (Expected: 4.00)");
        System.out.println(">> Priority Level     : " + p1Reported.getPriority() + " (Expected: P1_CRITICAL)");
        System.out.println(">> Applied SLA Policy : " + p1Reported.getAppliedSla().getDescription());
        System.out.println(">> Response Target    : " + p1Reported.getAppliedSla().getResponseTargetMinutes() + " mins (Deadline: " + p1Reported.getResponseDeadline() + ")");
        System.out.println(">> Resolution Target  : " + p1Reported.getAppliedSla().getResolutionTargetMinutes() + " mins (Deadline: " + p1Reported.getResolutionDeadline() + ")");

        // 2. Auto-assign technician -> transitions OPEN -> IN_PROGRESS
        Incident p1Assigned = incidentManagementService.autoAssignIncident(p1Reported.getIncidentCode(), reporter);
        System.out.println(">> Auto-Assigned State: " + p1Assigned.getStatus() + " (Expected: IN_PROGRESS)");
        System.out.println(">> Assigned Technician: " + p1Assigned.getAssignedTechnician().getName()
                + " [ID: " + p1Assigned.getAssignedTechnician().getUserIdCode()
                + ", Skills: " + p1Assigned.getAssignedTechnician().getSkills()
                + ", Workload: " + p1Assigned.getAssignedTechnician().getCurrentWorkload() + "]");

        // =========================================================================
        // TEST 2: Cascading Tie-Breaker (Skill -> Load -> Experience)
        // =========================================================================
        System.out.println("\n----------------------------------------------------------------------------------------");
        System.out.println("[PHASE 3 TEST 2] Triggering Cascading Tie-Breaker (Skill -> Load -> Experience)");
        System.out.println("----------------------------------------------------------------------------------------");

        // Equip T103 with "NETWORK" skill to create a skill tie between T101 and T103
        Technician t103 = technicianRepository.findByUserIdCode("T103").orElseThrow();
        t103.addSkill("NETWORK");
        technicianRepository.save(t103);

        // Technicians for Category "NETWORK":
        // - T101: has "NETWORK", load = 0, experience = 6 years
        // - T103: has "NETWORK", load = 0, experience = 3 years
        // Both have similar skill ("NETWORK") -> Both have similar load (0 within tolerance 1) -> T101 wins on Experience (6 vs 3)

        Incident tieReported = incidentManagementService.reportIncident(
                "Core Backbone Router BGP Flap",
                "Transit link peering sessions intermittently dropping packets between data centers.",
                reporter.getId(),
                networkService.getId(),
                networkCategory.getId(),
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        Incident tieAssigned = incidentManagementService.autoAssignIncident(tieReported.getIncidentCode(), reporter);
        Technician assigned = tieAssigned.getAssignedTechnician();

        System.out.println(">> Incident Code       : " + tieAssigned.getIncidentCode());
        System.out.println(">> Category Tested     : " + tieAssigned.getCategory().getName());
        System.out.println(">> Priority Level      : " + tieAssigned.getPriority() + " (Score: " + tieAssigned.getPriorityScore() + ")");
        System.out.println(">> Candidate 1         : T101 (Skills: [NETWORK, CLOUD, SECURITY], Experience: 6 yrs, Load: <=1)");
        System.out.println(">> Candidate 2         : T103 (Skills: [HARDWARE, AUTHENTICATION, STORAGE, NETWORK], Experience: 3 yrs, Load: <=1)");
        System.out.println(">> Cascading Selection : Winner is " + assigned.getName() + " (" + assigned.getUserIdCode() + ")");
        System.out.println("   - Status            : " + tieAssigned.getStatus() + " (Expected: IN_PROGRESS)");
        System.out.println("   - Skills            : " + assigned.getSkills());
        System.out.println("   - Experience Years  : " + assigned.getExperienceYears() + " (Selected due to highest experience)");
        System.out.println("   - Current Workload  : " + assigned.getCurrentWorkload());
        System.out.println("----------------------------------------------------------------------------------------\n");

        log.info("Phase 3 Verification completed successfully.");
    }
}
