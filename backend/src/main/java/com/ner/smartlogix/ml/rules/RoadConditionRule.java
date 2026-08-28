package com.ner.smartlogix.ml.rules;

import com.ner.smartlogix.enums.RoadCondition;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import org.springframework.stereotype.Component;

/**
 * The state of the surface and of the weakest bridge on the road.
 *
 * <p>Bridges are included here deliberately: a damaged bridge closes a corridor just as
 * effectively as a landslide, and the platform must not report a road as safe because the
 * tarmac happens to be in good condition.
 */
@Component
public class RoadConditionRule implements RiskRule {

    @Override
    public String name() {
        return "RoadConditionRule";
    }

    @Override
    public String weightKey() {
        return "road-condition";
    }

    @Override
    public double evaluate(RiskFeatureVector f) {
        double surface = switch (f.roadCondition() == null ? RoadCondition.GOOD : f.roadCondition()) {
            case GOOD -> 10;
            case FAIR -> 35;
            case POOR -> 65;
            case DAMAGED -> 90;
        };

        double bridge = f.worstBridgeCondition() == null ? 0 : switch (f.worstBridgeCondition()) {
            case GOOD -> 0;
            case FAIR -> 10;
            case POOR -> 25;
            case DAMAGED -> 40;
        };

        // A road already known to be in trouble should not be scored as if it were fine.
        double statusPenalty = switch (f.roadStatus() == null ? RoadStatus.OPEN : f.roadStatus()) {
            case OPEN -> 0;
            case PARTIALLY_ACCESSIBLE -> 15;
            case HIGH_RISK -> 25;
            case BLOCKED -> 40;
        };

        return Math.min(100, surface + bridge + statusPenalty);
    }

    @Override
    public String explain(RiskFeatureVector f) {
        return "surface %s, worst bridge %s, current status %s".formatted(
                f.roadCondition(),
                f.worstBridgeCondition() == null ? "none" : f.worstBridgeCondition(),
                f.roadStatus());
    }
}
