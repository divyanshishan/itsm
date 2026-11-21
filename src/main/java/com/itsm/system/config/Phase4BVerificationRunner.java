package com.itsm.system.config;

import com.itsm.system.dto.SuggestedResolutionDTO;
import com.itsm.system.model.entity.Employee;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.UrgencyLevel;
import com.itsm.system.repository.EmployeeRepository;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.service.IncidentLifecycleService;
import com.itsm.system.service.IncidentManagementService;
import com.itsm.system.service.intelligent.ResolutionRecommendationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup verification runner for Phase 4B:
 * 1. Intelligent Feature A: Duplicate Incident Detection, Co-Assignment to same Technician, and Cascading Cluster Closure.
 * 2. Intelligent Feature G: Suggested Historical Resolutions (returning ALL available closed solutions for the technician to inspect and choose from).
 */
@Component
@Order(5)
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "itsm.verification.runners.enabled", havingValue = "true", matchIfMissing = false)
public class Phase4BVerificationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Phase4BVerificationRunner.class);

    private final IncidentManagementService incidentManagementService;
    private final IncidentLifecycleService incidentLifecycleService;
    private final ResolutionRecommendationService resolutionRecommendationService;
    private final EmployeeRepository employeeRepository;
    private final ItServiceRepository itServiceRepository;
    private final IncidentRepository incidentRepository;

    public Phase4BVerificationRunner(IncidentManagementService incidentManagementService,
                                   IncidentLifecycleService incidentLifecycleService,
                                   ResolutionRecommendationService resolutionRecommendationService,
                                   EmployeeRepository employeeRepository,
                                   ItServiceRepository itServiceRepository,
                                   IncidentRepository incidentRepository) {
        this.incidentManagementService = incidentManagementService;
        this.incidentLifecycleService = incidentLifecycleService;
        this.resolutionRecommendationService = resolutionRecommendationService;
        this.employeeRepository = employeeRepository;
        this.itServiceRepository = itServiceRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("\n=========================================================================================");
        System.out.println("                 PHASE 4B: INTELLIGENT FEATURES A & G VERIFICATION");
        System.out.println("=========================================================================================");

        Employee reporter = employeeRepository.findByUserIdCode("E101")
                .orElseThrow(() -> new IllegalStateException("Reporter E101 missing from seed data"));
        ItService paymentService = itServiceRepository.findByServiceCode("SVC-PAY")
                .orElseThrow(() -> new IllegalStateException("Payment Application missing from seed data"));

        // =====================================================================================
        // PART 1: FEATURE A - DUPLICATE INCIDENT DETECTION, CO-ASSIGNMENT & CASCADE CLOSURE
        // =====================================================================================
        System.out.println("\n--- [FEATURE A] Duplicate Incident Detection & Cluster Management ---");

        // 1. Report Master Incident 1 on Payment Application and auto-assign
        Incident inc1 = incidentManagementService.reportIncident(
                "Payment gateway timeout and checkout transaction error",
                "Customer transaction failures during checkout with gateway timeout 504.",
                reporter.getId(),
                paymentService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );
        inc1 = incidentManagementService.autoAssignIncident(inc1.getIncidentCode(), reporter);

        System.out.printf("Master Incident Reported: %s | Status: %s | Assigned Tech: %s (%s)%n",
                inc1.getIncidentCode(),
                inc1.getStatus(),
                inc1.getAssignedTechnician().getName(),
                inc1.getAssignedTechnician().getUserIdCode());

        // 2. Report Duplicate Incident 2 on Payment Application with overlapping keywords
        Incident inc2 = incidentManagementService.reportAndAutoAssignIncident(
                "Checkout payment gateway timeout error for customers",
                "Users experiencing payment gateway timeout during checkout flow.",
                reporter.getId(),
                paymentService.getId(),
                null,
                ImpactLevel.HIGH,
                UrgencyLevel.HIGH
        );

        System.out.printf("Incoming Incident 2 Reported: %s%n", inc2.getIncidentCode());
        System.out.printf(" - Duplicate Detected Of  : %s%n", inc2.getDuplicateOfIncidentCode());
        System.out.printf(" - Lifecycle Status       : %s%n", inc2.getStatus());
        System.out.printf(" - Co-Assigned Technician : %s (%s)%n",
                inc2.getAssignedTechnician() != null ? inc2.getAssignedTechnician().getName() : "None",
                inc2.getAssignedTechnician() != null ? inc2.getAssignedTechnician().getUserIdCode() : "None");

        String expectedClusterMaster = inc1.getDuplicateOfIncidentCode() != null
                ? inc1.getDuplicateOfIncidentCode()
                : inc1.getIncidentCode();
        boolean duplicateMatched = expectedClusterMaster.equals(inc2.getDuplicateOfIncidentCode());
        boolean sameTechnician = inc1.getAssignedTechnician() != null &&
                inc2.getAssignedTechnician() != null &&
                inc1.getAssignedTechnician().getUserIdCode().equals(inc2.getAssignedTechnician().getUserIdCode());
        boolean movedToInProgress = inc2.getStatus() == IncidentStatus.IN_PROGRESS;

        if (duplicateMatched && sameTechnician && movedToInProgress) {
            System.out.println(">> [SUCCESS] Feature A Co-Assignment verified: Duplicate linked and co-assigned to existing technician.");
        } else {
            System.err.println(">> [FAILURE] Duplicate detection or co-assignment verification failed!");
        }

        // 3. Resolve Master Incident 1 -> Verifying Cascading Cluster Closure
        System.out.println("\nResolving Incident 1 to test Cascading Cluster Closure...");
        incidentLifecycleService.resolveAndCloseIncident(
                inc1.getIncidentCode(),
                inc1.getAssignedTechnician(),
                "Recycled payment gateway connection pool and scaled up microservice pods."
        );

        Incident reloadedInc1 = incidentRepository.findByIncidentCode(inc1.getIncidentCode()).orElseThrow();
        Incident reloadedInc2 = incidentRepository.findByIncidentCode(inc2.getIncidentCode()).orElseThrow();

        System.out.printf("Reloaded Incident 1: %s | Status: %s | Resolution: %s%n",
                reloadedInc1.getIncidentCode(), reloadedInc1.getStatus(), reloadedInc1.getResolutionDetails());
        System.out.printf("Reloaded Incident 2: %s | Status: %s | Resolution: %s%n",
                reloadedInc2.getIncidentCode(), reloadedInc2.getStatus(), reloadedInc2.getResolutionDetails());

        boolean bothClosed = reloadedInc1.getStatus() == IncidentStatus.CLOSED && reloadedInc2.getStatus() == IncidentStatus.CLOSED;
        if (bothClosed) {
            System.out.println(">> [SUCCESS] Feature A Cascading Closure verified: Both linked duplicate incidents closed at once!");
        } else {
            System.err.println(">> [FAILURE] Cascading closure failed!");
        }

        // =====================================================================================
        // PART 2: FEATURE G - SUGGESTED HISTORICAL RESOLUTIONS
        // =====================================================================================
        System.out.println("\n--- [FEATURE G] Suggested Historical Resolutions ---");

        // Create an active incident needing diagnosis
        Incident activeIncident = incidentManagementService.reportIncident(
                "Payment gateway connection reset during checkout",
                "Payment gateway dropping connections under high load",
                reporter.getId(),
                paymentService.getId(),
                null,
                ImpactLevel.MEDIUM,
                UrgencyLevel.HIGH
        );
        activeIncident = incidentManagementService.autoAssignIncident(activeIncident.getIncidentCode(), reporter);

        System.out.printf("Evaluating recommendations for active incident: %s ('%s')%n",
                activeIncident.getIncidentCode(), activeIncident.getTitle());

        List<SuggestedResolutionDTO> recommendations = resolutionRecommendationService.getAllAvailableHistoricalSolutions(activeIncident);

        System.out.printf("Total Historical Solutions Available: %d%n", recommendations.size());
        System.out.println("----------------------------------------------------------------------------------------------------------------");
        System.out.printf("%-10s | %-8s | %-20s | %-14s | %-8s | %s%n",
                "Ticket", "Score", "Service", "Category", "Tech", "Resolution Snippet");
        System.out.println("----------------------------------------------------------------------------------------------------------------");

        for (SuggestedResolutionDTO dto : recommendations) {
            String snippet = dto.getResolutionDetails() != null
                    ? (dto.getResolutionDetails().length() > 45 ? dto.getResolutionDetails().substring(0, 42) + "..." : dto.getResolutionDetails())
                    : "N/A";
            System.out.printf("%-10s | %-7.1f%% | %-20s | %-14s | %-8s | %s%n",
                    dto.getHistoricalIncidentCode(),
                    dto.getSimilarityScore(),
                    dto.getServiceName(),
                    dto.getCategoryName(),
                    dto.getResolvedByTechnicianCode(),
                    snippet);
        }
        System.out.println("----------------------------------------------------------------------------------------------------------------");

        // Verify ordering: descending similarity score
        boolean ordered = true;
        for (int i = 0; i < recommendations.size() - 1; i++) {
            if (recommendations.get(i).getSimilarityScore() < recommendations.get(i + 1).getSimilarityScore()) {
                ordered = false;
                break;
            }
        }

        if (!recommendations.isEmpty() && ordered) {
            System.out.println(">> [SUCCESS] Feature G verified: All historical solutions returned and ranked in descending order of relevance!");

            // Test applying top historical resolution to close the active diagnosis incident
            SuggestedResolutionDTO topSolution = recommendations.get(0);
            Incident closedDiagnosis = incidentLifecycleService.applyHistoricalResolutionAndClose(
                    activeIncident.getIncidentCode(),
                    topSolution.getHistoricalIncidentCode(),
                    reporter
            );
            System.out.printf("Applied top historical resolution from %s: Incident %s status is now %s.%n",
                    topSolution.getHistoricalIncidentCode(), closedDiagnosis.getIncidentCode(), closedDiagnosis.getStatus());
        } else {
            System.err.println(">> [FAILURE] Historical solutions recommendation ordering or retrieval failed!");
        }

        if (duplicateMatched && sameTechnician && bothClosed && !recommendations.isEmpty() && ordered) {
            System.out.println("\n[PHASE 4B TEST PASSED] Feature A (Duplicate Cluster Management) and Feature G (All Historical Suggested Resolutions) verified.");
            log.info("[PHASE 4B TEST PASSED] Feature A (Duplicate Cluster Management) and Feature G (All Historical Suggested Resolutions) verified.");
        }

        System.out.println("=========================================================================================\n");
    }
}
