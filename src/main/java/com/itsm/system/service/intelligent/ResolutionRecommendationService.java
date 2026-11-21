package com.itsm.system.service.intelligent;

import com.itsm.system.dto.SuggestedResolutionDTO;
import com.itsm.system.exception.InvalidIncidentException;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Intelligent Historical Resolution Recommendation Service (Feature G).
 * Evaluates all closed incidents with documented resolutions, calculates multi-factor relevance scores
 * (service match +40%, category match +30%, keyword Jaccard overlap +30%), and returns ALL historical solutions
 * ranked by relevance for the technician to inspect and choose from.
 */
@Service
@Transactional(readOnly = true)
public class ResolutionRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(ResolutionRecommendationService.class);

    private final IncidentRepository incidentRepository;
    private final DuplicateDetectionService duplicateDetectionService;

    public ResolutionRecommendationService(IncidentRepository incidentRepository,
                                         DuplicateDetectionService duplicateDetectionService) {
        this.incidentRepository = incidentRepository;
        this.duplicateDetectionService = duplicateDetectionService;
    }

    /**
     * Retrieves all available closed historical resolutions ranked by relevance for the target incident.
     * Never truncates or filters results—returns the entire historical solution repository.
     *
     * @param targetIncident the active incident being diagnosed
     * @return List of SuggestedResolutionDTO sorted in descending order of similarity score
     */
    public List<SuggestedResolutionDTO> getAllAvailableHistoricalSolutions(Incident targetIncident) {
        if (targetIncident == null) {
            throw new InvalidIncidentException("Target incident cannot be null when requesting historical recommendations.");
        }

        List<Incident> closedIncidents = incidentRepository.findByStatusAndResolutionDetailsIsNotNull(IncidentStatus.CLOSED);
        log.info("Evaluating {} closed incident(s) for suggested resolutions on ticket '{}'",
                closedIncidents.size(), targetIncident.getIncidentCode());

        String targetText = (targetIncident.getTitle() != null ? targetIncident.getTitle() : "") + " " +
                (targetIncident.getDescription() != null ? targetIncident.getDescription() : "");
        Set<String> targetKeywords = duplicateDetectionService.extractKeywords(targetText);

        List<SuggestedResolutionDTO> recommendations = new ArrayList<>();

        for (Incident historical : closedIncidents) {
            // Exclude the target incident itself if it happens to be closed
            if (targetIncident.getId() != null && Objects.equals(targetIncident.getId(), historical.getId())) {
                continue;
            }
            if (targetIncident.getIncidentCode() != null && targetIncident.getIncidentCode().equals(historical.getIncidentCode())) {
                continue;
            }

            double relevanceScore = calculateRelevanceScore(targetIncident, targetKeywords, historical);
            double roundedScore = Math.round(relevanceScore * 10.0) / 10.0;

            String serviceName = historical.getAffectedService() != null
                    ? historical.getAffectedService().getServiceName()
                    : "N/A";
            String categoryName = historical.getCategory() != null
                    ? historical.getCategory().getName()
                    : "N/A";
            String techCode = historical.getAssignedTechnician() != null
                    ? historical.getAssignedTechnician().getUserIdCode()
                    : "SYSTEM";

            SuggestedResolutionDTO dto = new SuggestedResolutionDTO(
                    historical.getIncidentCode(),
                    historical.getTitle(),
                    serviceName,
                    categoryName,
                    roundedScore,
                    historical.getResolutionDetails(),
                    techCode,
                    historical.getClosedAt() != null ? historical.getClosedAt() : historical.getResolvedAt()
            );

            recommendations.add(dto);
        }

        // Sort descending by similarityScore (highest relevance first)
        recommendations.sort(Comparator.comparingDouble(SuggestedResolutionDTO::getSimilarityScore).reversed());

        log.info("Generated {} historical solution recommendations for incident '{}'",
                recommendations.size(), targetIncident.getIncidentCode());

        return recommendations;
    }

    /**
     * Calculates the multi-factor relevance percentage (0.0 to 100.0):
     * - Same IT Service: +40.0 points
     * - Same Incident Category: +30.0 points
     * - Text Jaccard Keyword Overlap: up to +30.0 points
     */
    private double calculateRelevanceScore(Incident target, Set<String> targetKeywords, Incident historical) {
        double score = 0.0;

        // 1. IT Service Match (+40 points)
        if (target.getAffectedService() != null && historical.getAffectedService() != null) {
            if (Objects.equals(target.getAffectedService().getId(), historical.getAffectedService().getId()) ||
                    Objects.equals(target.getAffectedService().getServiceCode(), historical.getAffectedService().getServiceCode())) {
                score += 40.0;
            }
        }

        // 2. Incident Category Match (+30 points)
        if (target.getCategory() != null && historical.getCategory() != null) {
            if (Objects.equals(target.getCategory().getId(), historical.getCategory().getId()) ||
                    Objects.equals(target.getCategory().getName(), historical.getCategory().getName())) {
                score += 30.0;
            }
        }

        // 3. Keyword Jaccard Overlap (up to +30 points)
        String historicalText = (historical.getTitle() != null ? historical.getTitle() : "") + " " +
                (historical.getDescription() != null ? historical.getDescription() : "");
        Set<String> historicalKeywords = duplicateDetectionService.extractKeywords(historicalText);

        double jaccard = duplicateDetectionService.calculateJaccardSimilarity(targetKeywords, historicalKeywords);
        score += (jaccard * 30.0);

        return Math.min(score, 100.0);
    }
}
