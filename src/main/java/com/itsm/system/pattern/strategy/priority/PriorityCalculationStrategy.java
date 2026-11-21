package com.itsm.system.pattern.strategy.priority;

import com.itsm.system.model.enums.BusinessCriticality;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.PriorityLevel;
import com.itsm.system.model.enums.UrgencyLevel;

/**
 * Strategy interface defining priority calculation algorithms for incoming IT incidents.
 */
public interface PriorityCalculationStrategy {

    /**
     * Calculates a numeric priority score based on impact, urgency, and service criticality.
     *
     * @param impact      the assessed business impact level
     * @param urgency     the assessed urgency level
     * @param criticality the business criticality of the affected IT service
     * @return the calculated numeric priority score (rounded to 2 decimal places)
     */
    double calculatePriorityScore(ImpactLevel impact, UrgencyLevel urgency, BusinessCriticality criticality);

    /**
     * Maps a numeric priority score to a discrete ITIL PriorityLevel.
     *
     * @param score the calculated numeric priority score
     * @return the corresponding PriorityLevel
     */
    PriorityLevel determinePriorityLevel(double score);
}
