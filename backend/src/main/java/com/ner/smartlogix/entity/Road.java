package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.enums.RoadCondition;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.enums.RoadType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

/**
 * A monitored road segment - the central entity of the whole platform.
 *
 * <p>The geometry is a {@code LineString} (an ordered list of lat/lon points) rather
 * than a single coordinate, which is what makes the important spatial questions
 * answerable: "which roads are within 500 m of this landslide?"
 * ({@code ST_DWithin}) and "does this OSRM route pass through a blocked road?"
 * ({@code ST_Intersects}).
 *
 * <p>The last four static fields are the terrain/history inputs of the AI risk engine
 * (Phase 5). They change rarely, so they live on the road itself instead of being
 * recomputed on every prediction.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(name = "road")
public class Road extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "road_type", nullable = false, length = 30)
    private RoadType roadType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Column(nullable = false, columnDefinition = "geometry(LineString,4326)")
    private LineString geom;

    @Column(name = "length_km", nullable = false)
    private Double lengthKm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private RoadStatus status = RoadStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 20)
    private RoadCondition condition = RoadCondition.GOOD;

    /**
     * Cached copy of the newest RoadRiskAssessment. The dashboard and the map read this
     * thousands of times; joining the full assessment history for every read would be
     * wasteful, so we denormalise deliberately and keep it in sync in the service layer.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_risk_level", nullable = false, length = 20)
    private RiskLevel currentRiskLevel = RiskLevel.LOW_RISK;

    /** Average slope in degrees. Steep hill roads fail far sooner in heavy rain. */
    @Column(name = "slope_degrees")
    private Double slopeDegrees = 0.0;

    /** 0.0 to 1.0, seeded from geological landslide-susceptibility zonation. */
    @Column(name = "landslide_susceptibility")
    private Double landslideSusceptibility = 0.0;

    @Column(name = "flood_prone", nullable = false)
    private boolean floodProne = false;

    /** How many days per year this road has historically been unusable. */
    @Column(name = "historical_block_days_per_year")
    private Double historicalBlockDaysPerYear = 0.0;

    @Column(name = "status_updated_at")
    private OffsetDateTime statusUpdatedAt;
}
