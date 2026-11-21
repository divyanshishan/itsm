package com.itsm.system.service.intelligent;

import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.enums.IncidentStatus;
import com.itsm.system.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Intelligent Duplicate Incident Detection Service (Feature A).
 * Scans active incidents on the same IT service, tokenizes titles/descriptions,
 * removes English stop words, and evaluates keyword Jaccard similarity to identify duplicates.
 */
@Service
@Transactional(readOnly = true)
public class DuplicateDetectionService {

    private static final Logger log = LoggerFactory.getLogger(DuplicateDetectionService.class);

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "is", "a", "an", "and", "in", "to", "for", "of", "with",
            "at", "by", "from", "on", "it", "my", "we", "are", "unable"
    );

    private static final double SIMILARITY_THRESHOLD = 0.30;
    private static final int SHARED_WORDS_THRESHOLD = 2;

    private final IncidentRepository incidentRepository;

    public DuplicateDetectionService(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    /**
     * Searches for an active duplicate incident occurring on the specified service.
     *
     * @param title       incident title
     * @param description incident description
     * @param serviceId   ID of the affected IT service
     * @return Optional containing the best matching active Incident, or empty if no duplicate detected
     */
    public Optional<Incident> findActiveDuplicate(String title, String description, Long serviceId) {
        return findActiveDuplicate(title, description, serviceId, null);
    }

    /**
     * Searches for an active duplicate incident occurring on the specified service,
     * optionally excluding a specific incident code (e.g. self-exclusion for already persisted incidents).
     *
     * @param title               incident title
     * @param description         incident description
     * @param serviceId           ID of the affected IT service
     * @param excludeIncidentCode incident code to exclude from candidates, or null
     * @return Optional containing the best matching active Incident, or empty if no duplicate detected
     */
    public Optional<Incident> findActiveDuplicate(String title, String description, Long serviceId, String excludeIncidentCode) {
        if (serviceId == null) {
            return Optional.empty();
        }

        String combinedText = (title != null ? title : "") + " " + (description != null ? description : "");
        Set<String> queryKeywords = extractKeywords(combinedText);
        if (queryKeywords.isEmpty()) {
            return Optional.empty();
        }

        List<Incident> activeIncidents = incidentRepository.findByAffectedServiceIdAndStatusNot(serviceId, IncidentStatus.CLOSED);
        if (activeIncidents.isEmpty()) {
            return Optional.empty();
        }

        Incident bestMatch = null;
        double highestSimilarity = 0.0;

        for (Incident candidate : activeIncidents) {
            if (excludeIncidentCode != null && excludeIncidentCode.equals(candidate.getIncidentCode())) {
                continue;
            }

            String candidateText = (candidate.getTitle() != null ? candidate.getTitle() : "") + " " +
                    (candidate.getDescription() != null ? candidate.getDescription() : "");
            Set<String> candidateKeywords = extractKeywords(candidateText);

            Set<String> intersection = new HashSet<>(queryKeywords);
            intersection.retainAll(candidateKeywords);

            Set<String> union = new HashSet<>(queryKeywords);
            union.addAll(candidateKeywords);

            double similarity = union.isEmpty() ? 0.0 : ((double) intersection.size() / union.size());

            log.debug("Comparing against candidate {}: Jaccard={:.2f}, sharedWords={}",
                    candidate.getIncidentCode(), similarity, intersection);

            if (similarity >= SIMILARITY_THRESHOLD || intersection.size() >= SHARED_WORDS_THRESHOLD) {
                if (similarity > highestSimilarity || bestMatch == null) {
                    highestSimilarity = similarity;
                    bestMatch = candidate;
                }
            }
        }

        if (bestMatch != null) {
            log.info("Duplicate incident detected! Matched with active incident {} (Score: {:.2f})",
                    bestMatch.getIncidentCode(), highestSimilarity);
            return Optional.of(bestMatch);
        }

        return Optional.empty();
    }

    /**
     * Extracts normalized technical keywords from text, stripping punctuation and stop words.
     */
    public Set<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptySet();
        }
        String clean = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
        String[] tokens = clean.split("\\s+");
        Set<String> keywords = new HashSet<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.length() > 1 && !STOP_WORDS.contains(trimmed)) {
                keywords.add(trimmed);
            }
        }
        return keywords;
    }

    /**
     * Calculates Jaccard similarity coefficient between two keyword sets: |A ∩ B| / |A ∪ B|.
     */
    public double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1 == null || set2 == null || set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : ((double) intersection.size() / union.size());
    }
}
