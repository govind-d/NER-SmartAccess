package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.RiskLevel;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;

/**
 * A computed route between two points, together with the risk score our own engine
 * gave it (Phase 7).
 *
 * <p>A route is stored rather than recomputed because a delivery must remember which
 * route it was actually sent on, and because the dashboard compares the recommended
 * route with the alternates that were rejected. {@code parentRoute} links an alternate
 * back to the route it is an alternative to.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(name = "route")
public class Route extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 150)
    private String name;

    @Column(columnDefinition = "geometry(LineString,4326)")
    private LineString geom;

    @Column(name = "total_distance_km", nullable = false)
    private Double totalDistanceKm;

    @Column(name = "estimated_duration_min", nullable = false)
    private Integer estimatedDurationMin;

    /** Composite score from RouteRiskScorer. Lower is better. */
    @Column(name = "risk_score", nullable = false)
    private Double riskScore = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 20)
    private RiskLevel riskLevel = RiskLevel.LOW_RISK;

    /** OSRM, ORS or INTERNAL - which engine produced the geometry. */
    @Column(nullable = false, length = 20)
    private String provider = "OSRM";

    @Column(name = "is_alternate", nullable = false)
    private boolean alternate = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_route_id")
    private Route parentRoute;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    /**
     * Cascade + orphanRemoval: segments have no life of their own, so deleting a route
     * must delete its segments.
     */
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNo ASC")
    private List<RouteSegment> segments = new ArrayList<>();

    public void addSegment(RouteSegment segment) {
        segments.add(segment);
        segment.setRoute(this);
    }
}
