package com.ner.smartlogix.ml;

import com.ner.smartlogix.ml.feature.RiskFeatureVector;

/**
 * The contract that never changes.
 *
 * <p>Today the only implementation is {@code RuleBasedRiskPredictor}. In Phase 8 a
 * {@code TribuoRiskPredictor} can be added, trained on the history the rule engine has
 * been accumulating in {@code road_risk_assessment}. Because both implement this
 * interface and consume the same {@link RiskFeatureVector}, swapping them is a
 * configuration change: no controller, service or database change is involved.
 *
 * <p>This is dependency inversion in practice - the services depend on this abstraction,
 * not on how the prediction happens to be computed today.
 */
public interface RiskPredictor {

    RiskPrediction predict(RiskFeatureVector features);

    /** Recorded on every stored assessment so predictions can be traced to a model. */
    String modelName();
}
