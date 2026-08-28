package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.RiskPredictionRequest;
import com.ner.smartlogix.dto.response.RiskAssessmentResponse;
import java.util.List;

public interface RiskService {

    /** Runs the engine for one road now and stores the result. */
    RiskAssessmentResponse assessRoad(Long roadId);

    /** Answers a hypothetical without storing anything. */
    RiskAssessmentResponse predictWhatIf(RiskPredictionRequest request);

    RiskAssessmentResponse currentRisk(Long roadId);

    List<RiskAssessmentResponse> history(Long roadId, int days);

    /** Re-scores every road, or every road of one district. Returns how many changed. */
    int reassessAll(Long districtId);

    String activeModelName();
}
