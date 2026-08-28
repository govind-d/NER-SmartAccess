package com.ner.smartlogix.event;

import com.ner.smartlogix.enums.RiskLevel;

/** Published when the AI engine moves a road to a different risk level. */
public record RiskLevelChangedEvent(Long roadId, String roadCode, RiskLevel previousLevel,
                                    RiskLevel newLevel, double disruptionScore,
                                    String explanation) {
}
