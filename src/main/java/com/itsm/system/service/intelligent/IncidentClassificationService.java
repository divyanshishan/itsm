package com.itsm.system.service.intelligent;

import com.itsm.system.model.entity.ItService;
import com.itsm.system.model.enums.BusinessCriticality;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.UrgencyLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Intelligent Incident Auto-Classification Service.
 * Automatically determines ImpactLevel and UrgencyLevel when not explicitly provided
 * by employees, preventing ticket priority manipulation.
 */
@Service
public class IncidentClassificationService {

    private static final Logger log = LoggerFactory.getLogger(IncidentClassificationService.class);

    private static final List<String> HIGH_SCOPE_KEYWORDS = List.of(
            "production", "outage", "all users", "entire", "customers",
            "company-wide", "cluster", "checkout", "breach", "data loss"
    );

    private static final List<String> LOCALIZED_SCOPE_KEYWORDS = List.of(
            "my laptop", "docking", "monitor", "flicker", "conference room",
            "csv", "typo", "cosmetic"
    );

    private static final List<String> CRITICAL_URGENCY_KEYWORDS = List.of(
            "outage", "down", "crash", "blocked", "blocking", "unable to",
            "failed", "lockout", "security", "502", "connection reset", "timeout"
    );

    private static final List<String> HIGH_URGENCY_KEYWORDS = List.of(
            "dropping", "disconnect", "error", "eviction", "deadlock",
            "failing", "urgent", "not working"
    );

    private static final List<String> MEDIUM_URGENCY_KEYWORDS = List.of(
            "slow", "latency", "intermittent", "degraded", "warning", "rebuild", "delay"
    );

    private static final List<String> LOW_URGENCY_KEYWORDS = List.of(
            "flicker", "encoding", "minor", "cosmetic", "request"
    );

    /**
     * Auto-classifies ImpactLevel based on Affected IT Service business criticality and scope keywords.
     *
     * @param title       incident title
     * @param description incident description
     * @param service     affected IT Service
     * @return determined ImpactLevel
     */
    public ImpactLevel classifyImpact(String title, String description, ItService service) {
        String normalized = normalize(title, description);

        // 1. Initial baseline from service business criticality
        ImpactLevel baseline = ImpactLevel.MEDIUM;
        boolean isTierOneOrHigh = false;

        if (service != null) {
            BusinessCriticality crit = service.getCriticality();
            String code = service.getServiceCode() != null ? service.getServiceCode().toUpperCase() : "";

            if (crit == BusinessCriticality.MISSION_CRITICAL || crit == BusinessCriticality.CRITICAL
                    || code.equals("SVC-PAY") || code.equals("SVC-AUTH")) {
                baseline = ImpactLevel.HIGH;
                isTierOneOrHigh = true;
            } else if (crit == BusinessCriticality.HIGH || code.equals("SVC-NET") || code.equals("SVC-CLOUD")) {
                baseline = ImpactLevel.HIGH;
                isTierOneOrHigh = true;
            } else if (crit == BusinessCriticality.MEDIUM) {
                baseline = ImpactLevel.MEDIUM;
            } else {
                baseline = ImpactLevel.LOW;
            }
        }

        // 2. Scan scope modifiers
        boolean hasHighScope = containsAny(normalized, HIGH_SCOPE_KEYWORDS);
        boolean hasLocalizedScope = containsAny(normalized, LOCALIZED_SCOPE_KEYWORDS);

        if (hasHighScope) {
            ImpactLevel result = isTierOneOrHigh ? ImpactLevel.CRITICAL : ImpactLevel.HIGH;
            log.info("Auto-classified ImpactLevel={} (High scope detected, tierOneOrHigh={})", result, isTierOneOrHigh);
            return result;
        }

        if (hasLocalizedScope) {
            ImpactLevel result = (baseline == ImpactLevel.LOW || normalized.contains("flicker") || normalized.contains("cosmetic")
                    || normalized.contains("typo") || normalized.contains("my laptop") || normalized.contains("monitor")
                    || normalized.contains("docking")) ? ImpactLevel.LOW : ImpactLevel.MEDIUM;
            log.info("Auto-classified ImpactLevel={} (Localized scope detected, baseline={})", result, baseline);
            return result;
        }

        log.info("Auto-classified ImpactLevel={} (Baseline from service criticality)", baseline);
        return baseline;
    }

    /**
     * Auto-classifies UrgencyLevel based on symptom severity keywords in title and description.
     *
     * @param title       incident title
     * @param description incident description
     * @return determined UrgencyLevel
     */
    public UrgencyLevel classifyUrgency(String title, String description) {
        String normalized = normalize(title, description);

        if (containsAny(normalized, CRITICAL_URGENCY_KEYWORDS)) {
            log.info("Auto-classified UrgencyLevel=CRITICAL (Critical symptom detected)");
            return UrgencyLevel.CRITICAL;
        }

        if (containsAny(normalized, HIGH_URGENCY_KEYWORDS)) {
            log.info("Auto-classified UrgencyLevel=HIGH (High symptom detected)");
            return UrgencyLevel.HIGH;
        }

        if (containsAny(normalized, MEDIUM_URGENCY_KEYWORDS)) {
            log.info("Auto-classified UrgencyLevel=MEDIUM (Medium symptom detected)");
            return UrgencyLevel.MEDIUM;
        }

        log.info("Auto-classified UrgencyLevel=LOW (Default fallback / low symptom)");
        return UrgencyLevel.LOW;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String title, String description) {
        String t = title != null ? title.toLowerCase() : "";
        String d = description != null ? description.toLowerCase() : "";
        return t + " " + d;
    }
}
