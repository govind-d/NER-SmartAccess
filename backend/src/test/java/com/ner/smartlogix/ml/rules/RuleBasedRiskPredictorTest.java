package com.ner.smartlogix.ml.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.ner.smartlogix.config.RiskProperties;
import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.enums.RoadCondition;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.enums.WeatherCondition;
import com.ner.smartlogix.ml.RiskPrediction;
import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The engine as a whole: do the rules combine into a verdict a human would agree with?
 *
 * <p>These are the tests to show in a viva. They demonstrate that the same road moves
 * from LOW_RISK to HIGH_RISK purely because the weather changed, and that the system can
 * always say why.
 */
class RuleBasedRiskPredictorTest {

    private RuleBasedRiskPredictor predictor;

    @BeforeEach
    void setUp() {
        RiskProperties properties = new RiskProperties();
        properties.setWeights(Map.of(
                "rainfall", 0.30,
                "landslide", 0.20,
                "flood", 0.15,
                "incident-history", 0.15,
                "road-condition", 0.10,
                "terrain", 0.10));

        predictor = new RuleBasedRiskPredictor(
                List.of(new RainfallRule(), new LandslideRule(), new FloodRule(),
                        new IncidentHistoryRule(), new RoadConditionRule(), new TerrainRule()),
                properties);
    }

    /** A dry, well-maintained plains highway with no history of trouble. */
    private RiskFeatureVector calmPlainsRoad() {
        return new RiskFeatureVector(1L, 2, 6, WeatherCondition.CLEAR, 2, 0.05, false,
                RoadCondition.GOOD, RoadStatus.OPEN, 0, 999, 1, 1, false, null);
    }

    /** A landslide-prone hill road in the middle of a monsoon cloudburst. */
    private RiskFeatureVector monsoonHillRoad() {
        return new RiskFeatureVector(2L, 142, 310, WeatherCondition.HEAVY_RAIN, 27, 0.82,
                false, RoadCondition.POOR, RoadStatus.OPEN, 3, 4, 22, 2, true,
                RoadCondition.FAIR);
    }

    @Test
    @DisplayName("a calm plains road is LOW_RISK")
    void calmRoadIsLowRisk() {
        RiskPrediction prediction = predictor.predict(calmPlainsRoad());

        assertThat(prediction.riskLevel()).isEqualTo(RiskLevel.LOW_RISK);
        assertThat(prediction.disruptionScore()).isLessThan(35);
        assertThat(prediction.recommendation()).contains("No special precautions");
    }

    @Test
    @DisplayName("a hill road in a cloudburst is HIGH_RISK")
    void monsoonHillRoadIsHighRisk() {
        RiskPrediction prediction = predictor.predict(monsoonHillRoad());

        assertThat(prediction.riskLevel()).isEqualTo(RiskLevel.HIGH_RISK);
        assertThat(prediction.disruptionScore()).isGreaterThan(65);
        assertThat(prediction.recommendation()).contains("Avoid this corridor");
    }

    @Test
    @DisplayName("the score card explains the verdict, strongest reason first")
    void scoreCardIsOrderedAndComplete() {
        RiskPrediction prediction = predictor.predict(monsoonHillRoad());

        assertThat(prediction.scoreCard()).hasSize(6);
        assertThat(prediction.scoreCard())
                .isSortedAccordingTo((a, b) -> Double.compare(b.contribution(), a.contribution()));

        // Every entry must carry a human-readable reason - an unexplained number is
        // useless to the official who has to act on it.
        assertThat(prediction.scoreCard())
                .allSatisfy(entry -> assertThat(entry.reason()).isNotBlank());

        // In a cloudburst on a steep, fragile slope, rain or landslide must lead.
        String topRule = prediction.scoreCard().get(0).rule();
        assertThat(topRule).isIn("RainfallRule", "LandslideRule");
    }

    @Test
    @DisplayName("only the weather changes, and the verdict changes with it")
    void weatherAloneMovesTheVerdict() {
        RiskFeatureVector dry = new RiskFeatureVector(3L, 5, 12, WeatherCondition.CLEAR,
                27, 0.82, false, RoadCondition.FAIR, RoadStatus.OPEN, 0, 999, 10, 2,
                false, null);
        RiskFeatureVector wet = new RiskFeatureVector(3L, 180, 350,
                WeatherCondition.THUNDERSTORM, 27, 0.82, false, RoadCondition.FAIR,
                RoadStatus.OPEN, 0, 999, 10, 2, true, null);

        RiskPrediction dryPrediction = predictor.predict(dry);
        RiskPrediction wetPrediction = predictor.predict(wet);

        assertThat(dryPrediction.disruptionScore()).isLessThan(wetPrediction.disruptionScore());
        assertThat(wetPrediction.riskLevel()).isEqualTo(RiskLevel.HIGH_RISK);
    }

    @Test
    @DisplayName("a rule with weight zero is switched off entirely")
    void zeroWeightDisablesARule() {
        RiskProperties properties = new RiskProperties();
        properties.setWeights(Map.of("rainfall", 0.0, "terrain", 1.0));
        RuleBasedRiskPredictor tuned = new RuleBasedRiskPredictor(
                List.of(new RainfallRule(), new TerrainRule()), properties);

        RiskPrediction prediction = tuned.predict(monsoonHillRoad());

        assertThat(prediction.scoreCard()).hasSize(1);
        assertThat(prediction.scoreCard().get(0).rule()).isEqualTo("TerrainRule");
    }

    @Test
    @DisplayName("the model reports its own name so predictions can be traced")
    void modelIsIdentified() {
        assertThat(predictor.modelName()).isEqualTo("rule-engine-v1");
        assertThat(predictor.predict(calmPlainsRoad()).modelName())
                .isEqualTo(predictor.modelName());
    }

    @Test
    @DisplayName("the score always stays within 0 to 100")
    void scoreIsBounded() {
        RiskFeatureVector extreme = new RiskFeatureVector(4L, 900, 2000,
                WeatherCondition.THUNDERSTORM, 89, 1.0, true, RoadCondition.DAMAGED,
                RoadStatus.BLOCKED, 50, 0, 300, 4, true, RoadCondition.DAMAGED);

        assertThat(predictor.predict(extreme).disruptionScore()).isBetween(0.0, 100.0);
        assertThat(predictor.predict(calmPlainsRoad()).disruptionScore()).isBetween(0.0, 100.0);
    }
}
