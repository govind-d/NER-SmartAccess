package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.RiskLevel;
import java.time.OffsetDateTime;
import java.util.List;

/** The full explained verdict of the AI engine for one road. */
public record RiskAssessmentResponse(
        Long roadId,
        String roadCode,
        String roadName,
        RiskLevel riskLevel,
        double disruptionScore,
        double disruptionProbability,
        String modelName,
        List<ScoreCardEntry> scoreCard,
        String recommendation,
        OffsetDateTime assessedAt) {

    public record ScoreCardEntry(String rule, double subScore, double weight,
                                 double contribution, String reason) {
    }
}
