package com.ner.smartlogix.ml.rules;

import com.ner.smartlogix.ml.feature.RiskFeatureVector;
import org.springframework.stereotype.Component;

/**
 * The road's own track record.
 *
 * <p>A stretch that failed three times last month will very likely fail again: the
 * underlying cause is rarely fixed between monsoon showers. This rule is what lets the
 * engine learn from the past without any machine learning at all - and the same history
 * becomes the training data for the real model in Phase 8.
 */
@Component
public class IncidentHistoryRule implements RiskRule {

    @Override
    public String name() {
        return "IncidentHistoryRule";
    }

    @Override
    public String weightKey() {
        return "incident-history";
    }

    @Override
    public double evaluate(RiskFeatureVector f) {
        // Recent incidents, saturating at four - beyond that the road is simply bad.
        double recent = Math.min(4, f.incidentCount30d()) / 4.0 * 60;

        // Recency: a failure yesterday matters far more than one eleven months ago.
        double recency;
        int days = f.daysSinceLastIncident();
        if (days <= 7) {
            recency = 25;
        } else if (days <= 30) {
            recency = 15;
        } else if (days <= 90) {
            recency = 7;
        } else {
            recency = 0;
        }

        // Long-run reputation: days per year this road is normally unusable.
        double chronic = Math.min(15, f.historicalBlockDaysPerYear() / 45.0 * 15);

        return Math.min(100, recent + recency + chronic);
    }

    @Override
    public String explain(RiskFeatureVector f) {
        String last = f.daysSinceLastIncident() >= 999
                ? "no recorded incident"
                : "last one %d day(s) ago".formatted(f.daysSinceLastIncident());
        return "%d incident(s) in 30 days, %s, normally blocked %.0f day(s) a year"
                .formatted(f.incidentCount30d(), last, f.historicalBlockDaysPerYear());
    }
}
