package com.ner.smartlogix.config;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Weights and thresholds of the risk engine, bound from {@code app.risk} in
 * application.yml.
 *
 * <p>These numbers live in configuration rather than in Java because they are model
 * parameters, not logic. A domain expert can retune them without a recompile - and when
 * Phase 8 learns weights from data, the learned values can be written straight into the
 * same keys.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.risk")
public class RiskProperties {

    /** Keyed by RiskRule.weightKey(), e.g. "rainfall" -> 0.30 */
    private Map<String, Double> weights = new LinkedHashMap<>();

    private Thresholds thresholds = new Thresholds();
    private RouteWeights route = new RouteWeights();

    @Getter
    @Setter
    public static class Thresholds {
        /** At or above this score a road is MEDIUM_RISK. */
        private double medium = 35;
        /** At or above this score a road is HIGH_RISK. */
        private double high = 65;
    }

    @Getter
    @Setter
    public static class RouteWeights {
        private double travelTime = 0.35;
        private double weather = 0.20;
        private double road = 0.20;
        private double incident = 0.15;
        private double disruption = 0.10;
    }
}
