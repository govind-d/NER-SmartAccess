package com.ner.smartlogix.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One leg of a route.
 *
 * <p>This is the bridge between a route geometry computed by an external engine and
 * OUR monitored road network: each segment optionally points at a {@link Road} row,
 * which is what lets the system say "this route passes three HIGH_RISK roads".
 * {@code road} is nullable because part of a route may run over roads we do not track.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "route_segment",
       uniqueConstraints = @UniqueConstraint(columnNames = {"route_id", "sequence_no"}))
public class RouteSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "road_id")
    private Road road;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(name = "distance_km", nullable = false)
    private Double distanceKm;

    @Column(name = "duration_min", nullable = false)
    private Integer durationMin;

    @Column(name = "segment_risk_score")
    private Double segmentRiskScore = 0.0;
}
