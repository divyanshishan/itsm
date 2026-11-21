package com.itsm.system.controller;

import com.itsm.system.dto.ApiResponse;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.model.enums.PriorityLevel;
import com.itsm.system.model.enums.SlaStatus;
import com.itsm.system.repository.IncidentRepository;
import com.itsm.system.repository.ItServiceRepository;
import com.itsm.system.service.intelligent.DuplicateDetectionService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Public REST Controller serving unauthenticated Live Public Portal telemetry,
 * incident health summaries, quick ticket lookup, and self-service knowledge base.
 */
@RestController
@RequestMapping("/api/public")
@Transactional(readOnly = true)
public class PublicPortalRestController {

    private final IncidentRepository incidentRepository;
    private final ItServiceRepository itServiceRepository;
    private final DuplicateDetectionService duplicateDetectionService;

    public PublicPortalRestController(IncidentRepository incidentRepository,
                                    ItServiceRepository itServiceRepository,
                                    DuplicateDetectionService duplicateDetectionService) {
        this.incidentRepository = incidentRepository;
        this.itServiceRepository = itServiceRepository;
        this.duplicateDetectionService = duplicateDetectionService;
    }

    /**
     * Retrieves live public dashboard telemetry:
     * - Top IT services health status (Operational / Warning / Degraded)
     * - Real active Duplicate Incident cluster details (Feature A)
     * - SLA compliance metrics and non-overlapping tracking codes (Feature 4C)
     */
    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> getPublicSummary() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. Services Health Summary (Top 4 services)
        List<ItService> allServices = itServiceRepository.findAll();
        List<Incident> allIncidents = incidentRepository.findAll();
        List<Incident> activeIncidents = allIncidents.stream()
                .filter(i -> i.getStatus() != IncidentStatus.CLOSED)
                .toList();

        List<Map<String, Object>> serviceSummaries = new ArrayList<>();
        for (ItService svc : allServices) {
            List<Incident> serviceIncidents = allIncidents.stream()
                    .filter(i -> i.getAffectedService() != null &&
                            (Objects.equals(i.getAffectedService().getId(), svc.getId()) ||
                             Objects.equals(i.getAffectedService().getServiceCode(), svc.getServiceCode())))
                    .toList();

            List<Incident> svcActive = serviceIncidents.stream()
                    .filter(i -> i.getStatus() != IncidentStatus.CLOSED)
                    .toList();

            long activeCount = svcActive.size();
            long resolvedCount = serviceIncidents.stream()
                    .filter(i -> i.getStatus() == IncidentStatus.CLOSED)
                    .count();

            long breachedCount = svcActive.stream()
                    .filter(i -> i.isSlaViolated() || i.getSlaStatus() == SlaStatus.RESPONSE_VIOLATED || i.getSlaStatus() == SlaStatus.RESOLUTION_VIOLATED)
                    .count();
            long atRiskCount = svcActive.stream()
                    .filter(i -> !i.isSlaViolated() && i.getSlaStatus() == SlaStatus.AT_RISK)
                    .count();
            boolean hasCritical = svcActive.stream()
                    .anyMatch(i -> i.getPriority() == PriorityLevel.P1_CRITICAL);
            boolean hasHigh = svcActive.stream()
                    .anyMatch(i -> i.getPriority() == PriorityLevel.P2_HIGH);

            String status;
            if (breachedCount > 0 || hasCritical) {
                status = "Degraded";
            } else if (atRiskCount > 0 || hasHigh) {
                status = "Warning";
            } else {
                status = "Operational";
            }

            Map<String, Object> sMap = new LinkedHashMap<>();
            sMap.put("serviceCode", svc.getServiceCode());
            sMap.put("name", svc.getServiceName());
            sMap.put("activeCount", activeCount);
            sMap.put("resolvedCount", resolvedCount);
            sMap.put("breachedCount", breachedCount);
            sMap.put("atRiskCount", atRiskCount);
            sMap.put("status", status);
            serviceSummaries.add(sMap);
        }

        // Order consistently by total incident volume descending, then by serviceCode ascending
        serviceSummaries.sort((a, b) -> {
            long totalA = ((Long) a.get("activeCount")) + ((Long) a.get("resolvedCount"));
            long totalB = ((Long) b.get("activeCount")) + ((Long) b.get("resolvedCount"));
            int cmp = Long.compare(totalB, totalA);
            if (cmp != 0) return cmp;
            return ((String) a.get("serviceCode")).compareTo((String) b.get("serviceCode"));
        });

        if (serviceSummaries.size() > 4) {
            serviceSummaries = serviceSummaries.subList(0, 4);
        }
        data.put("services", serviceSummaries);

        // 2. Real Feature A Duplicate Cluster Info
        List<Incident> allWithDup = incidentRepository.findAll().stream()
                .filter(i -> i.getDuplicateOfIncidentCode() != null && !i.getDuplicateOfIncidentCode().trim().isEmpty())
                .toList();

        List<Incident> activeDups = allWithDup.stream()
                .filter(i -> i.getStatus() != IncidentStatus.CLOSED)
                .toList();

        List<Incident> dupPool = activeDups.isEmpty() ? allWithDup : activeDups;
        Map<String, Object> clusterMap = new LinkedHashMap<>();

        if (!dupPool.isEmpty()) {
            String masterCode = dupPool.get(0).getDuplicateOfIncidentCode();
            List<String> dupCodes = dupPool.stream()
                    .filter(i -> masterCode.equals(i.getDuplicateOfIncidentCode()))
                    .map(Incident::getIncidentCode)
                    .distinct()
                    .toList();

            Optional<Incident> masterOpt = incidentRepository.findByIncidentCode(masterCode);
            String masterTitle = masterOpt.map(Incident::getTitle).orElse("Master Clustered Incident");
            String serviceCode = masterOpt.map(m -> m.getAffectedService() != null ? m.getAffectedService().getServiceCode() : "N/A").orElse("N/A");
            String techName = masterOpt.map(m -> m.getAssignedTechnician() != null ? m.getAssignedTechnician().getName() : null)
                    .orElseGet(() -> dupPool.stream()
                            .map(Incident::getAssignedTechnician)
                            .filter(Objects::nonNull)
                            .map(t -> t.getName())
                            .findFirst()
                            .orElse("Unassigned"));

            clusterMap.put("totalLinkedDuplicates", dupCodes.size());
            clusterMap.put("masterCode", masterCode);
            clusterMap.put("masterTitle", masterTitle);
            clusterMap.put("serviceCode", serviceCode);
            clusterMap.put("duplicateCodes", dupCodes);
            clusterMap.put("assignedTechnicianName", techName);
        } else {
            clusterMap.put("totalLinkedDuplicates", 0);
            clusterMap.put("masterCode", null);
            clusterMap.put("masterTitle", null);
            clusterMap.put("serviceCode", null);
            clusterMap.put("duplicateCodes", Collections.emptyList());
            clusterMap.put("assignedTechnicianName", null);
        }
        data.put("duplicateCluster", clusterMap);

        // 3. Real Feature 4C SLA Metrics with strict mathematical consistency
        int totalTracked = activeIncidents.size();
        List<Incident> breachedList = activeIncidents.stream()
                .filter(i -> i.isSlaViolated() || i.getSlaStatus() == SlaStatus.RESPONSE_VIOLATED || i.getSlaStatus() == SlaStatus.RESOLUTION_VIOLATED)
                .toList();
        List<Incident> atRiskList = activeIncidents.stream()
                .filter(i -> !breachedList.contains(i) && i.getSlaStatus() == SlaStatus.AT_RISK)
                .toList();
        List<Incident> onTrackList = activeIncidents.stream()
                .filter(i -> !breachedList.contains(i) && !atRiskList.contains(i) && i.getSlaStatus() == SlaStatus.ON_TRACK)
                .toList();

        int breachedCount = breachedList.size();
        int atRiskCount = atRiskList.size();
        int onTrackCount = onTrackList.size();

        int denominator = Math.max(1, totalTracked);
        int compliancePercent = (int) Math.round(((double) (totalTracked - breachedCount) / denominator) * 100);
        compliancePercent = Math.max(0, Math.min(100, compliancePercent));
        int breachPercent = 100 - compliancePercent;

        List<String> onTrackCodes = onTrackList.stream()
                .map(Incident::getIncidentCode)
                .limit(2)
                .toList();
        List<String> breachedCodes = breachedList.stream()
                .map(Incident::getIncidentCode)
                .limit(2)
                .toList();

        Map<String, Object> slaMap = new LinkedHashMap<>();
        slaMap.put("totalTracked", totalTracked);
        slaMap.put("onTrackCount", onTrackCount);
        slaMap.put("atRiskCount", atRiskCount);
        slaMap.put("breachedCount", breachedCount);
        slaMap.put("compliancePercent", compliancePercent);
        slaMap.put("breachPercent", breachPercent);
        slaMap.put("onTrackCodes", onTrackCodes);
        slaMap.put("breachedCodes", breachedCodes);
        data.put("slaMetrics", slaMap);

        // 4. Incident Volume & Resolution Trend (6 chronological 2-hour buckets ending at now)
        LocalDateTime now = LocalDateTime.now();
        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        List<Map<String, Object>> trendBuckets = new ArrayList<>();

        long totalReported = allIncidents.size();
        long totalResolved = allIncidents.stream()
                .filter(i -> i.getStatus() == IncidentStatus.CLOSED)
                .count();
        long resolutionRate = Math.round(((double) totalResolved / Math.max(1, totalReported)) * 100);

        for (int k = 0; k < 6; k++) {
            LocalDateTime windowStart = now.minusHours(12 - k * 2);
            LocalDateTime windowEnd = (k == 5) ? now.plusMinutes(1) : now.minusHours(12 - (k + 1) * 2);

            final int bucketIdx = k;
            long reportedInBucket = allIncidents.stream().filter(inc -> {
                LocalDateTime cAt = inc.getCreatedAt() != null ? inc.getCreatedAt() : now;
                if (bucketIdx == 0) {
                    return cAt.isBefore(windowEnd);
                } else if (bucketIdx == 5) {
                    return !cAt.isBefore(windowStart) && !cAt.isAfter(now);
                } else {
                    return !cAt.isBefore(windowStart) && cAt.isBefore(windowEnd);
                }
            }).count();

            long resolvedInBucket = allIncidents.stream().filter(inc -> {
                if (inc.getStatus() != IncidentStatus.CLOSED) return false;
                LocalDateTime rAt = inc.getClosedAt() != null ? inc.getClosedAt() :
                        (inc.getResolvedAt() != null ? inc.getResolvedAt() : inc.getCreatedAt());
                if (rAt == null) rAt = now;

                if (bucketIdx == 0) {
                    return rAt.isBefore(windowEnd);
                } else if (bucketIdx == 5) {
                    return !rAt.isBefore(windowStart) && !rAt.isAfter(now);
                } else {
                    return !rAt.isBefore(windowStart) && rAt.isBefore(windowEnd);
                }
            }).count();

            Map<String, Object> bucket = new LinkedHashMap<>();
            bucket.put("label", windowStart.format(timeFmt));
            bucket.put("reportedCount", reportedInBucket);
            bucket.put("resolvedCount", resolvedInBucket);
            trendBuckets.add(bucket);
        }

        Map<String, Object> trendMap = new LinkedHashMap<>();
        trendMap.put("buckets", trendBuckets);
        trendMap.put("totalReported", totalReported);
        trendMap.put("totalResolved", totalResolved);
        trendMap.put("resolutionRate", resolutionRate);
        data.put("incidentTrend", trendMap);

        return ApiResponse.ok("Public portal summary retrieved successfully", data);
    }

    /**
     * Unified search supporting:
     * 1. Exact Incident Code lookup (TICKET status card)
     * 2. Natural language / issue symptom search matching Knowledge Base solutions and active tickets
     */
    @GetMapping("/search")
    public ApiResponse<Map<String, Object>> searchPublic(@RequestParam("q") String query) {
        if (query == null || query.trim().isEmpty()) {
            return ApiResponse.ok("Search results", Map.of("type", "EMPTY", "matchedSolutions", List.of(), "activeMatches", List.of()));
        }

        String q = query.trim();
        Map<String, Object> responseData = new LinkedHashMap<>();

        // Check if query is an Incident Code
        Optional<Incident> directTicketOpt = incidentRepository.findByIncidentCode(q.toUpperCase());
        if (directTicketOpt.isPresent()) {
            Incident inc = directTicketOpt.get();
            Map<String, Object> ticketMap = new LinkedHashMap<>();
            ticketMap.put("incidentCode", inc.getIncidentCode());
            ticketMap.put("title", inc.getTitle());
            ticketMap.put("description", inc.getDescription());
            ticketMap.put("status", inc.getStatus() != null ? inc.getStatus().name() : "OPEN");
            ticketMap.put("priority", inc.getPriority() != null ? inc.getPriority().name() : "P3_MEDIUM");
            ticketMap.put("slaStatus", inc.getSlaStatus() != null ? inc.getSlaStatus().name() : "ON_TRACK");
            ticketMap.put("serviceCode", inc.getAffectedService() != null ? inc.getAffectedService().getServiceCode() : "N/A");
            ticketMap.put("serviceName", inc.getAffectedService() != null ? inc.getAffectedService().getServiceName() : "N/A");
            ticketMap.put("assignedTechnicianName", inc.getAssignedTechnician() != null ? inc.getAssignedTechnician().getName() : "Unassigned");
            ticketMap.put("duplicateOfIncidentCode", inc.getDuplicateOfIncidentCode());
            ticketMap.put("resolutionDetails", inc.getResolutionDetails());
            ticketMap.put("createdAt", inc.getCreatedAt() != null ? inc.getCreatedAt().toString() : null);

            responseData.put("type", "TICKET");
            responseData.put("ticket", ticketMap);
            return ApiResponse.ok("Incident ticket found", responseData);
        }

        // Natural Language Knowledge Base and Active Incident Search
        Set<String> queryKeywords = duplicateDetectionService.extractKeywords(q);
        String lowerQ = q.toLowerCase();

        // 1. Match against CLOSED historical solutions
        List<Incident> closedIncidents = incidentRepository.findByStatusAndResolutionDetailsIsNotNull(IncidentStatus.CLOSED);
        List<Map<String, Object>> matchedSolutions = new ArrayList<>();

        for (Incident closed : closedIncidents) {
            String text = (closed.getTitle() != null ? closed.getTitle() : "") + " " +
                    (closed.getDescription() != null ? closed.getDescription() : "") + " " +
                    (closed.getResolutionDetails() != null ? closed.getResolutionDetails() : "");
            Set<String> closedKeywords = duplicateDetectionService.extractKeywords(text);
            double similarity = duplicateDetectionService.calculateJaccardSimilarity(queryKeywords, closedKeywords);
            boolean textMatch = text.toLowerCase().contains(lowerQ);

            if (similarity > 0.05 || textMatch || queryKeywords.stream().anyMatch(closedKeywords::contains)) {
                double score = Math.max(similarity * 100.0, textMatch ? 80.0 : 35.0);
                Map<String, Object> sol = new LinkedHashMap<>();
                sol.put("incidentCode", closed.getIncidentCode());
                sol.put("title", closed.getTitle());
                sol.put("serviceCode", closed.getAffectedService() != null ? closed.getAffectedService().getServiceCode() : "N/A");
                sol.put("categoryName", closed.getCategory() != null ? closed.getCategory().getName() : "General");
                sol.put("resolutionDetails", closed.getResolutionDetails());
                sol.put("similarityScore", Math.round(score * 10.0) / 10.0);
                matchedSolutions.add(sol);
            }
        }
        matchedSolutions.sort((a, b) -> Double.compare((Double) b.get("similarityScore"), (Double) a.get("similarityScore")));
        if (matchedSolutions.size() > 3) {
            matchedSolutions = matchedSolutions.subList(0, 3);
        }

        // 2. Match against active incidents
        List<Incident> activeIncidents = incidentRepository.findByStatusNot(IncidentStatus.CLOSED);
        List<Map<String, Object>> activeMatches = new ArrayList<>();
        for (Incident act : activeIncidents) {
            String text = (act.getTitle() != null ? act.getTitle() : "") + " " + (act.getDescription() != null ? act.getDescription() : "");
            Set<String> actKeywords = duplicateDetectionService.extractKeywords(text);
            if (text.toLowerCase().contains(lowerQ) || queryKeywords.stream().anyMatch(actKeywords::contains)) {
                Map<String, Object> aMap = new LinkedHashMap<>();
                aMap.put("incidentCode", act.getIncidentCode());
                aMap.put("title", act.getTitle());
                aMap.put("status", act.getStatus() != null ? act.getStatus().name() : "OPEN");
                aMap.put("priority", act.getPriority() != null ? act.getPriority().name() : "P3_MEDIUM");
                aMap.put("serviceCode", act.getAffectedService() != null ? act.getAffectedService().getServiceCode() : "N/A");
                activeMatches.add(aMap);
                if (activeMatches.size() >= 3) break;
            }
        }

        responseData.put("type", "KNOWLEDGE");
        responseData.put("matchedSolutions", matchedSolutions);
        responseData.put("activeMatches", activeMatches);
        return ApiResponse.ok("Knowledge base and active matches retrieved", responseData);
    }

    /**
     * Retrieves all closed incidents with verified resolution details for the public knowledge base modal.
     */
    @GetMapping("/knowledge-base")
    public ApiResponse<List<Map<String, Object>>> getKnowledgeBase() {
        List<Incident> closed = incidentRepository.findByStatusAndResolutionDetailsIsNotNull(IncidentStatus.CLOSED);
        List<Map<String, Object>> list = closed.stream()
                .filter(i -> i.getResolutionDetails() != null && !i.getResolutionDetails().trim().isEmpty())
                .sorted((a, b) -> {
                    if (a.getClosedAt() == null || b.getClosedAt() == null) return 0;
                    return b.getClosedAt().compareTo(a.getClosedAt());
                })
                .map(inc -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("incidentCode", inc.getIncidentCode());
                    m.put("title", inc.getTitle());
                    m.put("serviceCode", inc.getAffectedService() != null ? inc.getAffectedService().getServiceCode() : "N/A");
                    m.put("serviceName", inc.getAffectedService() != null ? inc.getAffectedService().getServiceName() : "N/A");
                    m.put("categoryName", inc.getCategory() != null ? inc.getCategory().getName() : "General");
                    m.put("resolutionDetails", inc.getResolutionDetails());
                    m.put("feedbackRating", inc.getFeedbackRating() != null ? inc.getFeedbackRating() : 5);
                    m.put("closedAt", inc.getClosedAt() != null ? inc.getClosedAt().toString() : null);
                    return m;
                })
                .toList();

        return ApiResponse.ok("Knowledge base solutions retrieved", list);
    }
}
