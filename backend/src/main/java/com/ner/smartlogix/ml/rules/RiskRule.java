package com.ner.smartlogix.ml.rules;

import com.ner.smartlogix.ml.feature.RiskFeatureVector;

/**
 * One reason a road might fail.
 *
 * <p>Each rule looks at the features from a single angle and returns a score from 0 (no
 * concern) to 100 (about to fail), plus a sentence explaining itself. The engine combines
 * them using configurable weights.
 *
 * <p>Splitting the model into small independent rules is what makes it testable: each
 * rule has its own unit test, and a domain expert can argue about one threshold without
 * touching anything else.
 */
public interface RiskRule {

    /** Stable name used in the score card and in the weights configuration. */
    String name();

    /** Configuration key under app.risk.weights, e.g. "rainfall". */
    String weightKey();

    /** 0 to 100. */
    double evaluate(RiskFeatureVector features);

    /** Human-readable justification for the score just produced. */
    String explain(RiskFeatureVector features);
}
