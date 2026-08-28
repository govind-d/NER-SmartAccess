package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.BridgeStatus;
import com.ner.smartlogix.enums.RoadCondition;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

/**
 * A bridge on a road. Modelled separately because in the NER a single weak or washed
 * out bridge closes an entire corridor even when every road around it is intact, and
 * because heavy vehicles must respect a load limit that the road itself does not have.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(name = "bridge")
public class Bridge extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "road_id", nullable = false)
    private Road road;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(name = "load_capacity_tons")
    private Double loadCapacityTons;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 20)
    private RoadCondition condition = RoadCondition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private BridgeStatus status = BridgeStatus.OPEN;

    @Column(name = "last_inspection_date")
    private LocalDate lastInspectionDate;
}
