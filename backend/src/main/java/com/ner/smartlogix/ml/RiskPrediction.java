package com.ner.smartlogix.ml;

import com.ner.smartlogix.enums.RiskLevel;
import java.util.List;

/**
 * What the engine decided, and why.
 *
 * <p>The {@code scoreCard} is not decoration. A government-facing system that says
 * "HIGH_RISK" without saying "because 142 mm of rain fell in 24 hours on a 27 degree
 * slope that has had three landslides this month" will not be trusted or acted on.
 *
 * @param disruptionScore 0 to 100
 * @param probability     the same number as 0 to 1, for charts and for comparison with a
 *                        future model that outputs a real probability
 */
public record RiskPrediction(
        RiskLevel riskLevel,
        double disruptionScore,
        double probability,
        String modelName,
        List<RuleContribution> scoreCard,
        String recommendation) {

    /** One rule's contribution to the final score. */
    public record RuleContribution(String rule, double subScore, double weight,
                                   double contribution, String reason) {
    }
}
