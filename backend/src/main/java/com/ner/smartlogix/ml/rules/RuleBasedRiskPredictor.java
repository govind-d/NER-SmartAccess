package com.ner.smartlogix.ml.rules;

import com.ner.smartlogix.config.RiskProperties;
import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.ml.RiskPrediction;
import com.ner.smartlogix.ml.RiskPredictor;
import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The Phase 5 prediction engine: a transparent weighted scoring model.
 *
 * <pre>
 *   disruptionScore = sum over rules of (ruleScore x ruleWeight)
 *
 *   score &lt; 35   -> LOW_RISK
 *   35 to 65     -> MEDIUM_RISK
 *   above 65     -> HIGH_RISK
 * </pre>
 *
 * <p>Why start here rather than with machine learning: there is no labelled history of
 * NER road disruptions to train on, the domain knowledge (rainfall thresholds, slope,
 * monsoon timing) really is predictive, and every answer can be explained to an official
 * who has to act on it. Meanwhile every prediction is stored, so the system is quietly
 * building the dataset that a Tribuo model will later be trained on.
 *
 * <p>Spring injects every {@link RiskRule} it finds. Writing a new rule and annotating it
 * {@code @Component} is enough to include it - this class never changes.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleBasedRiskPredictor implements RiskPredictor {

    private static final String MODEL_NAME = "rule-engine-v1";

    /**
     * A single rule scoring at or above this, with a meaningful weight, escalates the
     * whole verdict to HIGH_RISK regardless of the weighted average.
     *
     * <p>This escalation exists because a weighted mean dilutes extremes. A road with
     * 180 mm of rain on a 27 degree landslide-prone slope but no incident history and no
     * flood exposure averages out to "medium", which is exactly the answer that gets
     * people killed. When one factor says the road is about to fail, that is the answer.
     */
    private static final double DOMINANT_RULE_SCORE = 85;
    private static final double DOMINANT_RULE_MIN_WEIGHT = 0.15;

    private final List<RiskRule> rules;
    private final RiskProperties properties;

    @Override
    public RiskPrediction predict(RiskFeatureVector features) {
        List<RiskPrediction.RuleContribution> scoreCard = new ArrayList<>();
        double total = 0;
        double totalWeight = 0;

        for (RiskRule rule : rules) {
            double weight = properties.getWeights().getOrDefault(rule.weightKey(), 0.0);
            if (weight <= 0) {
                continue;   // a rule can be switched off by setting its weight to zero
            }
            double subScore = clamp(rule.evaluate(features));
            double contribution = subScore * weight;

            total += contribution;
            totalWeight += weight;
            scoreCard.add(new RiskPrediction.RuleContribution(
                    rule.name(), round(subScore), weight, round(contribution),
                    rule.explain(features)));
        }

        // Normalising by the weights that actually fired keeps the score on a 0-100 scale
        // even if someone configures weights that do not add up to 1.
        double score = totalWeight > 0 ? total / totalWeight : 0;
        RiskLevel level = toLevel(score);

        // Dominant-factor escalation: one rule screaming outranks five saying "fine".
        String escalatedBy = dominantRule(scoreCard);
        if (escalatedBy != null && level != RiskLevel.HIGH_RISK) {
            score = Math.max(score, properties.getThresholds().getHigh());
            level = RiskLevel.HIGH_RISK;
            log.debug("Road {} escalated to HIGH_RISK by {}", features.roadId(), escalatedBy);
        }

        // Most important reason first - that is what the dashboard shows.
        scoreCard.sort((a, b) -> Double.compare(b.contribution(), a.contribution()));

        String recommendation = recommendationFor(level, scoreCard);
        if (escalatedBy != null) {
            recommendation = recommendation
                    + " (escalated: " + escalatedBy + " alone is at a critical level)";
        }

        RiskPrediction prediction = new RiskPrediction(
                level, round(score), round(score) / 100.0, MODEL_NAME,
                scoreCard, recommendation);

        log.debug("Road {} scored {} -> {}", features.roadId(), round(score), level);
        return prediction;
    }

    @Override
    public String modelName() {
        return MODEL_NAME;
    }

    /** The name of the first rule severe enough to override the average, or null. */
    private String dominantRule(List<RiskPrediction.RuleContribution> scoreCard) {
        return scoreCard.stream()
                .filter(entry -> entry.subScore() >= DOMINANT_RULE_SCORE
                        && entry.weight() >= DOMINANT_RULE_MIN_WEIGHT)
                .map(RiskPrediction.RuleContribution::rule)
                .findFirst()
                .orElse(null);
    }

    private RiskLevel toLevel(double score) {
        if (score >= properties.getThresholds().getHigh()) {
            return RiskLevel.HIGH_RISK;
        }
        if (score >= properties.getThresholds().getMedium()) {
            return RiskLevel.MEDIUM_RISK;
        }
        return RiskLevel.LOW_RISK;
    }

    /** Turns the verdict into an instruction somebody can actually follow. */
    private String recommendationFor(RiskLevel level,
                                     List<RiskPrediction.RuleContribution> scoreCard) {
        String driver = scoreCard.stream()
                .max((a, b) -> Double.compare(a.contribution(), b.contribution()))
                .map(RiskPrediction.RuleContribution::reason)
                .orElse("no dominant factor");

        return switch (level) {
            case HIGH_RISK -> "Avoid this corridor for the next 24 hours and use an "
                    + "alternate route. Main factor: " + driver;
            case MEDIUM_RISK -> "Passable with caution; keep a fallback route ready. "
                    + "Main factor: " + driver;
            case LOW_RISK -> "No special precautions needed.";
        };
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
