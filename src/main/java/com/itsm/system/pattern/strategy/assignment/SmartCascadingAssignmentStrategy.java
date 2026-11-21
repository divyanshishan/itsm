package com.itsm.system.pattern.strategy.assignment;

import com.itsm.system.exception.TechnicianUnavailableException;
import com.itsm.system.exception.TechnicianWorkloadExceededException;
import com.itsm.system.model.entity.Incident;
import com.itsm.system.model.entity.Technician;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Intelligent cascading assignment strategy:
 * Cascades decisions in order: Skill-Based -> Load-Based (with tolerance) -> Experience-Based.
 */
@Component
public class SmartCascadingAssignmentStrategy implements TechnicianAssignmentStrategy {

    private static final Logger log = LoggerFactory.getLogger(SmartCascadingAssignmentStrategy.class);
    private static final int LOAD_SIMILARITY_TOLERANCE = 1;

    @Override
    public Technician selectTechnician(Incident incident, List<Technician> candidateTechnicians) {
        if (candidateTechnicians == null || candidateTechnicians.isEmpty()) {
            throw new TechnicianUnavailableException("No candidate technicians available in the system.");
        }

        // Eligibility Check 1: Active and Available
        List<Technician> availableTechnicians = candidateTechnicians.stream()
                .filter(t -> t.isActive() && t.isAvailable())
                .collect(Collectors.toList());

        if (availableTechnicians.isEmpty()) {
            throw new TechnicianUnavailableException("No active and available technicians found.");
        }

        // Eligibility Check 2: Workload capacity
        List<Technician> eligibleTechnicians = availableTechnicians.stream()
                .filter(Technician::canAcceptIncident)
                .collect(Collectors.toList());

        if (eligibleTechnicians.isEmpty()) {
            throw new TechnicianWorkloadExceededException("All available technicians have exceeded their maximum workload capacity.");
        }

        // Step 1: Skill-Based Priority
        String categoryName = (incident != null && incident.getCategory() != null)
                ? incident.getCategory().getName()
                : null;

        List<Technician> step1Candidates;
        if (categoryName != null && !categoryName.trim().isEmpty()) {
            List<Technician> skilledTechnicians = eligibleTechnicians.stream()
                    .filter(t -> t.hasSkill(categoryName))
                    .collect(Collectors.toList());

            if (skilledTechnicians.size() == 1) {
                log.info("Step 1 (Skill Match): Single qualified technician found: {}", skilledTechnicians.get(0).getUserIdCode());
                return skilledTechnicians.get(0);
            } else if (skilledTechnicians.size() > 1) {
                log.info("Step 1 (Skill Match): Multiple skilled technicians found ({}), cascading to Step 2.", skilledTechnicians.size());
                step1Candidates = skilledTechnicians;
            } else {
                log.info("Step 1 (Skill Match): No technician possesses skill '{}', falling back to all eligible technicians.", categoryName);
                step1Candidates = eligibleTechnicians;
            }
        } else {
            step1Candidates = eligibleTechnicians;
        }

        // Step 2: Load-Based Priority (with similarity tolerance)
        int minLoad = step1Candidates.stream()
                .mapToInt(Technician::getCurrentWorkload)
                .min()
                .orElse(0);

        List<Technician> step2Candidates = step1Candidates.stream()
                .filter(t -> t.getCurrentWorkload() <= minLoad + LOAD_SIMILARITY_TOLERANCE)
                .collect(Collectors.toList());

        if (step2Candidates.size() == 1) {
            log.info("Step 2 (Load Priority): Single least-loaded technician selected: {}", step2Candidates.get(0).getUserIdCode());
            return step2Candidates.get(0);
        }

        log.info("Step 2 (Load Priority): Multiple candidates ({}) within load tolerance (min: {}, tol: {}), cascading to Step 3.",
                step2Candidates.size(), minLoad, LOAD_SIMILARITY_TOLERANCE);

        // Step 3: Experience-Based Priority (highest experience, then lowest load, then userIdCode)
        Technician selected = step2Candidates.stream()
                .min(Comparator
                        .comparingInt(Technician::getExperienceYears).reversed()
                        .thenComparingInt(Technician::getCurrentWorkload)
                        .thenComparing(Technician::getUserIdCode))
                .orElseThrow(() -> new TechnicianUnavailableException("Unable to resolve technician assignment through cascading strategy."));

        log.info("Step 3 (Experience Priority): Selected technician {} with {} years experience (load: {}).",
                selected.getUserIdCode(), selected.getExperienceYears(), selected.getCurrentWorkload());

        return selected;
    }
}
