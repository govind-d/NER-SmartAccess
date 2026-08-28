package com.ner.smartlogix.ml.rules;

import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import org.springframework.stereotype.Component;

/**
 * Landslide exposure: geology multiplied by slope, then amplified by recent rain.
 *
 * <p>Susceptibility alone does not close a road - a landslide-prone hillside in dry
 * February is fine. It is the combination that matters, which is why this rule multiplies
 * rather than adds.
 */
@Component
public class LandslideRule implements RiskRule {

    @Override
    public String name() {
        return "LandslideRule";
    }

    @Override
    public String weightKey() {
        return "landslide";
    }

    @Override
    public double evaluate(RiskFeatureVector f) {
        // Slope: nothing below 10 degrees, everything above 35 is maximal.
        double slopeFactor = Math.min(1.0, Math.max(0, f.terrainSlopeDegrees() - 10) / 25.0);
        double terrain = (f.landslideSusceptibility() * 0.6 + slopeFactor * 0.4) * 100;

        // Rain is the trigger. Dry ground keeps even a fragile slope in place.
        double rainMultiplier;
        if (f.rainfall72h() >= 250) {
            rainMultiplier = 1.0;
        } else if (f.rainfall72h() >= 100) {
            rainMultiplier = 0.75;
        } else if (f.rainfall72h() >= 40) {
            rainMultiplier = 0.5;
        } else {
            rainMultiplier = 0.25;
        }

        double score = terrain * rainMultiplier;
        if (f.monsoonMonth()) {
            score *= 1.15;
        }
        return Math.min(100, score);
    }

    @Override
    public String explain(RiskFeatureVector f) {
        return "susceptibility %.2f on a %.0f degree slope with %.0f mm of rain over 72 h"
                .formatted(f.landslideSusceptibility(), f.terrainSlopeDegrees(),
                        f.rainfall72h());
    }
}
