package com.ner.smartlogix.ml.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.ner.smartlogix.enums.RoadCondition;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.enums.WeatherCondition;
import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Each rule of the AI engine is tested on its own.
 *
 * <p>This is the pay-off for splitting the model into small independent rules: every
 * threshold can be checked directly, in milliseconds, with no Spring context and no
 * database. It is also the evidence that the engine behaves the way the project report
 * claims it does.
 */
class RiskRuleTest {

    /** A baseline feature vector that each test perturbs in one direction. */
    private RiskFeatureVector features(double rain24, double rain72, double slope,
                                       double susceptibility, boolean floodProne,
                                       RoadCondition condition, int incidents30d,
                                       int daysSinceIncident, boolean monsoon) {
        return new RiskFeatureVector(1L, rain24, rain72,
                rain24 > 64.5 ? WeatherCondition.HEAVY_RAIN : WeatherCondition.CLEAR,
                slope, susceptibility, floodProne, condition, RoadStatus.OPEN,
                incidents30d, daysSinceIncident, 5.0, 1, monsoon, null);
    }

    @Nested
    @DisplayName("RainfallRule")
    class Rainfall {
        private final RainfallRule rule = new RainfallRule();

        @Test
        @DisplayName("a dry day scores near zero")
        void dryDay() {
            assertThat(rule.evaluate(features(0, 0, 10, 0.2, false,
                    RoadCondition.GOOD, 0, 999, false))).isLessThan(5);
        }

        @Test
        @DisplayName("IMD heavy rainfall (64.5 mm) crosses into the upper half")
        void heavyRain() {
            assertThat(rule.evaluate(features(64.5, 90, 10, 0.2, false,
                    RoadCondition.GOOD, 0, 999, false))).isGreaterThanOrEqualTo(55);
        }

        @Test
        @DisplayName("extremely heavy rainfall saturates the rule at 100")
        void extremeRain() {
            assertThat(rule.evaluate(features(230, 400, 10, 0.2, false,
                    RoadCondition.GOOD, 0, 999, false))).isEqualTo(100);
        }

        @Test
        @DisplayName("three days of rain scores higher than the same day in isolation")
        void sustainedRainAddsRisk() {
            double burst = rule.evaluate(features(70, 75, 10, 0.2, false,
                    RoadCondition.GOOD, 0, 999, false));
            double sustained = rule.evaluate(features(70, 300, 10, 0.2, false,
                    RoadCondition.GOOD, 0, 999, false));
            assertThat(sustained).isGreaterThan(burst);
        }
    }

    @Nested
    @DisplayName("LandslideRule")
    class Landslide {
        private final LandslideRule rule = new LandslideRule();

        @Test
        @DisplayName("a fragile slope in dry weather is not an emergency")
        void drySteepSlope() {
            assertThat(rule.evaluate(features(0, 5, 30, 0.9, false,
                    RoadCondition.GOOD, 0, 999, false))).isLessThan(30);
        }

        @Test
        @DisplayName("the same slope after three days of rain is")
        void wetSteepSlope() {
            assertThat(rule.evaluate(features(120, 300, 30, 0.9, false,
                    RoadCondition.GOOD, 0, 999, true))).isGreaterThan(70);
        }

        @Test
        @DisplayName("flat ground stays low however wet it gets")
        void flatGround() {
            assertThat(rule.evaluate(features(150, 350, 2, 0.05, false,
                    RoadCondition.GOOD, 0, 999, true))).isLessThan(25);
        }
    }

    @Nested
    @DisplayName("FloodRule")
    class Flood {
        private final FloodRule rule = new FloodRule();

        @Test
        @DisplayName("a road on a ridge does not flood however hard it rains")
        void notFloodProne() {
            assertThat(rule.evaluate(features(200, 400, 25, 0.5, false,
                    RoadCondition.GOOD, 0, 999, true))).isLessThanOrEqualTo(20);
        }

        @Test
        @DisplayName("a flat flood-prone stretch under heavy rain scores high")
        void floodProneValley() {
            assertThat(rule.evaluate(features(130, 250, 2, 0.1, true,
                    RoadCondition.GOOD, 0, 999, true))).isGreaterThan(75);
        }
    }

    @Nested
    @DisplayName("IncidentHistoryRule")
    class History {
        private final IncidentHistoryRule rule = new IncidentHistoryRule();

        @Test
        @DisplayName("a road that has never failed scores low")
        void cleanRecord() {
            assertThat(rule.evaluate(features(0, 0, 10, 0.2, false,
                    RoadCondition.GOOD, 0, 999, false))).isLessThan(20);
        }

        @Test
        @DisplayName("repeated recent failures dominate the score")
        void repeatOffender() {
            assertThat(rule.evaluate(features(0, 0, 10, 0.2, false,
                    RoadCondition.GOOD, 4, 2, false))).isGreaterThan(80);
        }

        @Test
        @DisplayName("an old incident matters less than a fresh one")
        void recencyMatters() {
            double fresh = rule.evaluate(features(0, 0, 10, 0.2, false,
                    RoadCondition.GOOD, 2, 3, false));
            double stale = rule.evaluate(features(0, 0, 10, 0.2, false,
                    RoadCondition.GOOD, 2, 200, false));
            assertThat(fresh).isGreaterThan(stale);
        }
    }

    @Nested
    @DisplayName("RoadConditionRule")
    class Condition {
        private final RoadConditionRule rule = new RoadConditionRule();

        @Test
        @DisplayName("scores rise monotonically from GOOD to DAMAGED")
        void monotonic() {
            double good = rule.evaluate(features(0, 0, 5, 0, false,
                    RoadCondition.GOOD, 0, 999, false));
            double fair = rule.evaluate(features(0, 0, 5, 0, false,
                    RoadCondition.FAIR, 0, 999, false));
            double poor = rule.evaluate(features(0, 0, 5, 0, false,
                    RoadCondition.POOR, 0, 999, false));
            double damaged = rule.evaluate(features(0, 0, 5, 0, false,
                    RoadCondition.DAMAGED, 0, 999, false));
            assertThat(good).isLessThan(fair);
            assertThat(fair).isLessThan(poor);
            assertThat(poor).isLessThan(damaged);
        }

        @Test
        @DisplayName("a damaged bridge raises the score of an otherwise good road")
        void weakBridgeCounts() {
            RiskFeatureVector withBridge = new RiskFeatureVector(1L, 0, 0,
                    WeatherCondition.CLEAR, 5, 0, false, RoadCondition.GOOD,
                    RoadStatus.OPEN, 0, 999, 0, 1, false, RoadCondition.DAMAGED);
            double withoutBridge = rule.evaluate(features(0, 0, 5, 0, false,
                    RoadCondition.GOOD, 0, 999, false));
            assertThat(rule.evaluate(withBridge)).isGreaterThan(withoutBridge);
        }
    }

    @Nested
    @DisplayName("TerrainRule")
    class Terrain {
        private final TerrainRule rule = new TerrainRule();

        @Test
        @DisplayName("plains score far below mountain passes")
        void slopeDominates() {
            double plains = rule.evaluate(features(0, 0, 1, 0, false,
                    RoadCondition.GOOD, 0, 999, false));
            double mountain = rule.evaluate(features(0, 0, 30, 0, false,
                    RoadCondition.GOOD, 0, 999, false));
            assertThat(mountain).isGreaterThan(plains + 50);
        }
    }

    @Test
    @DisplayName("every rule stays inside 0 to 100 even for absurd inputs")
    void rulesAreBounded() {
        RiskFeatureVector extreme = new RiskFeatureVector(1L, 900, 2000,
                WeatherCondition.THUNDERSTORM, 89, 1.0, true, RoadCondition.DAMAGED,
                RoadStatus.BLOCKED, 50, 0, 300, 4, true, RoadCondition.DAMAGED);

        List<RiskRule> rules = List.of(new RainfallRule(), new LandslideRule(),
                new FloodRule(), new IncidentHistoryRule(), new RoadConditionRule(),
                new TerrainRule());

        for (RiskRule rule : rules) {
            assertThat(rule.evaluate(extreme))
                    .as("%s must stay within 0..100", rule.name())
                    .isBetween(0.0, 100.0);
        }
    }
}
