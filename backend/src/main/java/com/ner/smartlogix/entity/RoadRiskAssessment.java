package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.RiskLevel;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * One output of the AI risk engine for one road at one moment in time.
 *
 * <p>Three things make this table important:
 * <ul>
 *   <li>{@code scoreCard} (JSONB) stores WHY the engine reached its verdict - which
 *       rules fired, their sub-scores and their contributions. A government-facing
 *       system that cannot explain itself will not be trusted.</li>
 *   <li>{@code modelName} records which engine produced the row
 *       ("rule-engine-v1" now, "tribuo-rf-v2" later), so the two can be compared.</li>
 *   <li>The accumulated history becomes the training data for the real Java ML model
 *       in Phase 8 - the rule engine bootstraps its own successor.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "road_risk_assessment")
public class RoadRiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "road_id", nullable = false)
    private Road road;

    @Column(name = "disruption_score", nullable = false)
    private Double disruptionScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel;

    @Column(name = "model_name", nullable = false, length = 40)
    private String modelName;

    /** Maps a Java String holding JSON onto a real PostgreSQL jsonb column. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_card", columnDefinition = "jsonb")
    private String scoreCard;

    @Column(name = "assessed_at", nullable = false)
    private OffsetDateTime assessedAt;
}
