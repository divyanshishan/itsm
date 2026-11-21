package com.itsm.system.config;

import com.itsm.system.model.entity.AuditLog;
import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.IncidentCategory;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.entity.NotificationRecord;
import com.itsm.system.model.entity.SlaPolicy;
import com.itsm.system.model.entity.Technician;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.NotificationEventType;
import com.itsm.system.model.enums.PriorityLevel;
import com.itsm.system.model.enums.SlaStatus;
import com.itsm.system.model.enums.UrgencyLevel;
import com.itsm.system.repository.EmployeeRepository;
import com.itsm.system.repository.IncidentCategoryRepository;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.repository.NotificationRecordRepository;
import com.itsm.system.repository.SlaPolicyRepository;
import com.itsm.system.repository.TechnicianRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Enterprise Demo Scenario Seeder (Phase 6A).
 * Populates 14 high-variety enterprise incidents covering:
 * - Group A: 5 Historical CLOSED tickets for AI Historical Resolution Recommendations (Feature G)
 * - Group B: Active 3-ticket Duplicate Cluster with live co-assignment and cascading closure (Feature A)
 * - Group C: Active tickets showcasing all SLA statuses (ON_TRACK, AT_RISK, RESOLUTION_VIOLATED, RESPONSE_VIOLATED)
 *            and all priority levels (P1 to P4).
 */
@Component
@Order(2)
public class DemoScenarioSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoScenarioSeeder.class);

    private final IncidentRepository incidentRepository;
    private final EmployeeRepository employeeRepository;
    private final TechnicianRepository technicianRepository;
    private final ItServiceRepository itServiceRepository;
    private final IncidentCategoryRepository categoryRepository;
    private final SlaPolicyRepository slaPolicyRepository;
    private final NotificationRecordRepository notificationRecordRepository;

    public DemoScenarioSeeder(IncidentRepository incidentRepository,
                              EmployeeRepository employeeRepository,
                              TechnicianRepository technicianRepository,
                              ItServiceRepository itServiceRepository,
                              IncidentCategoryRepository categoryRepository,
                              SlaPolicyRepository slaPolicyRepository,
                              NotificationRecordRepository notificationRecordRepository) {
        this.incidentRepository = incidentRepository;
        this.employeeRepository = employeeRepository;
        this.technicianRepository = technicianRepository;
        this.itServiceRepository = itServiceRepository;
        this.categoryRepository = categoryRepository;
        this.slaPolicyRepository = slaPolicyRepository;
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (incidentRepository.count() > 0) {
            log.info("Incidents already present in database (count: {}). Skipping DemoScenarioSeeder.",
                    incidentRepository.count());
            return;
        }

        log.info("Starting Phase 6A Enterprise Demo Scenario Seeder (14 realistic incidents)...");

        // Load reference entities
        Map<String, Employee> employees = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getUserIdCode, Function.identity()));
        Map<String, Technician> technicians = technicianRepository.findAll().stream()
                .collect(Collectors.toMap(Technician::getUserIdCode, Function.identity()));
        Map<String, ItService> services = itServiceRepository.findAll().stream()
                .collect(Collectors.toMap(ItService::getServiceCode, Function.identity()));
        Map<String, IncidentCategory> categories = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(IncidentCategory::getName, Function.identity()));
        Map<PriorityLevel, SlaPolicy> slaPolicies = slaPolicyRepository.findAll().stream()
                .collect(Collectors.toMap(SlaPolicy::getPriorityLevel, Function.identity()));

        Employee e101 = employees.get("E101");
        Employee e102 = employees.get("E102");
        Employee e103 = employees.get("E103");

        Technician t101 = technicians.get("T101");
        Technician t102 = technicians.get("T102");
        Technician t103 = technicians.get("T103");

        ItService svcPay = services.get("SVC-PAY");
        ItService svcAuth = services.get("SVC-AUTH");
        ItService svcNet = services.get("SVC-NET");
        ItService svcCloud = services.get("SVC-CLOUD");
        ItService svcPortal = services.get("SVC-PORTAL");

        IncidentCategory catApp = categories.get("APPLICATION");
        IncidentCategory catDb = categories.get("DATABASE");
        IncidentCategory catNet = categories.get("NETWORK");
        IncidentCategory catCloud = categories.get("CLOUD");
        IncidentCategory catAuth = categories.get("AUTHENTICATION");
        IncidentCategory catStorage = categories.get("STORAGE");
        IncidentCategory catHw = categories.get("HARDWARE");

        LocalDateTime now = LocalDateTime.now();

        // =====================================================================================
        // GROUP A: Historical CLOSED Knowledge Base (5 Tickets — Powers Feature G Suggestions)
        // =====================================================================================

        // 1. Payment Gateway Historical Fix #1 (90%+ match for active payment issue)
        LocalDateTime t1Created = now.minusDays(5);
        LocalDateTime t1Ack = t1Created.plusMinutes(3);
        LocalDateTime t1Closed = t1Created.plusMinutes(24);
        Incident inc1 = new Incident(
                "INC-1001",
                "Payment gateway connection reset and checkout API timeout error",
                "Customers unable to complete credit card checkout due to payment gateway TLS handshake timeout and connection pool exhaustion on production nodes.",
                e101, catApp, svcPay,
                ImpactLevel.CRITICAL, UrgencyLevel.HIGH, 4.0, PriorityLevel.P1_CRITICAL,
                slaPolicies.get(PriorityLevel.P1_CRITICAL),
                t1Created.plusMinutes(5), t1Created.plusMinutes(30)
        );
        inc1.setCreatedAt(t1Created);
        inc1.setAcknowledgedAt(t1Ack);
        inc1.setResolvedAt(t1Closed);
        inc1.setClosedAt(t1Closed);
        inc1.setStatus(IncidentStatus.CLOSED);
        inc1.setSlaStatus(SlaStatus.MET);
        inc1.setAssignedTechnician(t102);
        inc1.setInvestigationNotes("[T102 Diana Prince]: Verified upstream TLS 1.3 cipher mismatch and exhausted HikariCP threads on payment-gateway-prod-02.");
        inc1.setResolutionDetails("Rotated expired mTLS gateway certificate in Vault, increased HikariCP maxPoolSize from 20 to 60, and restarted payment-worker pods. Verified zero checkout errors.");
        inc1.setFeedbackRating(5);
        inc1.setCustomerFeedback("Fast resolution during peak billing hours, thank you!");
        addAudit(inc1, t1Created, "E101", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Alice Johnson");
        addAudit(inc1, t1Ack, "T102", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to technician Diana Prince (T102)");
        addAudit(inc1, t1Closed, "T102", "RESOLVE_AND_CLOSE", "IN_PROGRESS", "CLOSED", "Incident resolved: mTLS cert rotated and HikariCP pool resized.");
        saveIncidentAndNotifications(inc1, "Incident Resolved and Closed: INC-1001", NotificationEventType.INCIDENT_RESOLVED_CLOSED);

        // 2. Payment Gateway Historical Fix #2 (Partial ~55% match for ranking)
        LocalDateTime t2Created = now.minusDays(4);
        LocalDateTime t2Closed = t2Created.plusMinutes(75);
        Incident inc2 = new Incident(
                "INC-1002",
                "Checkout payment transaction deadlock in billing database",
                "Intermittent checkout timeout errors when concurrent invoice payments lock the billing ledger table.",
                e102, catDb, svcPay,
                ImpactLevel.HIGH, UrgencyLevel.HIGH, 3.0, PriorityLevel.P2_HIGH,
                slaPolicies.get(PriorityLevel.P2_HIGH),
                t2Created.plusMinutes(15), t2Created.plusMinutes(120)
        );
        inc2.setCreatedAt(t2Created);
        inc2.setAcknowledgedAt(t2Created.plusMinutes(10));
        inc2.setResolvedAt(t2Closed);
        inc2.setClosedAt(t2Closed);
        inc2.setStatus(IncidentStatus.CLOSED);
        inc2.setSlaStatus(SlaStatus.MET);
        inc2.setAssignedTechnician(t102);
        inc2.setResolutionDetails("Added composite index idx_ledger_tx_status on billing_transactions and lowered transaction isolation level from SERIALIZABLE to READ_COMMITTED.");
        inc2.setFeedbackRating(4);
        inc2.setCustomerFeedback("Checkout latency is back to normal.");
        addAudit(inc2, t2Created, "E102", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Bob Smith");
        addAudit(inc2, t2Created.plusMinutes(10), "T102", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to Diana Prince");
        addAudit(inc2, t2Closed, "T102", "RESOLVE_AND_CLOSE", "IN_PROGRESS", "CLOSED", "Database deadlock resolved with index optimization.");
        saveIncidentAndNotifications(inc2, "Incident Resolved and Closed: INC-1002", NotificationEventType.INCIDENT_RESOLVED_CLOSED);

        // 3. VPN & Core Network Historical Fix (Powers Feature G for T101)
        LocalDateTime t3Created = now.minusDays(3);
        LocalDateTime t3Closed = t3Created.plusMinutes(55);
        Incident inc3 = new Incident(
                "INC-1003",
                "Corporate VPN gateway dropping remote user tunnel connections",
                "Remote employees experiencing frequent Cisco AnyConnect VPN disconnects and BGP route flapping on primary firewall.",
                e103, catNet, svcNet,
                ImpactLevel.HIGH, UrgencyLevel.HIGH, 3.0, PriorityLevel.P2_HIGH,
                slaPolicies.get(PriorityLevel.P2_HIGH),
                t3Created.plusMinutes(15), t3Created.plusMinutes(120)
        );
        inc3.setCreatedAt(t3Created);
        inc3.setAcknowledgedAt(t3Created.plusMinutes(8));
        inc3.setResolvedAt(t3Closed);
        inc3.setClosedAt(t3Closed);
        inc3.setStatus(IncidentStatus.CLOSED);
        inc3.setSlaStatus(SlaStatus.MET);
        inc3.setAssignedTechnician(t101);
        inc3.setResolutionDetails("Lowered IPsec tunnel MTU from 1500 to 1380 bytes to prevent packet fragmentation, cleared stale IKEv2 SA sessions, and stabilized BGP hold timers.");
        inc3.setFeedbackRating(5);
        inc3.setCustomerFeedback("VPN connection has been rock solid all afternoon.");
        addAudit(inc3, t3Created, "E103", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Priya Patel");
        addAudit(inc3, t3Created.plusMinutes(8), "T101", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to Charlie Davis");
        addAudit(inc3, t3Closed, "T101", "RESOLVE_AND_CLOSE", "IN_PROGRESS", "CLOSED", "MTU lowered and BGP stabilized.");
        saveIncidentAndNotifications(inc3, "Incident Resolved and Closed: INC-1003", NotificationEventType.INCIDENT_RESOLVED_CLOSED);

        // 4. Cloud / Kubernetes Historical Fix (Powers Feature G for T101)
        LocalDateTime t4Created = now.minusDays(2);
        LocalDateTime t4Closed = t4Created.plusMinutes(80);
        Incident inc4 = new Incident(
                "INC-1004",
                "Kubernetes ingress controller 502 Bad Gateway pod eviction",
                "Production NGINX ingress pods evicted due to node memory pressure causing 502 errors on external web services.",
                e101, catCloud, svcCloud,
                ImpactLevel.HIGH, UrgencyLevel.MEDIUM, 2.7, PriorityLevel.P2_HIGH,
                slaPolicies.get(PriorityLevel.P2_HIGH),
                t4Created.plusMinutes(15), t4Created.plusMinutes(120)
        );
        inc4.setCreatedAt(t4Created);
        inc4.setAcknowledgedAt(t4Created.plusMinutes(12));
        inc4.setResolvedAt(t4Closed);
        inc4.setClosedAt(t4Closed);
        inc4.setStatus(IncidentStatus.CLOSED);
        inc4.setSlaStatus(SlaStatus.MET);
        inc4.setAssignedTechnician(t101);
        inc4.setResolutionDetails("Increased NGINX worker memory limits to 2Gi, configured HorizontalPodAutoscaler (min 3, max 8 replicas), and cordoned degraded worker node.");
        inc4.setFeedbackRating(5);
        inc4.setCustomerFeedback("Great root cause analysis.");
        addAudit(inc4, t4Created, "E101", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Alice Johnson");
        addAudit(inc4, t4Created.plusMinutes(12), "T101", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to Charlie Davis");
        addAudit(inc4, t4Closed, "T101", "RESOLVE_AND_CLOSE", "IN_PROGRESS", "CLOSED", "Kubernetes ingress limits and HPA configured.");
        saveIncidentAndNotifications(inc4, "Incident Resolved and Closed: INC-1004", NotificationEventType.INCIDENT_RESOLVED_CLOSED);

        // 5. Active Directory / SSO Historical Fix (Powers Feature G for T103)
        LocalDateTime t5Created = now.minusDays(1);
        LocalDateTime t5Closed = t5Created.plusMinutes(65);
        Incident inc5 = new Incident(
                "INC-1005",
                "Single Sign-On SAML token expiration and Active Directory lockout",
                "Users unable to authenticate into internal portal due to Kerberos clock skew and SAML assertion validation failure.",
                e102, catAuth, svcAuth,
                ImpactLevel.MEDIUM, UrgencyLevel.HIGH, 2.8, PriorityLevel.P2_HIGH,
                slaPolicies.get(PriorityLevel.P2_HIGH),
                t5Created.plusMinutes(15), t5Created.plusMinutes(120)
        );
        inc5.setCreatedAt(t5Created);
        inc5.setAcknowledgedAt(t5Created.plusMinutes(10));
        inc5.setResolvedAt(t5Closed);
        inc5.setClosedAt(t5Closed);
        inc5.setStatus(IncidentStatus.CLOSED);
        inc5.setSlaStatus(SlaStatus.MET);
        inc5.setAssignedTechnician(t103);
        inc5.setResolutionDetails("Synchronized NTP chrony daemon across primary and secondary Domain Controllers and refreshed SAML IDP signing metadata certificate.");
        inc5.setFeedbackRating(4);
        inc5.setCustomerFeedback("SSO login working smoothly now.");
        addAudit(inc5, t5Created, "E102", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Bob Smith");
        addAudit(inc5, t5Created.plusMinutes(10), "T103", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to Evan Wright");
        addAudit(inc5, t5Closed, "T103", "RESOLVE_AND_CLOSE", "IN_PROGRESS", "CLOSED", "NTP chrony synchronized and SAML metadata refreshed.");
        saveIncidentAndNotifications(inc5, "Incident Resolved and Closed: INC-1005", NotificationEventType.INCIDENT_RESOLVED_CLOSED);

        // =====================================================================================
        // GROUP B: Active Duplicate Cluster (IN_PROGRESS — Powers Feature A Live Cascading Closure)
        // =====================================================================================

        // 6. Active Master Incident (IN_PROGRESS, ON_TRACK)
        LocalDateTime t6Created = now.minusMinutes(12);
        LocalDateTime t6Ack = now.minusMinutes(10);
        Incident inc6 = new Incident(
                "INC-1006",
                "Production payment gateway timeout and checkout connection reset",
                "Live checkout transactions failing with payment gateway connection reset and TLS handshake timeout on production cluster.",
                e101, catApp, svcPay,
                ImpactLevel.CRITICAL, UrgencyLevel.CRITICAL, 4.0, PriorityLevel.P1_CRITICAL,
                slaPolicies.get(PriorityLevel.P1_CRITICAL),
                t6Created.plusMinutes(5), t6Created.plusMinutes(30)
        );
        inc6.setCreatedAt(t6Created);
        inc6.setAcknowledgedAt(t6Ack);
        inc6.setStatus(IncidentStatus.IN_PROGRESS);
        inc6.setSlaStatus(SlaStatus.ON_TRACK);
        inc6.setAssignedTechnician(t102);
        inc6.setInvestigationNotes("[T102 Diana Prince]: Investigating live checkout latency spike; cluster of 2 duplicate reports linked automatically.");
        addAudit(inc6, t6Created, "E101", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Alice Johnson");
        addAudit(inc6, t6Ack, "T102", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to technician Diana Prince (T102)");
        saveIncidentAndNotifications(inc6, "Technician Assigned: INC-1006", NotificationEventType.TECHNICIAN_ASSIGNED);

        // 7. Linked Duplicate #1 (IN_PROGRESS, duplicateOfIncidentCode = INC-1006)
        LocalDateTime t7Created = now.minusMinutes(8);
        LocalDateTime t7Ack = now.minusMinutes(8);
        Incident inc7 = new Incident(
                "INC-1007",
                "Checkout payment gateway connection reset error for customers",
                "Multiple customers reporting checkout failure due to payment gateway timeout and connection reset on production.",
                e102, catApp, svcPay,
                ImpactLevel.HIGH, UrgencyLevel.CRITICAL, 3.7, PriorityLevel.P1_CRITICAL,
                slaPolicies.get(PriorityLevel.P1_CRITICAL),
                t7Created.plusMinutes(5), t7Created.plusMinutes(30)
        );
        inc7.setCreatedAt(t7Created);
        inc7.setAcknowledgedAt(t7Ack);
        inc7.setStatus(IncidentStatus.IN_PROGRESS);
        inc7.setSlaStatus(SlaStatus.ON_TRACK);
        inc7.setAssignedTechnician(t102);
        inc7.setDuplicateOfIncidentCode("INC-1006");
        addAudit(inc7, t7Created, "E102", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Bob Smith");
        addAudit(inc7, t7Ack, "SYSTEM", "DUPLICATE_LINKED", "OPEN", "IN_PROGRESS",
                "[DUPLICATE LINKED] Identified as duplicate of active incident INC-1006. Co-assigned to technician T102 and moved to IN_PROGRESS.");
        saveIncidentAndNotifications(inc7, "Technician Co-Assigned (Duplicate Link): INC-1007", NotificationEventType.TECHNICIAN_ASSIGNED);

        // 8. Linked Duplicate #2 (IN_PROGRESS, duplicateOfIncidentCode = INC-1006)
        LocalDateTime t8Created = now.minusMinutes(5);
        LocalDateTime t8Ack = now.minusMinutes(5);
        Incident inc8 = new Incident(
                "INC-1008",
                "Unable to process credit card checkout payment gateway timeout",
                "Payment gateway connection reset and timeout blocking credit card checkout orders.",
                e103, catApp, svcPay,
                ImpactLevel.HIGH, UrgencyLevel.HIGH, 3.0, PriorityLevel.P2_HIGH,
                slaPolicies.get(PriorityLevel.P2_HIGH),
                t8Created.plusMinutes(15), t8Created.plusMinutes(120)
        );
        inc8.setCreatedAt(t8Created);
        inc8.setAcknowledgedAt(t8Ack);
        inc8.setStatus(IncidentStatus.IN_PROGRESS);
        inc8.setSlaStatus(SlaStatus.ON_TRACK);
        inc8.setAssignedTechnician(t102);
        inc8.setDuplicateOfIncidentCode("INC-1006");
        addAudit(inc8, t8Created, "E103", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Priya Patel");
        addAudit(inc8, t8Ack, "SYSTEM", "DUPLICATE_LINKED", "OPEN", "IN_PROGRESS",
                "[DUPLICATE LINKED] Identified as duplicate of active incident INC-1006. Co-assigned to technician T102 and moved to IN_PROGRESS.");
        saveIncidentAndNotifications(inc8, "Technician Co-Assigned (Duplicate Link): INC-1008", NotificationEventType.TECHNICIAN_ASSIGNED);

        // =====================================================================================
        // GROUP C: Active Incidents Across All SLA States & All Priorities
        // =====================================================================================

        // 9. AT_RISK Incident (IN_PROGRESS — For T101 Charlie Davis, shows Amber AT_RISK Badge)
        LocalDateTime t9Created = now.minusHours(3).minusMinutes(50);
        LocalDateTime t9Ack = now.minusHours(3).minusMinutes(40);
        Incident inc9 = new Incident(
                "INC-1009",
                "Corporate VPN tunnel dropping remote employee sessions intermittently",
                "Remote staff VPN gateway dropping tunnel connections and experiencing packet fragmentation issues.",
                e103, catNet, svcNet,
                ImpactLevel.HIGH, UrgencyLevel.HIGH, 3.0, PriorityLevel.P2_HIGH,
                slaPolicies.get(PriorityLevel.P2_HIGH),
                t9Created.plusMinutes(15), now.plusMinutes(10)
        );
        inc9.setCreatedAt(t9Created);
        inc9.setAcknowledgedAt(t9Ack);
        inc9.setStatus(IncidentStatus.IN_PROGRESS);
        inc9.setSlaStatus(SlaStatus.AT_RISK);
        inc9.setAssignedTechnician(t101);
        inc9.setInvestigationNotes("[T101 Charlie Davis]: Monitoring IPsec packet drops; resolution deadline approaching in 10 minutes.");
        addAudit(inc9, t9Created, "E103", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Priya Patel");
        addAudit(inc9, t9Ack, "T101", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to Charlie Davis (T101)");
        addAudit(inc9, now.minusMinutes(15), "SYSTEM", "SLA_WARNING", "IN_PROGRESS", "IN_PROGRESS",
                "[SLA WARNING] Incident resolution deadline is within 15 minutes! Approaching breach threshold.");
        saveIncidentAndNotifications(inc9, "SLA WARNING: Incident INC-1009 Approaching Deadline", NotificationEventType.SLA_APPROACHING);

        // 10. RESOLUTION_VIOLATED Incident (IN_PROGRESS — For T101, shows Red RESOL BREACH Badge)
        LocalDateTime t10Created = now.minusHours(6);
        LocalDateTime t10Ack = now.minusHours(5).minusMinutes(50);
        Incident inc10 = new Incident(
                "INC-1010",
                "Kubernetes production ingress controller 502 Bad Gateway errors",
                "Production NGINX ingress pods evicting under memory pressure and returning 502 Bad Gateway responses.",
                e102, catCloud, svcCloud,
                ImpactLevel.HIGH, UrgencyLevel.MEDIUM, 2.7, PriorityLevel.P2_HIGH,
                slaPolicies.get(PriorityLevel.P2_HIGH),
                t10Created.plusMinutes(15), now.minusMinutes(45)
        );
        inc10.setCreatedAt(t10Created);
        inc10.setAcknowledgedAt(t10Ack);
        inc10.setStatus(IncidentStatus.IN_PROGRESS);
        inc10.setSlaStatus(SlaStatus.RESOLUTION_VIOLATED);
        inc10.setSlaViolated(true);
        inc10.setEscalated(true);
        inc10.setAssignedTechnician(t101);
        inc10.setInvestigationNotes("[T101 Charlie Davis]: Major node instability in worker cluster; SLA resolution deadline breached.");
        addAudit(inc10, t10Created, "E102", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Bob Smith");
        addAudit(inc10, t10Ack, "T101", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to Charlie Davis (T101)");
        addAudit(inc10, now.minusMinutes(45), "SYSTEM", "SLA_VIOLATED", "IN_PROGRESS", "IN_PROGRESS",
                "[SLA RESOLUTION BREACH] Incident breached resolution deadline of " + now.minusMinutes(45) + ". Escalated.");
        saveIncidentAndNotifications(inc10, "SLA VIOLATION ALERT: Incident INC-1010", NotificationEventType.SLA_VIOLATED);

        // 11. RESPONSE_VIOLATED Unassigned Incident (OPEN — Shows Red RESP BREACH Badge)
        LocalDateTime t11Created = now.minusHours(2);
        Incident inc11 = new Incident(
                "INC-1011",
                "Active Directory SSO SAML token validation failure on HR portal",
                "Employees locked out of internal portal due to Active Directory Kerberos clock skew and SAML assertion error.",
                e101, catAuth, svcAuth,
                ImpactLevel.MEDIUM, UrgencyLevel.HIGH, 2.8, PriorityLevel.P2_HIGH,
                slaPolicies.get(PriorityLevel.P2_HIGH),
                now.minusMinutes(30), now.plusHours(1)
        );
        inc11.setCreatedAt(t11Created);
        inc11.setAcknowledgedAt(null);
        inc11.setAssignedTechnician(null);
        inc11.setStatus(IncidentStatus.OPEN);
        inc11.setSlaStatus(SlaStatus.RESPONSE_VIOLATED);
        inc11.setSlaViolated(true);
        inc11.setEscalated(true);
        addAudit(inc11, t11Created, "E101", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Alice Johnson");
        addAudit(inc11, now.minusMinutes(30), "SYSTEM", "SLA_VIOLATED", "OPEN", "OPEN",
                "[SLA RESPONSE BREACH] Incident was not acknowledged/assigned within response target of 15m. Response deadline was " + now.minusMinutes(30) + ". Escalated.");
        saveIncidentAndNotifications(inc11, "SLA RESPONSE BREACH: Unassigned Ticket INC-1011", NotificationEventType.SLA_VIOLATED);

        // 12. P3 - MEDIUM Priority Active Incident (IN_PROGRESS, ON_TRACK — Assigned to T103 Evan Wright)
        LocalDateTime t12Created = now.minusHours(1);
        LocalDateTime t12Ack = now.minusMinutes(45);
        Incident inc12 = new Incident(
                "INC-1012",
                "NAS backup storage array RAID rebuild latency warning",
                "Secondary SAN storage volume reporting degraded read IOPS during scheduled incremental snapshot.",
                e102, catStorage, svcCloud,
                ImpactLevel.MEDIUM, UrgencyLevel.MEDIUM, 2.0, PriorityLevel.P3_MEDIUM,
                slaPolicies.get(PriorityLevel.P3_MEDIUM),
                t12Created.plusMinutes(60), now.plusHours(7)
        );
        inc12.setCreatedAt(t12Created);
        inc12.setAcknowledgedAt(t12Ack);
        inc12.setStatus(IncidentStatus.IN_PROGRESS);
        inc12.setSlaStatus(SlaStatus.ON_TRACK);
        inc12.setAssignedTechnician(t103);
        inc12.setInvestigationNotes("[T103 Evan Wright]: Checking SAN disk parity checks and IOPS metrics.");
        addAudit(inc12, t12Created, "E102", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Bob Smith");
        addAudit(inc12, t12Ack, "T103", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to Evan Wright (T103)");
        saveIncidentAndNotifications(inc12, "Technician Assigned: INC-1012", NotificationEventType.TECHNICIAN_ASSIGNED);

        // 13. P4 - LOW Priority Active Incident (IN_PROGRESS, ON_TRACK — Assigned to T103 Evan Wright)
        LocalDateTime t13Created = now.minusHours(2);
        LocalDateTime t13Ack = now.minusHours(1);
        Incident inc13 = new Incident(
                "INC-1013",
                "Workstation docking station dual-monitor DisplayPort flicker",
                "3rd floor conference room USB-C docking station intermittently blanking secondary 4K display.",
                e103, catHw, svcPortal,
                ImpactLevel.LOW, UrgencyLevel.LOW, 1.0, PriorityLevel.P4_LOW,
                slaPolicies.get(PriorityLevel.P4_LOW),
                t13Created.plusMinutes(240), now.plusHours(22)
        );
        inc13.setCreatedAt(t13Created);
        inc13.setAcknowledgedAt(t13Ack);
        inc13.setStatus(IncidentStatus.IN_PROGRESS);
        inc13.setSlaStatus(SlaStatus.ON_TRACK);
        inc13.setAssignedTechnician(t103);
        inc13.setInvestigationNotes("[T103 Evan Wright]: Testing DisplayPort cable replacement and firmware update on Dell dock.");
        addAudit(inc13, t13Created, "E103", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Priya Patel");
        addAudit(inc13, t13Ack, "T103", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to Evan Wright (T103)");
        saveIncidentAndNotifications(inc13, "Technician Assigned: INC-1013", NotificationEventType.TECHNICIAN_ASSIGNED);

        // 14. Live SLA Breach Time-Bomb Incident (IN_PROGRESS, ON_TRACK — Assigned to T102 Diana Prince)
        LocalDateTime t14Created = now.minusMinutes(30);
        LocalDateTime t14Ack = now.minusMinutes(25);
        Incident inc14 = new Incident(
                "INC-1014",
                "Monthly financial reporting export CSV encoding mismatch",
                "UTF-8 currency symbols rendering incorrectly in automated ERP batch export.",
                e101, catApp, svcPay,
                ImpactLevel.MEDIUM, UrgencyLevel.LOW, 2.2, PriorityLevel.P3_MEDIUM,
                slaPolicies.get(PriorityLevel.P3_MEDIUM),
                t14Created.plusMinutes(60), now.plusMinutes(90)
        );
        inc14.setCreatedAt(t14Created);
        inc14.setAcknowledgedAt(t14Ack);
        inc14.setStatus(IncidentStatus.IN_PROGRESS);
        inc14.setSlaStatus(SlaStatus.ON_TRACK);
        inc14.setAssignedTechnician(t102);
        inc14.setInvestigationNotes("[T102 Diana Prince]: Investigating locale parser and export template charset.");
        addAudit(inc14, t14Created, "E101", "INCIDENT_REPORTED", null, "OPEN", "Incident reported by Alice Johnson");
        addAudit(inc14, t14Ack, "T102", "TECHNICIAN_ASSIGNED", "OPEN", "IN_PROGRESS", "Assigned to Diana Prince (T102)");
        saveIncidentAndNotifications(inc14, "Technician Assigned: INC-1014", NotificationEventType.TECHNICIAN_ASSIGNED);

        // Update active workload counts for technicians
        t101.setCurrentWorkload(2); // inc9, inc10
        t102.setCurrentWorkload(4); // inc6, inc7, inc8, inc14
        t103.setCurrentWorkload(2); // inc12, inc13
        technicianRepository.save(t101);
        technicianRepository.save(t102);
        technicianRepository.save(t103);

        log.info("DemoScenarioSeeder: Successfully seeded 14 rich enterprise incidents, audit histories, and notifications.");
    }

    private void addAudit(Incident incident, LocalDateTime timestamp, String performedBy, String action,
                          String prevStatus, String newStatus, String description) {
        AuditLog audit = new AuditLog(incident, timestamp, performedBy, action, prevStatus, newStatus, description);
        incident.addAuditLog(audit);
    }

    private void saveIncidentAndNotifications(Incident incident, String subject, NotificationEventType eventType) {
        incidentRepository.save(incident);

        // Seed Dashboard Notification for Reporter
        if (incident.getReporter() != null) {
            NotificationRecord notifDashReporter = new NotificationRecord(
                    incident.getReporter().getUserIdCode(),
                    incident.getReporter().getEmail(),
                    incident.getIncidentCode(),
                    eventType,
                    "DASHBOARD",
                    subject,
                    "Incident update for ticket " + incident.getIncidentCode() + ": " + incident.getTitle()
            );
            notifDashReporter.setCreatedAt(incident.getCreatedAt());
            notificationRecordRepository.save(notifDashReporter);
        }

        // Seed Dashboard Notification for Assigned Technician
        if (incident.getAssignedTechnician() != null) {
            NotificationRecord notifDashTech = new NotificationRecord(
                    incident.getAssignedTechnician().getUserIdCode(),
                    incident.getAssignedTechnician().getEmail(),
                    incident.getIncidentCode(),
                    eventType,
                    "DASHBOARD",
                    subject,
                    "Assigned ticket " + incident.getIncidentCode() + ": " + incident.getTitle()
            );
            notifDashTech.setCreatedAt(incident.getCreatedAt());
            notificationRecordRepository.save(notifDashTech);
        }

        // Seed Simulated Email Record
        if (incident.getReporter() != null) {
            NotificationRecord notifEmail = new NotificationRecord(
                    incident.getReporter().getUserIdCode(),
                    incident.getReporter().getEmail(),
                    incident.getIncidentCode(),
                    eventType,
                    "SIMULATED_EMAIL",
                    "[I-ITSM " + incident.getIncidentCode() + "] " + subject,
                    "Dear " + incident.getReporter().getName() + ",\n\nYour incident " + incident.getIncidentCode() +
                            " ('" + incident.getTitle() + "') status: " + incident.getStatus() +
                            (incident.getResolutionDetails() != null ? "\nResolution: " + incident.getResolutionDetails() : "")
            );
            notifEmail.setCreatedAt(incident.getCreatedAt());
            notificationRecordRepository.save(notifEmail);
        }
    }
}
