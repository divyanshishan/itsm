package com.itsm.system.pattern.strategy.priority;

import com.itsm.system.exception.InvalidUserInputException;
import com.itsm.system.model.enums.BusinessCriticality;
import com.itsm.system.model.enums.ImpactLevel;
import com.itsm.system.model.enums.PriorityLevel;
import com.itsm.system.model.enums.UrgencyLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete implementation of PriorityCalculationStrategy using weighted scoring.
 * Formula: score = (impact * 0.40) + (urgency * 0.30) + (criticality * 0.30), rounded to 2 decimal places.
 */
@Component
public class WeightedPriorityCalculationStrategy implements PriorityCalculationStrategy {

    private static final double IMPACT_WEIGHT = 0.40;
    private static final double URGENCY_WEIGHT = 0.30;
    private static final double CRITICALITY_WEIGHT = 0.30;

    @Override
    public double calculatePriorityScore(ImpactLevel impact, UrgencyLevel urgency, BusinessCriticality criticality) {
        if (impact == null || urgency == null || criticality == null) {
            throw new InvalidUserInputException("Impact, urgency, and service criticality must all be non-null to compute priority score.");
        }

        double rawScore = (impact.getWeight() * IMPACT_WEIGHT)
                + (urgency.getWeight() * URGENCY_WEIGHT)
                + (criticality.getWeight() * CRITICALITY_WEIGHT);

        return BigDecimal.valueOf(rawScore)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public PriorityLevel determinePriorityLevel(double score) {
        if (score >= 3.4) {
            return PriorityLevel.P1_CRITICAL;
        } else if (score >= 2.6) {
            return PriorityLevel.P2_HIGH;
        } else if (score >= 1.8) {
            return PriorityLevel.P3_MEDIUM;
        } else {
            return PriorityLevel.P4_LOW;
        }
    }
}
