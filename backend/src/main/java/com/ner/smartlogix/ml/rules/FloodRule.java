package com.ner.smartlogix.ml.rules;

import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import org.springframework.stereotype.Component;

/**
 * Flooding, which behaves almost opposite to landslides: it threatens the flat valley
 * roads of the Brahmaputra and Barak plains rather than the hill sections, and it
 * responds to short, intense bursts of rain.
 */
@Component
public class FloodRule implements RiskRule {

    @Override
    public String name() {
        return "FloodRule";
    }

    @Override
    public String weightKey() {
        return "flood";
    }

    @Override
    public double evaluate(RiskFeatureVector f) {
        if (!f.floodProne()) {
            // A road on a ridge does not flood however hard it rains.
            return Math.min(20, f.rainfall24h() / 10);
        }
        double score = 25;
        if (f.rainfall24h() >= 115.5) {
            score += 45;
        } else if (f.rainfall24h() >= 64.5) {
            score += 30;
        } else if (f.rainfall24h() >= 30) {
            score += 15;
        }
        if (f.rainfall72h() >= 200) {
            score += 20;
        }
        // Flat terrain drains slowly, so standing water lingers.
        if (f.terrainSlopeDegrees() < 5) {
            score += 10;
        }
        return Math.min(100, score);
    }

    @Override
    public String explain(RiskFeatureVector f) {
        return f.floodProne()
                ? "flood-prone stretch with %.0f mm in 24 h".formatted(f.rainfall24h())
                : "not flood-prone";
    }
}
