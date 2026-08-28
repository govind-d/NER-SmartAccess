package com.ner.smartlogix.ml.rules;

import com.ner.smartlogix.enums.WeatherCondition;
import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import org.springframework.stereotype.Component;

/**
 * Rainfall, the single strongest predictor of road failure in the North East.
 *
 * <p>The thresholds follow the categories the India Meteorological Department uses:
 * 64.5 mm in a day is "heavy", 115.5 mm is "very heavy", 204.5 mm is "extremely heavy".
 * Three-day totals matter separately because a hillside that has been soaking for
 * seventy-two hours slips under rain that a dry slope would shrug off.
 */
@Component
public class RainfallRule implements RiskRule {

    @Override
    public String name() {
        return "RainfallRule";
    }

    @Override
    public String weightKey() {
        return "rainfall";
    }

    @Override
    public double evaluate(RiskFeatureVector f) {
        double daily = f.rainfall24h();
        double score;
        if (daily >= 204.5) {
            score = 100;
        } else if (daily >= 115.5) {
            score = 80 + (daily - 115.5) / (204.5 - 115.5) * 20;
        } else if (daily >= 64.5) {
            score = 55 + (daily - 64.5) / (115.5 - 64.5) * 25;
        } else if (daily >= 15.6) {
            score = 20 + (daily - 15.6) / (64.5 - 15.6) * 35;
        } else {
            score = daily / 15.6 * 20;
        }

        // Sustained rain adds on top: saturated ground fails at lower daily totals.
        if (f.rainfall72h() >= 250) {
            score += 15;
        } else if (f.rainfall72h() >= 150) {
            score += 8;
        }

        if (f.weatherCondition() == WeatherCondition.THUNDERSTORM) {
            score += 5;
        }
        return Math.min(100, score);
    }

    @Override
    public String explain(RiskFeatureVector f) {
        return "%.0f mm in 24 h and %.0f mm over 72 h (%s)".formatted(
                f.rainfall24h(), f.rainfall72h(),
                f.weatherCondition() == null ? "no reading" : f.weatherCondition());
    }
}
