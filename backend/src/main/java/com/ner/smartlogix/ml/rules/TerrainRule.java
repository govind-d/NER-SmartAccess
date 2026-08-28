package com.ner.smartlogix.ml.rules;

import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import org.springframework.stereotype.Component;

/**
 * Terrain and traffic.
 *
 * <p>Steep mountain sections are slower to clear once anything goes wrong: a single
 * stranded truck on a narrow hill road blocks it completely, where the same truck on a
 * plains highway is simply overtaken. Congestion therefore multiplies terrain risk rather
 * than being counted separately.
 */
@Component
public class TerrainRule implements RiskRule {

    @Override
    public String name() {
        return "TerrainRule";
    }

    @Override
    public String weightKey() {
        return "terrain";
    }

    @Override
    public double evaluate(RiskFeatureVector f) {
        // 0 degrees -> 0, 30 degrees and above -> 80.
        double slope = Math.min(80, f.terrainSlopeDegrees() / 30.0 * 80);
        double congestion = f.trafficCongestionLevel() / 4.0 * 20;
        double score = slope + congestion;

        if (f.monsoonMonth() && f.terrainSlopeDegrees() > 15) {
            score += 10;
        }
        return Math.min(100, score);
    }

    @Override
    public String explain(RiskFeatureVector f) {
        return "%.0f degree average slope, congestion level %d/4%s".formatted(
                f.terrainSlopeDegrees(), f.trafficCongestionLevel(),
                f.monsoonMonth() ? ", monsoon month" : "");
    }
}
