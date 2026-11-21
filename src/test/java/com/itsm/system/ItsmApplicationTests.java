package com.itsm.system;

import com.itsm.system.exception.InvalidStateTransitionException;
import com.itsm.system.exception.InvalidUserInputException;
import com.itsm.system.exception.TechnicianUnavailableException;
import com.itsm.system.exception.TechnicianWorkloadExceededException;
import com.itsm.system.exception.UnauthorizedOperationException;
import com.itsm.system.model.entity.Administrator;
import com.itsm.system.model.entity.AuditLog;
import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.IncidentCategory;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.entity.SlaPolicy;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.enums.BusinessCriticality;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.PriorityLevel;
import com.itsm.system.model.enums.Role;
import com.itsm.system.model.enums.SlaStatus;
import com.itsm.system.model.enums.UrgencyLevel;
import com.itsm.system.pattern.state.IncidentState;
import com.itsm.system.pattern.state.IncidentStateFactory;
import com.itsm.system.pattern.strategy.assignment.TechnicianAssignmentStrategy;
import com.itsm.system.pattern.strategy.priority.PriorityCalculationStrategy;
import com.itsm.system.repository.AdministratorRepository;
import com.itsm.system.repository.AuditLogRepository;
import com.itsm.system.repository.EmployeeRepository;
import com.itsm.system.dto.SuggestedResolutionDTO;
import com.itsm.system.model.entity.NotificationRecord;
import com.itsm.system.model.enums.NotificationEventType;
import com.itsm.system.pattern.observer.notification.NotificationChannel;
import com.itsm.system.pattern.observer.notification.NotificationPublisherService;
import com.itsm.system.repository.IncidentCategoryRepository;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.repository.NotificationRecordRepository;
import com.itsm.system.repository.SlaPolicyRepository;
import com.itsm.system.repository.TechnicianRepository;
import com.itsm.system.repository.UserRepository;
import com.itsm.system.service.IncidentLifecycleService;
import com.itsm.system.service.IncidentManagementService;
import com.itsm.system.service.intelligent.DuplicateDetectionService;
import com.itsm.system.service.intelligent.ResolutionRecommendationService;
import com.itsm.system.service.intelligent.SlaMonitoringService;
import com.itsm.system.controller.AuthRestController;
import com.itsm.system.controller.IncidentRestController;
import com.itsm.system.controller.IntelligenceRestController;
import com.itsm.system.controller.NotificationRestController;
import com.itsm.system.controller.PublicPortalRestController;
import com.itsm.system.controller.SystemLookupRestController;
import com.itsm.system.dto.ApiResponse;
import com.itsm.system.dto.FeedbackRequest;
import com.itsm.system.dto.IncidentNoteRequest;
import com.itsm.system.dto.IncidentReportRequest;
import com.itsm.system.dto.IncidentResolveRequest;
import com.itsm.system.dto.LoginRequest;
import com.itsm.system.dto.UserSessionDTO;
import com.itsm.system.exception.UnauthorizedActionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ItsmApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private IncidentCategoryRepository categoryRepository;

    @Autowired
    private SlaPolicyRepository slaPolicyRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ItServiceRepository itServiceRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentStateFactory stateFactory;

    @Autowired
    private IncidentLifecycleService lifecycleService;

    @Autowired
    private PriorityCalculationStrategy priorityCalculationStrategy;

    @Autowired
    private TechnicianAssignmentStrategy technicianAssignmentStrategy;

    @Autowired
    private IncidentManagementService incidentManagementService;

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Autowired
    private NotificationPublisherService notificationPublisherService;

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;

    @Autowired
    private ResolutionRecommendationService resolutionRecommendationService;

    @Autowired
    private SlaMonitoringService slaMonitoringService;

    @Autowired
    private AuthRestController authRestController;

    @Autowired
    private IncidentRestController incidentRestController;

    @Autowired
    private IntelligenceRestController intelligenceRestController;

    @Autowired
    private NotificationRestController notificationRestController;

    @Autowired
    private SystemLookupRestController systemLookupRestController;

    @Autowired
    private PublicPortalRestController publicPortalRestController;

    @Test
    @DisplayName("Context loads and seed data is populated")
    void contextLoadsAndSeedDataVerified() {
        assertEquals(9, categoryRepository.count(), "9 default IncidentCategories should be seeded");
        assertEquals(4, slaPolicyRepository.count(), "4 default SlaPolicies should be seeded");
        assertEquals(5, itServiceRepository.count(), "5 sample ItServices should be seeded");
        assertEquals(7, userRepository.count(), "7 total sample users should be seeded");
        assertEquals(3, employeeRepository.count(), "3 sample Employees should be seeded (E101, E102, E103)");
        assertEquals(3, technicianRepository.count(), "3 sample Technicians should be seeded");
        assertEquals(1, administratorRepository.count(), "1 sample Administrator should be seeded");
        assertEquals(14, incidentRepository.count(), "14 enterprise demo incidents should be seeded");
    }

    @Test
    @DisplayName("Polymorphism and Encapsulation: User hierarchy permissions check")
    void testPolymorphismAndPermissions() {
        Employee emp = employeeRepository.findByUserIdCode("E101").orElseThrow();
        Technician tech = technicianRepository.findByUserIdCode("T101").orElseThrow();
        Administrator admin = administratorRepository.findByUserIdCode("A001").orElseThrow();

        assertEquals(Role.EMPLOYEE, emp.getRole());
        assertEquals(Role.TECHNICIAN, tech.getRole());
        assertEquals(Role.ADMINISTRATOR, admin.getRole());

        assertTrue(emp.getDashboardPermissions().contains("CREATE_INCIDENT"));
        assertTrue(tech.getDashboardPermissions().contains("UPDATE_INCIDENT_STATUS"));
        assertTrue(admin.getDashboardPermissions().contains("ALL_PRIVILEGES"));
    }

    @Test
    @DisplayName("Technician OOP Business Logic: Workload and skills checks")
    void testTechnicianBusinessLogic() {
        Technician tech = technicianRepository.findByUserIdCode("T101").orElseThrow();

        assertTrue(tech.hasSkill("NETWORK"));
        assertTrue(tech.hasSkill("network")); // Case-insensitive
        assertFalse(tech.hasSkill("UNKNOWN_SKILL"));

        assertTrue(tech.canAcceptIncident());
        int initialWorkload = tech.getCurrentWorkload();
        tech.incrementWorkload();
        assertEquals(initialWorkload + 1, tech.getCurrentWorkload());
        tech.decrementWorkload();
        assertEquals(initialWorkload, tech.getCurrentWorkload());
    }

    @Test
    @DisplayName("Incident and AuditLog Composition Lifecycle check")
    void testIncidentCreationAndAuditLogComposition() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        IncidentCategory category = categoryRepository.findByName("NETWORK").orElseThrow();
        ItService service = itServiceRepository.findByServiceCode("SVC-NET").orElseThrow();
        SlaPolicy sla = slaPolicyRepository.findByPriorityLevel(PriorityLevel.P1_CRITICAL).orElseThrow();

        incidentRepository.findByIncidentCode("INC-TEST-001").ifPresent(existing -> {
            incidentRepository.delete(existing);
            incidentRepository.flush();
        });

        Incident incident = new Incident(
                "INC-TEST-001",
                "Corporate WiFi unreachable in Building A",
                "Multiple users reporting complete connectivity outage on floor 3.",
                reporter,
                category,
                service,
                ImpactLevel.CRITICAL,
                UrgencyLevel.CRITICAL,
                4.0,
                PriorityLevel.P1_CRITICAL,
                sla,
                LocalDateTime.now().plusMinutes(5),
                LocalDateTime.now().plusMinutes(30)
        );

        // Add audit log via composition method
        AuditLog log = new AuditLog(
                reporter.getUserIdCode(),
                "INCIDENT_CREATED",
                null,
                IncidentStatus.OPEN.name(),
                "Incident opened by employee"
        );
        incident.addAuditLog(log);

        Incident saved = incidentRepository.save(incident);
        assertNotNull(saved.getId());
        assertEquals(1, saved.getAuditLogs().size());
        assertEquals(saved, saved.getAuditLogs().get(0).getIncident());
        assertEquals(SlaStatus.ON_TRACK, saved.getSlaStatus());
    }

    @Test
    @DisplayName("State Pattern: 3-State Machine strict transitions and custom exception enforcement")
    void testStatePatternTransitions() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        Technician tech = technicianRepository.findByUserIdCode("T102").orElseThrow();

        Incident incident = new Incident();
        incident.setIncidentCode("INC-STATE-UNIT");
        incident.setReporter(reporter);
        incident.setResponseDeadline(LocalDateTime.now().plusMinutes(15));
        incident.setResolutionDeadline(LocalDateTime.now().plusMinutes(60));

        // 1. Initial state is OPEN
        assertEquals(IncidentStatus.OPEN, incident.getStatus());
        IncidentState openState = stateFactory.getState(incident);
        assertEquals(IncidentStatus.OPEN, openState.getStatus());

        // Illegal: cannot resolve directly from OPEN without technician assignment
        assertThrows(InvalidStateTransitionException.class, () ->
                openState.resolveAndClose(incident, reporter, "Direct resolution attempt"));

        // Illegal: cannot reopen from OPEN
        assertThrows(InvalidStateTransitionException.class, () ->
                openState.reopen(incident, reporter, "Reopen attempt"));

        // Null technician validation
        assertThrows(TechnicianUnavailableException.class, () ->
                openState.assignTechnician(incident, null, reporter));

        // Valid: assignTechnician from OPEN -> IN_PROGRESS
        int initialWorkload = tech.getCurrentWorkload();
        openState.assignTechnician(incident, tech, reporter);
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertEquals(tech, incident.getAssignedTechnician());
        assertEquals(initialWorkload + 1, tech.getCurrentWorkload());
        assertNotNull(incident.getAcknowledgedAt());

        // 2. InProgress State
        IncidentState inProgressState = stateFactory.getState(incident);
        assertEquals(IncidentStatus.IN_PROGRESS, inProgressState.getStatus());

        // Illegal: cannot assign technician again from IN_PROGRESS
        assertThrows(InvalidStateTransitionException.class, () ->
                inProgressState.assignTechnician(incident, tech, reporter));

        // Illegal: cannot reopen from IN_PROGRESS
        assertThrows(InvalidStateTransitionException.class, () ->
                inProgressState.reopen(incident, reporter, "Reopen while in progress"));

        // Validation: empty resolution details throws InvalidUserInputException
        assertThrows(InvalidUserInputException.class, () ->
                inProgressState.resolveAndClose(incident, tech, "   "));

        // Valid: resolveAndClose from IN_PROGRESS -> CLOSED
        inProgressState.resolveAndClose(incident, tech, "Database connection timeout fixed via pool resizing.");
        assertEquals(IncidentStatus.CLOSED, incident.getStatus());
        assertNotNull(incident.getResolvedAt());
        assertNotNull(incident.getClosedAt());
        assertEquals(initialWorkload, tech.getCurrentWorkload(), "Technician workload must be decremented upon closure");

        // 3. Closed State
        IncidentState closedState = stateFactory.getState(incident);
        assertEquals(IncidentStatus.CLOSED, closedState.getStatus());

        // Illegal: cannot assign or close an already closed incident
        assertThrows(InvalidStateTransitionException.class, () ->
                closedState.assignTechnician(incident, tech, reporter));
        assertThrows(InvalidStateTransitionException.class, () ->
                closedState.resolveAndClose(incident, tech, "Close again"));

        // Valid: reopen from CLOSED -> IN_PROGRESS
        closedState.reopen(incident, reporter, "Issue recurred after peak load.");
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertNull(incident.getResolvedAt());
        assertNull(incident.getClosedAt());
        assertEquals(initialWorkload + 1, tech.getCurrentWorkload(), "Workload must be re-incremented on reopen");
    }

    @Test
    @DisplayName("Lifecycle Service: 3-State transitions, feedback authorization, and audit trail")
    void testLifecycleServiceFeedbackAndAudit() {
        Employee reporterE101 = employeeRepository.findByUserIdCode("E101").orElseThrow();
        Employee otherEmployeeE102 = employeeRepository.findByUserIdCode("E102").orElseThrow();
        Technician tech = technicianRepository.findByUserIdCode("T103").orElseThrow();
        IncidentCategory cat = categoryRepository.findByName("HARDWARE").orElseThrow();
        ItService svc = itServiceRepository.findByServiceCode("SVC-PORTAL").orElseThrow();
        SlaPolicy sla = slaPolicyRepository.findByPriorityLevel(PriorityLevel.P4_LOW).orElseThrow();

        incidentRepository.findByIncidentCode("INC-SERVICE-TEST").ifPresent(existing -> {
            incidentRepository.delete(existing);
            incidentRepository.flush();
        });

        // 1. Create an unassigned OPEN incident
        Incident incident = new Incident(
                "INC-SERVICE-TEST",
                "Laptop battery swollen",
                "Employee laptop battery expanding in chassis.",
                reporterE101,
                cat,
                svc,
                ImpactLevel.MEDIUM,
                UrgencyLevel.HIGH,
                2.5,
                PriorityLevel.P3_MEDIUM,
                sla,
                LocalDateTime.now().plusHours(4),
                LocalDateTime.now().plusHours(24)
        );
        incidentRepository.save(incident);

        int initialWorkload = tech.getCurrentWorkload();

        // 2. Assign technician: OPEN -> IN_PROGRESS
        lifecycleService.assignTechnicianToIncident("INC-SERVICE-TEST", tech, reporterE101);
        assertEquals(initialWorkload + 1, tech.getCurrentWorkload());

        // 3. Add investigation note (allowed in IN_PROGRESS)
        lifecycleService.addInvestigationNote("INC-SERVICE-TEST", tech, "Replacement battery ordered from stockroom");

        // 4. Resolve and close: IN_PROGRESS -> CLOSED
        lifecycleService.resolveAndCloseIncident("INC-SERVICE-TEST", tech, "Replaced battery pack, thermal test passed");

        Technician updatedTech = technicianRepository.findByUserIdCode("T103").orElseThrow();
        assertEquals(initialWorkload, updatedTech.getCurrentWorkload(), "Workload decremented back to initial");

        // 5. Customer Feedback (allowed in CLOSED)
        // Other employee attempting feedback should throw UnauthorizedOperationException
        assertThrows(UnauthorizedOperationException.class, () ->
                lifecycleService.submitCustomerFeedback("INC-SERVICE-TEST", otherEmployeeE102, "Looks good", 5));

        // Invalid rating < 1 or > 5 should throw InvalidUserInputException
        assertThrows(InvalidUserInputException.class, () ->
                lifecycleService.submitCustomerFeedback("INC-SERVICE-TEST", reporterE101, "Feedback", 6));

        // Original reporter submitting feedback
        Incident resolvedIncident = lifecycleService.submitCustomerFeedback("INC-SERVICE-TEST", reporterE101, "Fast service!", 5);
        assertEquals(5, resolvedIncident.getFeedbackRating());
        assertEquals("Fast service!", resolvedIncident.getCustomerFeedback());

        // 6. Verify audit logs generated for all operations
        assertTrue(auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc("INC-SERVICE-TEST").size() >= 4);
    }

    @Test
    @DisplayName("Priority Determination Strategy: Weighted score and thresholds")
    void testWeightedPriorityCalculationStrategy() {
        // P1_CRITICAL: 4*0.4 + 4*0.3 + 4*0.3 = 4.0
        double scoreP1 = priorityCalculationStrategy.calculatePriorityScore(ImpactLevel.CRITICAL, UrgencyLevel.CRITICAL, BusinessCriticality.MISSION_CRITICAL);
        assertEquals(4.00, scoreP1);
        assertEquals(PriorityLevel.P1_CRITICAL, priorityCalculationStrategy.determinePriorityLevel(scoreP1));

        // P2_HIGH: 3*0.4 + 2*0.3 + 3*0.3 = 1.2 + 0.6 + 0.9 = 2.70
        double scoreP2 = priorityCalculationStrategy.calculatePriorityScore(ImpactLevel.HIGH, UrgencyLevel.MEDIUM, BusinessCriticality.HIGH);
        assertEquals(2.70, scoreP2);
        assertEquals(PriorityLevel.P2_HIGH, priorityCalculationStrategy.determinePriorityLevel(scoreP2));

        // P3_MEDIUM: 2*0.4 + 2*0.3 + 2*0.3 = 0.8 + 0.6 + 0.6 = 2.00
        double scoreP3 = priorityCalculationStrategy.calculatePriorityScore(ImpactLevel.MEDIUM, UrgencyLevel.MEDIUM, BusinessCriticality.MEDIUM);
        assertEquals(2.00, scoreP3);
        assertEquals(PriorityLevel.P3_MEDIUM, priorityCalculationStrategy.determinePriorityLevel(scoreP3));

        // P4_LOW: 1*0.4 + 1*0.3 + 1*0.3 = 0.4 + 0.3 + 0.3 = 1.00
        double scoreP4 = priorityCalculationStrategy.calculatePriorityScore(ImpactLevel.LOW, UrgencyLevel.LOW, BusinessCriticality.LOW);
        assertEquals(1.00, scoreP4);
        assertEquals(PriorityLevel.P4_LOW, priorityCalculationStrategy.determinePriorityLevel(scoreP4));

        // Null checks
        assertThrows(InvalidUserInputException.class, () ->
                priorityCalculationStrategy.calculatePriorityScore(null, UrgencyLevel.HIGH, BusinessCriticality.HIGH));
    }

    @Test
    @DisplayName("Technician Assignment Strategy: SmartCascadingAssignmentStrategy cascading logic")
    void testSmartCascadingAssignmentStrategy() {
        IncidentCategory networkCat = new IncidentCategory("NETWORK", "Network issues", "wifi,vpn", true);
        Incident incident = new Incident();
        incident.setCategory(networkCat);

        // Tech A: has NETWORK skill, workload 2, exp 5
        Technician techA = new Technician("T_A", "Tech A", "a@test.com", "pass", true, 5, true, 5);
        techA.setSkills(Set.of("NETWORK"));
        techA.setCurrentWorkload(2);

        // Tech B: has NETWORK skill, workload 3, exp 10
        Technician techB = new Technician("T_B", "Tech B", "b@test.com", "pass", true, 10, true, 5);
        techB.setSkills(Set.of("NETWORK"));
        techB.setCurrentWorkload(3);

        // Tech C: has DATABASE skill, workload 0, exp 8
        Technician techC = new Technician("T_C", "Tech C", "c@test.com", "pass", true, 8, true, 5);
        techC.setSkills(Set.of("DATABASE"));
        techC.setCurrentWorkload(0);

        List<Technician> candidates = List.of(techA, techB, techC);

        // Step 1: Both techA and techB have "NETWORK" skill (techC filtered out)
        // Step 2: Min workload among (A, B) is 2. Tolerance is 1. Both techA (2) and techB (3 <= 2+1) are within tolerance!
        // Step 3: Experience tie-breaker: techB (10 yrs) > techA (5 yrs) -> techB wins!
        Technician selected = technicianAssignmentStrategy.selectTechnician(incident, candidates);
        assertEquals("T_B", selected.getUserIdCode(), "Tech B should win because of higher experience despite 1 higher workload (within tolerance)");

        // Test single skill match
        Incident dbIncident = new Incident();
        IncidentCategory dbCat = new IncidentCategory("DATABASE", "DB issues", "sql", true);
        dbIncident.setCategory(dbCat);

        Technician singleSkillWinner = technicianAssignmentStrategy.selectTechnician(dbIncident, candidates);
        assertEquals("T_C", singleSkillWinner.getUserIdCode(), "Tech C should win immediately at Step 1 as single skill match");

        // Test workload exceeded
        Technician overloadedTech = new Technician("T_FULL", "Tech Full", "full@test.com", "pass", true, 5, true, 2);
        overloadedTech.setCurrentWorkload(2);
        assertThrows(TechnicianWorkloadExceededException.class, () ->
                technicianAssignmentStrategy.selectTechnician(incident, List.of(overloadedTech)));

        // Test unavailable technician
        Technician offlineTech = new Technician("T_OFF", "Tech Off", "off@test.com", "pass", true, 5, false, 5);
        assertThrows(TechnicianUnavailableException.class, () ->
                technicianAssignmentStrategy.selectTechnician(incident, List.of(offlineTech)));
    }

    @Test
    @DisplayName("Incident Management Service: Distinct reporting and auto-assignment steps")
    void testDistinctReportingAndAssignmentFlow() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService paymentService = itServiceRepository.findByServiceCode("SVC-PAY").orElseThrow();

        // 1. Report incident -> status is OPEN, assignedTechnician is null
        Incident reported = incidentManagementService.reportIncident(
                "Payment Microservice High Latency",
                "Payment gateway response times degraded by 400%",
                reporter.getId(),
                paymentService.getId(),
                null, // Use service default category (APPLICATION)
                ImpactLevel.CRITICAL,
                UrgencyLevel.CRITICAL
        );

        assertNotNull(reported.getId());
        assertNotNull(reported.getIncidentCode());
        assertEquals(IncidentStatus.OPEN, reported.getStatus(), "Reported incident must start in OPEN status");
        assertNull(reported.getAssignedTechnician(), "Reported incident must not yet have an assigned technician");
        assertEquals(PriorityLevel.P1_CRITICAL, reported.getPriority());
        assertEquals(4.00, reported.getPriorityScore());
        assertNotNull(reported.getResponseDeadline());
        assertNotNull(reported.getResolutionDeadline());

        // 2. Auto-assign incident -> status transitions to IN_PROGRESS, technician assigned
        Incident assigned = incidentManagementService.autoAssignIncident(reported.getIncidentCode(), reporter);
        assertEquals(IncidentStatus.IN_PROGRESS, assigned.getStatus(), "Incident must transition to IN_PROGRESS upon assignment");
        assertNotNull(assigned.getAssignedTechnician(), "Technician must be assigned");

        // 3. Verify audit logs captured
        incidentRepository.flush();
        List<AuditLog> logs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(reported.getIncidentCode());
        assertTrue(logs.size() >= 3, "Should have logs for INCIDENT_REPORTED, PRIORITY_CALCULATED, and TECHNICIAN_ASSIGNED");

        // 4. Test strategy pluggability setter
        TechnicianAssignmentStrategy mockCustomStrategy = (inc, candidates) -> candidates.get(0);
        incidentManagementService.setAssignmentStrategy(mockCustomStrategy);
        assertEquals(mockCustomStrategy, incidentManagementService.getAssignmentStrategy());

        // Restore default strategy
        incidentManagementService.setAssignmentStrategy(technicianAssignmentStrategy);
    }

    @Test
    @DisplayName("Observer Pattern: Notification dispatch across DASHBOARD and SIMULATED_EMAIL channels on lifecycle events")
    void testNotificationObserverPatternOnIncidentLifecycle() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService svc = itServiceRepository.findByServiceCode("SVC-PORTAL").orElseThrow();

        // 1. Report incident -> Should notify reporter across all channels (INCIDENT_REPORTED)
        Incident incident = incidentManagementService.reportIncident(
                "HR Portal Access Failure",
                "Cannot log in to HR portal to view salary slip",
                reporter.getId(),
                svc.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        String incidentCode = incident.getIncidentCode();
        List<NotificationRecord> reportedNotifs = notificationRecordRepository.findByIncidentCodeOrderByCreatedAtDesc(incidentCode);
        assertFalse(reportedNotifs.isEmpty(), "Notifications must be recorded for incident reporting");

        // Verify DASHBOARD and SIMULATED_EMAIL both got INCIDENT_REPORTED
        boolean hasDashboardReported = reportedNotifs.stream().anyMatch(n ->
                "DASHBOARD".equals(n.getChannelType()) && n.getEventType() == NotificationEventType.INCIDENT_REPORTED);
        boolean hasEmailReported = reportedNotifs.stream().anyMatch(n ->
                "SIMULATED_EMAIL".equals(n.getChannelType()) && n.getEventType() == NotificationEventType.INCIDENT_REPORTED);

        assertTrue(hasDashboardReported, "Dashboard channel should receive INCIDENT_REPORTED");
        assertTrue(hasEmailReported, "Simulated email channel should receive INCIDENT_REPORTED");

        // 2. Auto-assign incident -> Should notify both assigned technician and reporter across all channels
        Incident assigned = incidentManagementService.autoAssignIncident(incidentCode, reporter);
        List<NotificationRecord> assignedNotifs = notificationRecordRepository.findByIncidentCodeOrderByCreatedAtDesc(incidentCode);

        boolean hasTechNotif = assignedNotifs.stream().anyMatch(n ->
                assigned.getAssignedTechnician().getUserIdCode().equals(n.getRecipientUserCode()) &&
                n.getEventType() == NotificationEventType.TECHNICIAN_ASSIGNED);
        boolean hasReporterAssignmentNotif = assignedNotifs.stream().anyMatch(n ->
                reporter.getUserIdCode().equals(n.getRecipientUserCode()) &&
                n.getEventType() == NotificationEventType.TECHNICIAN_ASSIGNED);

        assertTrue(hasTechNotif, "Assigned technician must receive TECHNICIAN_ASSIGNED notification");
        assertTrue(hasReporterAssignmentNotif, "Reporting employee must receive TECHNICIAN_ASSIGNED notification");

        // 3. Resolve and close incident -> Should notify reporter of closure
        lifecycleService.resolveAndCloseIncident(incidentCode, assigned.getAssignedTechnician(), "Password reset link sent and SSO session cleared.");
        List<NotificationRecord> closedNotifs = notificationRecordRepository.findByIncidentCodeOrderByCreatedAtDesc(incidentCode);

        boolean hasResolvedNotif = closedNotifs.stream().anyMatch(n ->
                reporter.getUserIdCode().equals(n.getRecipientUserCode()) &&
                n.getEventType() == NotificationEventType.INCIDENT_RESOLVED_CLOSED);

        assertTrue(hasResolvedNotif, "Reporter must receive INCIDENT_RESOLVED_CLOSED notification");
    }

    @Test
    @DisplayName("Observer Pattern & Open/Closed Principle: Dynamic channel registration at runtime")
    void testDynamicNotificationChannelRegistration() {
        class MockSmsNotificationChannel implements NotificationChannel {
            private int notificationCount = 0;

            @Override
            public String getChannelName() {
                return "SMS_GATEWAY";
            }

            @Override
            public void sendNotification(com.itsm.system.model.entity.User recipient, Incident incident, NotificationEventType eventType, String subject, String message) {
                notificationCount++;
            }

            public int getNotificationCount() {
                return notificationCount;
            }
        }

        MockSmsNotificationChannel mockSmsChannel = new MockSmsNotificationChannel();

        // 1. Dynamic registration
        notificationPublisherService.registerChannel(mockSmsChannel);
        assertTrue(notificationPublisherService.getChannels().contains(mockSmsChannel));

        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        Incident incident = new Incident();
        incident.setIncidentCode("INC-DYN-01");

        // 2. Broadcast notification
        notificationPublisherService.notifyUser(
                reporter, incident, NotificationEventType.PRIORITY_CHANGED,
                "Priority Elevated", "Incident priority changed to P1"
        );

        assertEquals(1, mockSmsChannel.getNotificationCount(), "Dynamically registered channel must receive broadcast");

        // 3. Dynamic unregistration
        notificationPublisherService.unregisterChannel(mockSmsChannel);
        assertFalse(notificationPublisherService.getChannels().contains(mockSmsChannel));

        notificationPublisherService.notifyUser(
                reporter, incident, NotificationEventType.PRIORITY_CHANGED,
                "Priority Adjusted", "Incident priority changed to P2"
        );

        assertEquals(1, mockSmsChannel.getNotificationCount(), "Unregistered channel must not receive further notifications");
    }

    @Test
    @DisplayName("Notification System: SLA Warning and Violation Alerts")
    void testSlaAlertNotifications() {
        Technician tech = technicianRepository.findByUserIdCode("T101").orElseThrow();
        Incident incident = new Incident();
        incident.setIncidentCode("INC-SLA-TEST");
        incident.setTitle("Critical Core Switch Flapping");
        incident.setPriority(PriorityLevel.P1_CRITICAL);
        incident.setResolutionDeadline(LocalDateTime.now().plusMinutes(10));

        // Test SLA Approaching Warning
        notificationPublisherService.notifySlaAlert(incident, tech, false);
        List<NotificationRecord> warningNotifs = notificationRecordRepository.findByIncidentCodeOrderByCreatedAtDesc("INC-SLA-TEST");
        assertTrue(warningNotifs.stream().anyMatch(n -> n.getEventType() == NotificationEventType.SLA_APPROACHING));

        // Test SLA Violation Alert
        notificationPublisherService.notifySlaAlert(incident, tech, true);
        List<NotificationRecord> allSlaNotifs = notificationRecordRepository.findByIncidentCodeOrderByCreatedAtDesc("INC-SLA-TEST");
        assertTrue(allSlaNotifs.stream().anyMatch(n -> n.getEventType() == NotificationEventType.SLA_VIOLATED));
    }

    @Test
    @DisplayName("Feature A: Duplicate Incident Detection, Co-Assignment to same Technician, and Cascading Cluster Closure")
    void testDuplicateDetectionAndCoAssignmentAndCascadeClosure() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        IncidentCategory netCat = categoryRepository.findByName("NETWORK").orElseThrow();
        ItService testService = itServiceRepository.findByServiceCode("SVC-DUP-TEST").orElseGet(() ->
                itServiceRepository.save(new ItService(
                        "SVC-DUP-TEST",
                        "Duplicate Cluster Test Service",
                        "Isolated service for testing duplicate detection",
                        BusinessCriticality.HIGH,
                        netCat,
                        "OPERATIONAL"
                ))
        );

        // 1. Report Master Incident 1 on testService and auto-assign
        Incident inc1 = incidentManagementService.reportIncident(
                "Core edge switch port flapping and packet loss",
                "Spine switch 01 experiencing severe link flapping and dropped frames.",
                reporter.getId(),
                testService.getId(),
                netCat.getId(),
                ImpactLevel.CRITICAL,
                UrgencyLevel.CRITICAL
        );
        inc1 = incidentManagementService.autoAssignIncident(inc1.getIncidentCode(), reporter);
        assertEquals(IncidentStatus.IN_PROGRESS, inc1.getStatus());
        assertNotNull(inc1.getAssignedTechnician());

        Technician assignedTech = inc1.getAssignedTechnician();
        int techWorkload = assignedTech.getCurrentWorkload();

        // 2. Report Duplicate Incident 2 with strong keyword overlap on the same service
        Incident inc2 = incidentManagementService.reportAndAutoAssignIncident(
                "Spine switch 01 port flapping and packet drop issue",
                "Subnet users observing link flapping and packet loss on spine switch 01.",
                reporter.getId(),
                testService.getId(),
                netCat.getId(),
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        // Verify Duplicate Detection and Co-Assignment
        assertEquals(inc1.getIncidentCode(), inc2.getDuplicateOfIncidentCode(),
                "Incident 2 should be linked as duplicate of Incident 1");
        assertEquals(IncidentStatus.IN_PROGRESS, inc2.getStatus(),
                "Duplicate incident should be moved to IN_PROGRESS upon co-assignment");
        assertNotNull(inc2.getAssignedTechnician(),
                "Duplicate incident should have technician assigned");
        assertEquals(assignedTech.getUserIdCode(), inc2.getAssignedTechnician().getUserIdCode(),
                "Duplicate incident must be co-assigned to the exact same technician handling master incident");
        assertEquals(techWorkload, assignedTech.getCurrentWorkload(),
                "Technician workload should NOT be incremented again for co-assigned duplicates");

        // Verify DUPLICATE_LINKED audit log
        boolean hasDuplicateAudit = inc2.getAuditLogs().stream()
                .anyMatch(l -> "DUPLICATE_LINKED".equals(l.getAction()));
        assertTrue(hasDuplicateAudit, "Duplicate incident should contain DUPLICATE_LINKED audit log");

        // 3. Resolve Master Incident 1 -> Verify Cascading Closure of Incident 2
        lifecycleService.resolveAndCloseIncident(
                inc1.getIncidentCode(),
                assignedTech,
                "Flushed stale pooled DB connections and restarted payment workers."
        );

        Incident reloadedInc1 = incidentRepository.findByIncidentCode(inc1.getIncidentCode()).orElseThrow();
        Incident reloadedInc2 = incidentRepository.findByIncidentCode(inc2.getIncidentCode()).orElseThrow();

        assertEquals(IncidentStatus.CLOSED, reloadedInc1.getStatus(), "Master incident 1 must be CLOSED");
        assertEquals(IncidentStatus.CLOSED, reloadedInc2.getStatus(), "Linked duplicate incident 2 must be CLOSED via cascade");
        assertNotNull(reloadedInc2.getResolvedAt());
        assertNotNull(reloadedInc2.getClosedAt());
        assertTrue(reloadedInc2.getResolutionDetails().contains("Resolved together with duplicate incident " + inc1.getIncidentCode()),
                "Duplicate incident resolution should reference master closure details");

        // Verify CASCADE_CLOSED audit log on duplicate incident
        boolean hasCascadeAudit = reloadedInc2.getAuditLogs().stream()
                .anyMatch(l -> "CASCADE_CLOSED".equals(l.getAction()));
        assertTrue(hasCascadeAudit, "Duplicate incident should contain CASCADE_CLOSED audit log");
    }

    @Test
    @DisplayName("Feature G: Suggested Historical Resolutions ranking and applyHistoricalResolutionAndClose")
    void testResolutionRecommendationServiceRankingAndApply() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService paymentService = itServiceRepository.findByServiceCode("SVC-PAY").orElseThrow();

        // 1. Ensure at least one closed incident with resolution details exists
        Incident historicalInc = incidentManagementService.reportIncident(
                "Payment gateway latency and timeout",
                "Stuck connection threads causing payment gateway timeouts",
                reporter.getId(),
                paymentService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );
        historicalInc = incidentManagementService.autoAssignIncident(historicalInc.getIncidentCode(), reporter);
        lifecycleService.resolveAndCloseIncident(
                historicalInc.getIncidentCode(),
                historicalInc.getAssignedTechnician(),
                "Increased connection timeout threshold to 30s and purged dead sockets."
        );

        // 2. Create an active target incident requiring diagnosis
        Incident targetIncident = incidentManagementService.reportIncident(
                "Payment gateway transaction timeouts during peak checkout",
                "High volume causing payment gateway connection drops",
                reporter.getId(),
                paymentService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );
        targetIncident = incidentManagementService.autoAssignIncident(targetIncident.getIncidentCode(), reporter);

        // 3. Query historical recommendations
        List<SuggestedResolutionDTO> recommendations = resolutionRecommendationService.getAllAvailableHistoricalSolutions(targetIncident);

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty(), "Historical recommendations must not be empty");

        // Verify descending order of similarityScore
        for (int i = 0; i < recommendations.size() - 1; i++) {
            assertTrue(recommendations.get(i).getSimilarityScore() >= recommendations.get(i + 1).getSimilarityScore(),
                    "Recommendations must be sorted in descending order of similarity score");
        }

        // Top recommendation should have high relevance (service match +40%, category match +30%)
        SuggestedResolutionDTO topRecommendation = recommendations.get(0);
        assertTrue(topRecommendation.getSimilarityScore() >= 70.0,
                "Top recommendation on same service and category should have relevance >= 70.0");
        assertNotNull(topRecommendation.getResolutionDetails());

        // 4. Test applyHistoricalResolutionAndClose
        Incident closedTarget = lifecycleService.applyHistoricalResolutionAndClose(
                targetIncident.getIncidentCode(),
                topRecommendation.getHistoricalIncidentCode(),
                reporter
        );

        assertEquals(IncidentStatus.CLOSED, closedTarget.getStatus(),
                "Target incident should be CLOSED after applying historical resolution");
        assertEquals(topRecommendation.getResolutionDetails(), closedTarget.getResolutionDetails(),
                "Resolution details should match the historical solution applied");
    }

    @Test
    @DisplayName("Phase 4C: Response SLA breach detection transitions OPEN incident to RESPONSE_VIOLATED")
    void testResponseSlaBreachDetection() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService authService = itServiceRepository.findByServiceCode("SVC-AUTH").orElseThrow();

        Incident incident = incidentManagementService.reportIncident(
                "Unit Test: Directory Federation ADFS Authentication Failure",
                "Federation service throwing HTTP 503 during SAML assertion token validation.",
                reporter.getId(),
                authService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        LocalDateTime now = LocalDateTime.now();
        incident.setCreatedAt(now.minusMinutes(35));
        incident.setResponseDeadline(now.minusMinutes(10));
        incident = incidentRepository.save(incident);
        incidentRepository.flush();

        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();

        Incident reloaded = incidentRepository.findByIncidentCode(incident.getIncidentCode()).orElseThrow();
        assertTrue(reloaded.isSlaViolated(), "slaViolated flag must be true");
        assertEquals(SlaStatus.RESPONSE_VIOLATED, reloaded.getSlaStatus(), "Status must be RESPONSE_VIOLATED");

        List<AuditLog> logs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(reloaded.getIncidentCode());
        assertTrue(logs.stream().anyMatch(l -> "SLA_VIOLATED".equals(l.getAction())), "SLA_VIOLATED audit log must be recorded");

        List<NotificationRecord> notifs = notificationRecordRepository.findByIncidentCodeOrderByCreatedAtDesc(reloaded.getIncidentCode());
        assertTrue(notifs.stream().anyMatch(n -> n.getEventType() == NotificationEventType.SLA_VIOLATED), "SLA_VIOLATED notification must be dispatched");
    }

    @Test
    @DisplayName("Phase 4C: Response SLA pre-warning triggers AT_RISK when >= 75% elapsed")
    void testResponseSlaWarningAtRiskDetection() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService authService = itServiceRepository.findByServiceCode("SVC-AUTH").orElseThrow();

        Incident incident = incidentManagementService.reportIncident(
                "Unit Test: Multi-factor token sync failure on security key",
                "Hardware security keys intermittently desynchronized with authenticator portal.",
                reporter.getId(),
                authService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        LocalDateTime now = LocalDateTime.now();
        incident.setCreatedAt(now.minusMinutes(16));
        incident.setResponseDeadline(now.plusMinutes(4));
        incident = incidentRepository.save(incident);
        incidentRepository.flush();

        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();

        Incident reloaded = incidentRepository.findByIncidentCode(incident.getIncidentCode()).orElseThrow();
        assertFalse(reloaded.isSlaViolated(), "Incident should not be breached yet");
        assertEquals(SlaStatus.AT_RISK, reloaded.getSlaStatus(), "Status must be AT_RISK");

        List<AuditLog> logs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(reloaded.getIncidentCode());
        assertTrue(logs.stream().anyMatch(l -> "SLA_WARNING".equals(l.getAction())), "SLA_WARNING audit log must be recorded");

        List<NotificationRecord> notifs = notificationRecordRepository.findByIncidentCodeOrderByCreatedAtDesc(reloaded.getIncidentCode());
        assertTrue(notifs.stream().anyMatch(n -> n.getEventType() == NotificationEventType.SLA_APPROACHING), "SLA_APPROACHING notification must be dispatched");
    }

    @Test
    @DisplayName("Phase 4C: Resolution SLA pre-warning triggers AT_RISK when >= 75% elapsed in IN_PROGRESS")
    void testResolutionSlaWarningAtRiskDetection() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService cloudService = itServiceRepository.findByServiceCode("SVC-CLOUD").orElseThrow();

        Incident incident = incidentManagementService.reportIncident(
                "Unit Test: Elastic Block Store I/O queue stalls on data volume",
                "Cloud persistent volume attached to database pod reporting elevated IO wait times.",
                reporter.getId(),
                cloudService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        incident = incidentManagementService.autoAssignIncident(incident.getIncidentCode(), reporter);
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());

        LocalDateTime now = LocalDateTime.now();
        incident.setCreatedAt(now.minusMinutes(80));
        incident.setResolutionDeadline(now.plusMinutes(20));
        incident = incidentRepository.save(incident);
        incidentRepository.flush();

        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();

        Incident reloaded = incidentRepository.findByIncidentCode(incident.getIncidentCode()).orElseThrow();
        assertFalse(reloaded.isSlaViolated(), "Incident should not be breached yet");
        assertEquals(SlaStatus.AT_RISK, reloaded.getSlaStatus(), "Status must be AT_RISK");

        List<AuditLog> logs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(reloaded.getIncidentCode());
        assertTrue(logs.stream().anyMatch(l -> "SLA_WARNING".equals(l.getAction())), "SLA_WARNING audit log must be recorded");

        List<NotificationRecord> notifs = notificationRecordRepository.findByIncidentCodeOrderByCreatedAtDesc(reloaded.getIncidentCode());
        assertTrue(notifs.stream().anyMatch(n -> n.getEventType() == NotificationEventType.SLA_APPROACHING), "SLA_APPROACHING notification must be dispatched");
    }

    @Test
    @DisplayName("Phase 4C: Resolution SLA breach detection transitions IN_PROGRESS incident to RESOLUTION_VIOLATED")
    void testResolutionSlaBreachDetection() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService cloudService = itServiceRepository.findByServiceCode("SVC-CLOUD").orElseThrow();

        Incident incident = incidentManagementService.reportIncident(
                "Unit Test: Object Storage S3 Gateway 500 internal errors",
                "Object storage bucket endpoint returning HTTP 500 during file uploads.",
                reporter.getId(),
                cloudService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        incident = incidentManagementService.autoAssignIncident(incident.getIncidentCode(), reporter);
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());

        LocalDateTime now = LocalDateTime.now();
        incident.setCreatedAt(now.minusMinutes(120));
        incident.setResolutionDeadline(now.minusMinutes(15));
        incident = incidentRepository.save(incident);
        incidentRepository.flush();

        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();

        Incident reloaded = incidentRepository.findByIncidentCode(incident.getIncidentCode()).orElseThrow();
        assertTrue(reloaded.isSlaViolated(), "slaViolated flag must be true");
        assertEquals(SlaStatus.RESOLUTION_VIOLATED, reloaded.getSlaStatus(), "Status must be RESOLUTION_VIOLATED");

        List<AuditLog> logs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(reloaded.getIncidentCode());
        assertTrue(logs.stream().anyMatch(l -> "SLA_VIOLATED".equals(l.getAction())), "SLA_VIOLATED audit log must be recorded");

        List<NotificationRecord> notifs = notificationRecordRepository.findByIncidentCodeOrderByCreatedAtDesc(reloaded.getIncidentCode());
        assertTrue(notifs.stream().anyMatch(n -> n.getEventType() == NotificationEventType.SLA_VIOLATED), "SLA_VIOLATED notification must be dispatched");
    }

    @Test
    @DisplayName("Phase 5 Part 1: AuthRestController login succeeds for E101, T101, T103, A001 without LazyInitializationException and supports demo passwords")
    void testAuthRestControllerLoginValidation() {
        // 1. Employee login with user code
        ApiResponse<UserSessionDTO> empResponse = authRestController.login(new LoginRequest("E101", "password123"));
        assertTrue(empResponse.isSuccess());
        assertNotNull(empResponse.getData());
        assertEquals("E101", empResponse.getData().getUserIdCode());
        assertEquals(Role.EMPLOYEE, empResponse.getData().getRole());
        assertEquals("Finance", empResponse.getData().getDepartment());

        // Also test demo password "pass123" for E101
        ApiResponse<UserSessionDTO> empPassDemo = authRestController.login(new LoginRequest("E101", "pass123"));
        assertTrue(empPassDemo.isSuccess());
        assertEquals("E101", empPassDemo.getData().getUserIdCode());

        // 2. Technician T101 login with email (case-insensitive)
        ApiResponse<UserSessionDTO> techResponse = authRestController.login(new LoginRequest("charlie.davis@itsm.com", "password123"));
        assertTrue(techResponse.isSuccess());
        assertNotNull(techResponse.getData());
        assertEquals("T101", techResponse.getData().getUserIdCode());
        assertEquals(Role.TECHNICIAN, techResponse.getData().getRole());
        assertTrue(techResponse.getData().getSkills().contains("NETWORK"));

        // 3. Technician T103 login verifying lazy skills collection does not throw LazyInitializationException
        ApiResponse<UserSessionDTO> t103Response = authRestController.login(new LoginRequest("T103", "password123"));
        assertTrue(t103Response.isSuccess());
        assertNotNull(t103Response.getData());
        assertEquals("T103", t103Response.getData().getUserIdCode());
        assertEquals(Role.TECHNICIAN, t103Response.getData().getRole());
        assertNotNull(t103Response.getData().getSkills());
        assertFalse(t103Response.getData().getSkills().isEmpty(), "T103 skills must be loaded");
        assertTrue(t103Response.getData().getSkills().contains("HARDWARE"));

        // Test demo password "pass123" on T103
        ApiResponse<UserSessionDTO> t103Demo = authRestController.login(new LoginRequest("T103", "pass123"));
        assertTrue(t103Demo.isSuccess());
        assertEquals("T103", t103Demo.getData().getUserIdCode());

        // 4. Administrator A001 login with original password and demo password
        ApiResponse<UserSessionDTO> adminResponse = authRestController.login(new LoginRequest("A001", "adminPassword123"));
        assertTrue(adminResponse.isSuccess());
        assertNotNull(adminResponse.getData());
        assertEquals("A001", adminResponse.getData().getUserIdCode());
        assertEquals(Role.ADMINISTRATOR, adminResponse.getData().getRole());

        ApiResponse<UserSessionDTO> adminDemoResponse = authRestController.login(new LoginRequest("A001", "password123"));
        assertTrue(adminDemoResponse.isSuccess());
        assertEquals("A001", adminDemoResponse.getData().getUserIdCode());

        // 5. Invalid password rejection
        assertThrows(UnauthorizedActionException.class, () ->
                authRestController.login(new LoginRequest("E101", "wrongPassword")));

        // 6. Non-existent user rejection
        assertThrows(UnauthorizedActionException.class, () ->
                authRestController.login(new LoginRequest("NON_EXISTENT_USER", "password123")));

        // 7. Logout
        ApiResponse<Void> logoutResponse = authRestController.logout();
        assertTrue(logoutResponse.isSuccess());
    }

    @Test
    @DisplayName("Phase 5 Part 1: IncidentRestController reportAndAutoAssign pipeline transitions to IN_PROGRESS via auto-assignment & duplicate co-assignment")
    void testIncidentRestControllerReportAndAutoAssignPipeline() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        IncidentCategory netCat = categoryRepository.findByName("NETWORK").orElseThrow();
        ItService restTestService = itServiceRepository.findByServiceCode("SVC-REST-TEST").orElseGet(() ->
                itServiceRepository.save(new ItService(
                        "SVC-REST-TEST",
                        "REST Pipeline Test Service",
                        "Service created for testing REST reporting pipeline",
                        BusinessCriticality.HIGH,
                        netCat,
                        "OPERATIONAL"
                ))
        );

        // 1. Report new primary incident via REST endpoint
        IncidentReportRequest reportRequest = new IncidentReportRequest(
                "API Pipeline: Core Backbone Router Interface Dropping Packets",
                "Severe packet degradation on primary backbone switch fabric.",
                reporter.getId(),
                restTestService.getId(),
                netCat.getId(),
                ImpactLevel.CRITICAL,
                UrgencyLevel.CRITICAL
        );

        ApiResponse<Incident> createResponse = incidentRestController.reportIncident(reportRequest);
        assertTrue(createResponse.isSuccess());
        Incident primaryInc = createResponse.getData();
        assertNotNull(primaryInc);
        assertNotNull(primaryInc.getIncidentCode());
        assertEquals(IncidentStatus.IN_PROGRESS, primaryInc.getStatus(),
                "Incident submitted via REST controller must be auto-assigned to IN_PROGRESS");
        assertNotNull(primaryInc.getAssignedTechnician(),
                "Technician must be assigned immediately via SmartCascadingAssignmentStrategy");

        String assignedTechCode = primaryInc.getAssignedTechnician().getUserIdCode();

        // 2. Report duplicate incident on same service via REST endpoint
        IncidentReportRequest duplicateRequest = new IncidentReportRequest(
                "API Pipeline: Primary backbone switch interface dropping packets",
                "Subnet users observing backbone router packet drops on core switch.",
                reporter.getId(),
                restTestService.getId(),
                netCat.getId(),
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        ApiResponse<Incident> dupResponse = incidentRestController.reportIncident(duplicateRequest);
        assertTrue(dupResponse.isSuccess());
        Incident dupInc = dupResponse.getData();
        assertNotNull(dupInc);
        assertEquals(IncidentStatus.IN_PROGRESS, dupInc.getStatus(),
                "Duplicate incident must also transition to IN_PROGRESS upon co-assignment");
        assertEquals(primaryInc.getIncidentCode(), dupInc.getDuplicateOfIncidentCode(),
                "Duplicate incident must be linked to master ticket");
        assertNotNull(dupInc.getAssignedTechnician());
        assertEquals(assignedTechCode, dupInc.getAssignedTechnician().getUserIdCode(),
                "Duplicate incident must be co-assigned to the exact same technician");
    }

    @Test
    @DisplayName("Phase 5 Part 1: IncidentRestController lifecycle endpoints (GET, notes, resolve, feedback, reopen)")
    void testIncidentRestControllerLifecycleEndpoints() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService authService = itServiceRepository.findByServiceCode("SVC-AUTH").orElseThrow();

        // 1. Create and auto-assign incident
        IncidentReportRequest reportRequest = new IncidentReportRequest(
                "REST Lifecycle: SSO Token Expiration Glitch",
                "SSO token expiring unexpectedly within 2 minutes of login.",
                reporter.getId(),
                authService.getId(),
                null,
                ImpactLevel.MEDIUM,
                UrgencyLevel.MEDIUM
        );
        ApiResponse<Incident> reported = incidentRestController.reportIncident(reportRequest);
        String code = reported.getData().getIncidentCode();

        // 2. GET by code
        ApiResponse<Incident> getByCode = incidentRestController.getIncidentByCode(code);
        assertTrue(getByCode.isSuccess());
        assertEquals(code, getByCode.getData().getIncidentCode());

        // 3. GET with filter
        ApiResponse<List<Incident>> filteredList = incidentRestController.getIncidents(
                IncidentStatus.IN_PROGRESS, null, authService.getId(), null, null, null, null, null);
        assertTrue(filteredList.isSuccess());
        assertTrue(filteredList.getData().stream().anyMatch(i -> code.equals(i.getIncidentCode())));

        // 4. Add investigation note
        ApiResponse<Incident> noted = incidentRestController.addInvestigationNote(code,
                new IncidentNoteRequest("T101", "Checking Kerberos expiration policy in AD."));
        assertTrue(noted.isSuccess());
        assertTrue(noted.getData().getInvestigationNotes().contains("Checking Kerberos expiration policy"));

        // 5. Resolve and close
        ApiResponse<Incident> resolved = incidentRestController.resolveIncident(code,
                new IncidentResolveRequest("T101", "Updated AD session token lifetime policy to 8 hours."));
        assertTrue(resolved.isSuccess());
        assertEquals(IncidentStatus.CLOSED, resolved.getData().getStatus());

        // 6. Submit feedback
        ApiResponse<Incident> feedbacked = incidentRestController.submitCustomerFeedback(code,
                new FeedbackRequest("E101", 5, "SSO session working reliably now!"));
        assertTrue(feedbacked.isSuccess());
        assertEquals(5, feedbacked.getData().getFeedbackRating());

        // 7. Reopen incident
        ApiResponse<Incident> reopened = incidentRestController.reopenIncident(code,
                new IncidentNoteRequest("E101", "Token glitch reoccurred after browser restart."));
        assertTrue(reopened.isSuccess());
        assertEquals(IncidentStatus.IN_PROGRESS, reopened.getData().getStatus());
    }

    @Test
    @DisplayName("Phase 5 Part 1: SystemLookup, Intelligence, and Notification REST controllers")
    void testSystemLookupAndNotificationAndIntelligenceControllers() {
        // 1. Lookup Endpoints
        ApiResponse<List<ItService>> servicesResp = systemLookupRestController.getAllServices();
        assertTrue(servicesResp.isSuccess());
        assertFalse(servicesResp.getData().isEmpty());

        ApiResponse<List<IncidentCategory>> catResp = systemLookupRestController.getAllCategories();
        assertTrue(catResp.isSuccess());
        assertFalse(catResp.getData().isEmpty());

        ApiResponse<List<Technician>> techResp = systemLookupRestController.getAllTechnicians();
        assertTrue(techResp.isSuccess());
        assertFalse(techResp.getData().isEmpty());

        ApiResponse<List<AuditLog>> auditResp = systemLookupRestController.getAuditLogs(null);
        assertTrue(auditResp.isSuccess());
        assertFalse(auditResp.getData().isEmpty());

        // 2. Intelligence SLA check
        ApiResponse<List<Incident>> slaResp = intelligenceRestController.runSlaCheckNow();
        assertTrue(slaResp.isSuccess());

        // 3. Notifications endpoints
        ApiResponse<List<NotificationRecord>> userNotifs = notificationRestController.getUserNotifications("E101");
        assertTrue(userNotifs.isSuccess());

        ApiResponse<List<NotificationRecord>> emails = notificationRestController.getSimulatedEmails();
        assertTrue(emails.isSuccess());

        if (!userNotifs.getData().isEmpty()) {
            Long notifId = userNotifs.getData().get(0).getId();
            ApiResponse<NotificationRecord> readResp = notificationRestController.markAsRead(notifId);
            assertTrue(readResp.isSuccess());
            assertTrue(readResp.getData().isReadStatus());
        }
    }

    @Test
    @DisplayName("Phase 6A: Multi-role authentication (E101-E103, T101-T103, A001) and REST filter combinations")
    void testMultiRoleAuthenticationAndRestFilterCombinations() {
        // 1. Multi-role Authentication validation across all 7 seed users
        List<String> allUserCodes = List.of("E101", "E102", "E103", "T101", "T102", "T103", "A001");
        for (String userCode : allUserCodes) {
            // Test standard password
            ApiResponse<UserSessionDTO> resp1 = authRestController.login(new LoginRequest(userCode, "password123"));
            assertTrue(resp1.isSuccess(), "Login with 'password123' must succeed for " + userCode);
            assertEquals(userCode, resp1.getData().getUserIdCode());

            // Test alternative demo password
            ApiResponse<UserSessionDTO> resp2 = authRestController.login(new LoginRequest(userCode, "pass123"));
            assertTrue(resp2.isSuccess(), "Login with 'pass123' must succeed for " + userCode);
            assertEquals(userCode, resp2.getData().getUserIdCode());
        }

        // Verify specific user attributes
        ApiResponse<UserSessionDTO> e103Resp = authRestController.login(new LoginRequest("E103", "password123"));
        assertEquals(Role.EMPLOYEE, e103Resp.getData().getRole());
        assertEquals("Operations & HR", e103Resp.getData().getDepartment());
        assertEquals("Priya Patel", e103Resp.getData().getName());

        ApiResponse<UserSessionDTO> t102Resp = authRestController.login(new LoginRequest("T102", "password123"));
        assertEquals(Role.TECHNICIAN, t102Resp.getData().getRole());
        assertEquals("Diana Prince", t102Resp.getData().getName());
        assertTrue(t102Resp.getData().getSkills().contains("DATABASE"));
        assertTrue(t102Resp.getData().getSkills().contains("APPLICATION"));

        ApiResponse<UserSessionDTO> a001Resp = authRestController.login(new LoginRequest("A001", "password123"));
        assertEquals(Role.ADMINISTRATOR, a001Resp.getData().getRole());
        assertTrue(a001Resp.getData().getPermissions().contains("ALL_PRIVILEGES"));

        // 2. REST Filter Combinations on incidentRestController.getIncidents(...)
        // Filter by Status: CLOSED (at least 5 seeded in Group A)
        ApiResponse<List<Incident>> closedResp = incidentRestController.getIncidents(
                IncidentStatus.CLOSED, null, null, null, null, null, null, null);
        assertTrue(closedResp.isSuccess());
        assertTrue(closedResp.getData().size() >= 5, "At least 5 closed incidents should be returned");
        assertTrue(closedResp.getData().stream().allMatch(i -> i.getStatus() == IncidentStatus.CLOSED));

        // Filter by Status: IN_PROGRESS (at least 8 seeded across Group B and C)
        ApiResponse<List<Incident>> inProgressResp = incidentRestController.getIncidents(
                IncidentStatus.IN_PROGRESS, null, null, null, null, null, null, null);
        assertTrue(inProgressResp.isSuccess());
        assertTrue(inProgressResp.getData().size() >= 8, "At least 8 in-progress incidents should be returned");
        assertTrue(inProgressResp.getData().stream().allMatch(i -> i.getStatus() == IncidentStatus.IN_PROGRESS));

        // Filter by Status: OPEN (INC-1011 unassigned)
        ApiResponse<List<Incident>> openResp = incidentRestController.getIncidents(
                IncidentStatus.OPEN, null, null, null, null, null, null, null);
        assertTrue(openResp.isSuccess());
        assertTrue(openResp.getData().size() >= 1, "At least 1 open incident should be returned");
        assertTrue(openResp.getData().stream().allMatch(i -> i.getStatus() == IncidentStatus.OPEN));

        // Filter by Priority: P1_CRITICAL (INC-1001, INC-1006, INC-1007)
        ApiResponse<List<Incident>> p1Resp = incidentRestController.getIncidents(
                null, PriorityLevel.P1_CRITICAL, null, null, null, null, null, null);
        assertTrue(p1Resp.isSuccess());
        assertTrue(p1Resp.getData().size() >= 3, "At least 3 P1 incidents should be returned");
        assertTrue(p1Resp.getData().stream().allMatch(i -> i.getPriority() == PriorityLevel.P1_CRITICAL));

        // Filter by Priority: P2_HIGH
        ApiResponse<List<Incident>> p2Resp = incidentRestController.getIncidents(
                null, PriorityLevel.P2_HIGH, null, null, null, null, null, null);
        assertTrue(p2Resp.isSuccess());
        assertTrue(p2Resp.getData().size() >= 5, "At least 5 P2 incidents should be returned");
        assertTrue(p2Resp.getData().stream().allMatch(i -> i.getPriority() == PriorityLevel.P2_HIGH));

        // Filter by Reporter Code: E101
        ApiResponse<List<Incident>> reporterResp = incidentRestController.getIncidents(
                null, null, null, null, null, null, null, "E101");
        assertTrue(reporterResp.isSuccess());
        assertTrue(reporterResp.getData().size() >= 4, "E101 should have at least 4 reported incidents");
        assertTrue(reporterResp.getData().stream().allMatch(i ->
                i.getReporter() != null && "E101".equalsIgnoreCase(i.getReporter().getUserIdCode())));

        // Filter by Technician Code: T102
        ApiResponse<List<Incident>> techResp = incidentRestController.getIncidents(
                null, null, null, null, null, null, "T102", null);
        assertTrue(techResp.isSuccess());
        assertTrue(techResp.getData().size() >= 4, "T102 should have at least 4 assigned incidents");
        assertTrue(techResp.getData().stream().allMatch(i ->
                i.getAssignedTechnician() != null && "T102".equalsIgnoreCase(i.getAssignedTechnician().getUserIdCode())));

        // Filter by Service: SVC-PAY
        ItService payService = itServiceRepository.findByServiceCode("SVC-PAY").orElseThrow();
        ApiResponse<List<Incident>> serviceResp = incidentRestController.getIncidents(
                null, null, payService.getId(), null, null, null, null, null);
        assertTrue(serviceResp.isSuccess());
        assertTrue(serviceResp.getData().size() >= 4, "SVC-PAY should have at least 4 incidents");
        assertTrue(serviceResp.getData().stream().allMatch(i ->
                i.getAffectedService() != null && payService.getId().equals(i.getAffectedService().getId())));

        // Multi-criteria composite filter: IN_PROGRESS + P1_CRITICAL + technicianCode T102
        ApiResponse<List<Incident>> compositeResp = incidentRestController.getIncidents(
                IncidentStatus.IN_PROGRESS, PriorityLevel.P1_CRITICAL, null, null, null, null, "T102", null);
        assertTrue(compositeResp.isSuccess());
        assertFalse(compositeResp.getData().isEmpty(), "Composite filtered incidents should not be empty");
        assertTrue(compositeResp.getData().stream().allMatch(i ->
                i.getStatus() == IncidentStatus.IN_PROGRESS &&
                i.getPriority() == PriorityLevel.P1_CRITICAL &&
                i.getAssignedTechnician() != null &&
                "T102".equalsIgnoreCase(i.getAssignedTechnician().getUserIdCode())));
    }

    @Test
    @DisplayName("Phase 6A: SLA Monitoring status transitions (ON_TRACK -> AT_RISK -> VIOLATED) and idempotency")
    void testSlaMonitoringTransitionsAndIdempotency() {
        // 1. Test idempotency on seeded breached incident (INC-1010)
        long initialViolatedLogs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc("INC-1010")
                .stream().filter(l -> "SLA_VIOLATED".equals(l.getAction())).count();
        assertTrue(initialViolatedLogs >= 1, "INC-1010 should have initial SLA_VIOLATED audit log");

        // Run SLA check multiple times
        slaMonitoringService.runSlaCheckNow();
        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();

        long postViolatedLogs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc("INC-1010")
                .stream().filter(l -> "SLA_VIOLATED".equals(l.getAction())).count();
        assertEquals(initialViolatedLogs, postViolatedLogs, "SLA monitoring must be idempotent for already violated incidents");

        // 2. Test idempotency on seeded at-risk incident (INC-1009)
        long initialWarningLogs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc("INC-1009")
                .stream().filter(l -> "SLA_WARNING".equals(l.getAction())).count();
        assertTrue(initialWarningLogs >= 1, "INC-1009 should have initial SLA_WARNING audit log");

        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();

        long postWarningLogs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc("INC-1009")
                .stream().filter(l -> "SLA_WARNING".equals(l.getAction())).count();
        assertEquals(initialWarningLogs, postWarningLogs, "SLA monitoring must be idempotent for at-risk incidents");

        // 3. Test dynamic lifecycle progression: ON_TRACK -> AT_RISK -> RESPONSE_VIOLATED
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService authService = itServiceRepository.findByServiceCode("SVC-AUTH").orElseThrow();

        Incident freshIncident = incidentManagementService.reportIncident(
                "SLA Dynamic Transition Lifecycle Test Ticket",
                "Testing real-time SLA state progression through monitoring checks",
                reporter.getId(),
                authService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );
        String ticketCode = freshIncident.getIncidentCode();
        assertEquals(SlaStatus.ON_TRACK, freshIncident.getSlaStatus());
        assertFalse(freshIncident.isSlaViolated());

        // Step A: Simulate 80% elapsed time into response window -> Triggers AT_RISK
        LocalDateTime now = LocalDateTime.now();
        freshIncident.setCreatedAt(now.minusMinutes(16));
        freshIncident.setResponseDeadline(now.plusMinutes(4));
        incidentRepository.saveAndFlush(freshIncident);

        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();

        Incident atRiskIncident = incidentRepository.findByIncidentCode(ticketCode).orElseThrow();
        assertEquals(SlaStatus.AT_RISK, atRiskIncident.getSlaStatus(), "Incident must transition to AT_RISK at >= 75% elapsed");
        assertFalse(atRiskIncident.isSlaViolated(), "Incident should not be flagged violated yet");

        long atRiskWarningCount = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(ticketCode)
                .stream().filter(l -> "SLA_WARNING".equals(l.getAction())).count();
        assertEquals(1, atRiskWarningCount, "Exactly 1 SLA_WARNING log should be created");

        // Idempotency check at AT_RISK
        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();
        long atRiskWarningCountAfter = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(ticketCode)
                .stream().filter(l -> "SLA_WARNING".equals(l.getAction())).count();
        assertEquals(1, atRiskWarningCountAfter, "No duplicate SLA_WARNING log should be created");

        // Step B: Simulate response deadline expiration -> Triggers RESPONSE_VIOLATED
        freshIncident.setResponseDeadline(now.minusMinutes(2));
        incidentRepository.saveAndFlush(freshIncident);

        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();

        Incident violatedIncident = incidentRepository.findByIncidentCode(ticketCode).orElseThrow();
        assertEquals(SlaStatus.RESPONSE_VIOLATED, violatedIncident.getSlaStatus(), "Incident must transition to RESPONSE_VIOLATED");
        assertTrue(violatedIncident.isSlaViolated(), "Incident slaViolated flag must be set to true");

        long violationCount = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(ticketCode)
                .stream().filter(l -> "SLA_VIOLATED".equals(l.getAction())).count();
        assertEquals(1, violationCount, "Exactly 1 SLA_VIOLATED log should be created");

        // Idempotency check at VIOLATED
        slaMonitoringService.runSlaCheckNow();
        incidentRepository.flush();
        long violationCountAfter = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(ticketCode)
                .stream().filter(l -> "SLA_VIOLATED".equals(l.getAction())).count();
        assertEquals(1, violationCountAfter, "No duplicate SLA_VIOLATED log should be created on subsequent monitoring runs");
    }

    @Test
    @DisplayName("Auto-Classification: Production outage on SVC-PAY auto-classifies as CRITICAL priority")
    void testAutoClassificationProductionOutageCritical() {
        Employee reporter = employeeRepository.findByUserIdCode("E101").orElseThrow();
        ItService payService = itServiceRepository.findByServiceCode("SVC-PAY").orElseThrow();

        IncidentReportRequest request = new IncidentReportRequest();
        request.setTitle("Payment gateway timeout blocking checkout");
        request.setDescription("Production checkout transactions are failing across all users due to payment gateway timeout.");
        request.setReporterId(reporter.getId());
        request.setServiceId(payService.getId());
        request.setImpact(null);
        request.setUrgency(null);

        ApiResponse<Incident> response = incidentRestController.reportIncident(request);
        assertTrue(response.isSuccess());
        Incident incident = response.getData();
        assertNotNull(incident);
        assertEquals(PriorityLevel.P1_CRITICAL, incident.getPriority(),
                "Production outage on Tier-1 SVC-PAY must auto-classify as CRITICAL priority (P1_CRITICAL)");
        assertEquals(ImpactLevel.CRITICAL, incident.getImpact(),
                "High scope on Tier-1 service must auto-classify impact as CRITICAL");
        assertEquals(UrgencyLevel.CRITICAL, incident.getUrgency(),
                "Outage/timeout symptoms must auto-classify urgency as CRITICAL");
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertNotNull(incident.getAssignedTechnician(), "Technician must be assigned automatically");

        // Verify audit log records auto-classification
        incidentRepository.flush();
        List<AuditLog> logs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(incident.getIncidentCode());
        boolean hasAutoClassifyAudit = logs.stream().anyMatch(l ->
                l.getDescription().contains("Auto-classified Impact=CRITICAL") && l.getDescription().contains("Urgency=CRITICAL"));
        assertTrue(hasAutoClassifyAudit, "Audit log must record auto-classification decision");
    }

    @Test
    @DisplayName("Auto-Classification: Minor localized issue auto-classifies as LOW or MEDIUM priority")
    void testAutoClassificationLocalizedIssueLowOrMedium() {
        Employee reporter = employeeRepository.findByUserIdCode("E102").orElseThrow();
        ItService portalService = itServiceRepository.findByServiceCode("SVC-PORTAL").orElseThrow();

        IncidentReportRequest request = new IncidentReportRequest();
        request.setTitle("Monitor DisplayPort flicker on docking station");
        request.setDescription("External monitor occasionally flickers when laptop is connected to desk docking station.");
        request.setReporterId(reporter.getId());
        request.setServiceId(portalService.getId());
        request.setImpact(null);
        request.setUrgency(null);

        ApiResponse<Incident> response = incidentRestController.reportIncident(request);
        assertTrue(response.isSuccess());
        Incident incident = response.getData();
        assertNotNull(incident);
        assertTrue(incident.getPriority() == PriorityLevel.P4_LOW || incident.getPriority() == PriorityLevel.P3_MEDIUM,
                "Localized docking flicker issue must auto-classify as LOW or MEDIUM priority, actual: " + incident.getPriority());
        assertEquals(ImpactLevel.LOW, incident.getImpact(),
                "Localized single-device issue must cap impact at LOW");
        assertEquals(UrgencyLevel.LOW, incident.getUrgency(),
                "Flicker symptom with no outage/crash must classify urgency as LOW");
        assertEquals(IncidentStatus.IN_PROGRESS, incident.getStatus());
        assertNotNull(incident.getAssignedTechnician(), "Technician must be assigned automatically");

        // Verify audit log records auto-classification
        incidentRepository.flush();
        List<AuditLog> logs = auditLogRepository.findByIncidentIncidentCodeOrderByTimestampDesc(incident.getIncidentCode());
        boolean hasAutoClassifyAudit = logs.stream().anyMatch(l ->
                l.getDescription().contains("Auto-classified Impact=LOW") && l.getDescription().contains("Urgency=LOW"));
        assertTrue(hasAutoClassifyAudit, "Audit log must record auto-classification decision");
    }

    @Test
    @DisplayName("Public Portal: /api/public/summary returns live services, duplicate cluster, and strictly consistent SLA metrics")
    void testPublicPortalSummaryEndpoint() {
        ApiResponse<java.util.Map<String, Object>> response = publicPortalRestController.getPublicSummary();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        java.util.Map<String, Object> data = response.getData();
        assertNotNull(data);

        // 1. Services health
        assertTrue(data.containsKey("services"));
        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> services = (List<java.util.Map<String, Object>>) data.get("services");
        assertNotNull(services);
        assertFalse(services.isEmpty(), "Seed services must be returned");
        assertTrue(services.size() <= 4, "Top services capped at 4");
        long totalResolved = 0;
        for (java.util.Map<String, Object> svc : services) {
            assertNotNull(svc.get("serviceCode"));
            assertNotNull(svc.get("name"));
            assertNotNull(svc.get("status"));
            assertNotNull(svc.get("resolvedCount"), "resolvedCount must be present");
            long resCount = ((Number) svc.get("resolvedCount")).longValue();
            assertTrue(resCount >= 0);
            totalResolved += resCount;
            assertTrue(List.of("Operational", "Warning", "Degraded").contains(svc.get("status")),
                    "Status must be Operational, Warning, or Degraded: " + svc.get("status"));
        }
        assertTrue(totalResolved > 0, "Total resolvedCount across services must be > 0");

        // Verify that seeded services with closed incidents have resolvedCount > 0
        java.util.Map<String, Long> resolvedByCode = new java.util.HashMap<>();
        for (java.util.Map<String, Object> svc : services) {
            resolvedByCode.put((String) svc.get("serviceCode"), ((Number) svc.get("resolvedCount")).longValue());
        }
        if (resolvedByCode.containsKey("SVC-PAY")) {
            assertTrue(resolvedByCode.get("SVC-PAY") > 0, "SVC-PAY must have non-zero resolvedCount");
        }
        if (resolvedByCode.containsKey("SVC-AUTH")) {
            assertTrue(resolvedByCode.get("SVC-AUTH") > 0, "SVC-AUTH must have non-zero resolvedCount");
        }
        if (resolvedByCode.containsKey("SVC-NET")) {
            assertTrue(resolvedByCode.get("SVC-NET") > 0, "SVC-NET must have non-zero resolvedCount");
        }
        if (resolvedByCode.containsKey("SVC-CLOUD")) {
            assertTrue(resolvedByCode.get("SVC-CLOUD") > 0, "SVC-CLOUD must have non-zero resolvedCount");
        }

        // 2. Duplicate cluster
        assertTrue(data.containsKey("duplicateCluster"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> cluster = (java.util.Map<String, Object>) data.get("duplicateCluster");
        assertNotNull(cluster);
        assertTrue((Integer) cluster.get("totalLinkedDuplicates") >= 0);

        // 3. SLA metrics
        assertTrue(data.containsKey("slaMetrics"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> sla = (java.util.Map<String, Object>) data.get("slaMetrics");
        assertNotNull(sla);
        int totalTracked = (Integer) sla.get("totalTracked");
        int onTrackCount = (Integer) sla.get("onTrackCount");
        int atRiskCount = (Integer) sla.get("atRiskCount");
        int breachedCount = (Integer) sla.get("breachedCount");
        assertTrue(totalTracked >= 0);
        assertTrue(onTrackCount >= 0);
        assertTrue(atRiskCount >= 0);
        assertTrue(breachedCount >= 0);

        int compliance = (Integer) sla.get("compliancePercent");
        int breach = (Integer) sla.get("breachPercent");
        assertEquals(100, compliance + breach, "compliancePercent + breachPercent must equal 100%");

        @SuppressWarnings("unchecked")
        List<String> onTrackCodes = (List<String>) sla.get("onTrackCodes");
        @SuppressWarnings("unchecked")
        List<String> breachedCodes = (List<String>) sla.get("breachedCodes");
        assertNotNull(onTrackCodes);
        assertNotNull(breachedCodes);

        // Crucial requirement: onTrackCodes and breachedCodes must NEVER overlap!
        for (String code : onTrackCodes) {
            assertFalse(breachedCodes.contains(code),
                    "onTrackCodes and breachedCodes must be strictly disjoint! Overlapping code: " + code);
        }

        // 4. Incident Volume & Resolution Trend
        assertTrue(data.containsKey("incidentTrend"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> trend = (java.util.Map<String, Object>) data.get("incidentTrend");
        assertNotNull(trend);
        assertEquals(14L, ((Number) trend.get("totalReported")).longValue());
        assertEquals(5L, ((Number) trend.get("totalResolved")).longValue());
        assertTrue(((Number) trend.get("resolutionRate")).longValue() >= 0);

        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> buckets = (List<java.util.Map<String, Object>>) trend.get("buckets");
        assertNotNull(buckets);
        assertEquals(6, buckets.size(), "Must contain exactly 6 two-hour buckets");
        long sumReported = 0;
        long sumResolved = 0;
        for (java.util.Map<String, Object> b : buckets) {
            assertNotNull(b.get("label"));
            long rep = ((Number) b.get("reportedCount")).longValue();
            long res = ((Number) b.get("resolvedCount")).longValue();
            assertTrue(rep >= 0);
            assertTrue(res >= 0);
            sumReported += rep;
            sumResolved += res;
        }
        assertEquals(14L, sumReported, "Sum of reported in buckets must match totalReported");
        assertEquals(5L, sumResolved, "Sum of resolved in buckets must match totalResolved");
    }

    @Test
    @DisplayName("Public Portal: /api/public/search directly looks up tickets by Incident Code")
    void testPublicPortalSearchDirectTicket() {
        List<Incident> all = incidentRepository.findAll();
        assertFalse(all.isEmpty(), "Incidents must exist from seeding");
        Incident sample = all.get(0);

        ApiResponse<java.util.Map<String, Object>> response = publicPortalRestController.searchPublic(sample.getIncidentCode());
        assertNotNull(response);
        assertTrue(response.isSuccess());
        java.util.Map<String, Object> data = response.getData();
        assertNotNull(data);
        assertEquals("TICKET", data.get("type"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> ticket = (java.util.Map<String, Object>) data.get("ticket");
        assertNotNull(ticket);
        assertEquals(sample.getIncidentCode(), ticket.get("incidentCode"));
        assertEquals(sample.getTitle(), ticket.get("title"));
    }

    @Test
    @DisplayName("Public Portal: /api/public/search returns matching knowledge base solutions and active matches for natural language queries")
    void testPublicPortalSearchKnowledgeAndActive() {
        ApiResponse<java.util.Map<String, Object>> response = publicPortalRestController.searchPublic("vpn connection authentication drop");
        assertNotNull(response);
        assertTrue(response.isSuccess());
        java.util.Map<String, Object> data = response.getData();
        assertNotNull(data);
        assertEquals("KNOWLEDGE", data.get("type"));

        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> matchedSolutions = (List<java.util.Map<String, Object>>) data.get("matchedSolutions");
        assertNotNull(matchedSolutions);

        @SuppressWarnings("unchecked")
        List<java.util.Map<String, Object>> activeMatches = (List<java.util.Map<String, Object>>) data.get("activeMatches");
        assertNotNull(activeMatches);
    }

    @Test
    @DisplayName("Public Portal: /api/public/knowledge-base returns verified resolutions from closed incidents")
    void testPublicPortalKnowledgeBaseEndpoint() {
        ApiResponse<List<java.util.Map<String, Object>>> response = publicPortalRestController.getKnowledgeBase();
        assertNotNull(response);
        assertTrue(response.isSuccess());
        List<java.util.Map<String, Object>> list = response.getData();
        assertNotNull(list);
        assertFalse(list.isEmpty(), "Closed seed incidents with resolutions must be present");

        for (java.util.Map<String, Object> item : list) {
            assertNotNull(item.get("incidentCode"));
            assertNotNull(item.get("title"));
            assertNotNull(item.get("serviceCode"));
            assertNotNull(item.get("resolutionDetails"));
            assertTrue(item.get("resolutionDetails").toString().trim().length() > 0, "Resolution details must not be empty");
        }
    }
}
