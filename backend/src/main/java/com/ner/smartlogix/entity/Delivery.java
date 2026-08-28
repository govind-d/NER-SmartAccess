package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.DeliveryStatus;
import com.ner.smartlogix.enums.GoodsType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A consignment moving from one district to another.
 *
 * <p>{@code trackingCode} is a short public identifier that can be shared outside the
 * system, so the database id never has to be exposed.
 *
 * <p>{@code eta} is recomputed whenever the route risk changes; {@code delayMinutes}
 * is the difference between the original and the current estimate and is what the
 * "delayed deliveries" dashboard card counts.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(name = "delivery")
public class Delivery extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracking_code", nullable = false, unique = true, length = 24)
    private String trackingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_district_id", nullable = false)
    private District sourceDistrict;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_district_id", nullable = false)
    private District destinationDistrict;

    /** Free-text pickup point inside the source district, e.g. "Guwahati Central Warehouse". */
    @Column(name = "source_label", length = 150)
    private String sourceLabel;

    @Column(name = "destination_label", length = 150)
    private String destinationLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "goods_type", nullable = false, length = 30)
    private GoodsType goodsType;

    @Column(name = "weight_tons")
    private Double weightTons;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status = DeliveryStatus.CREATED;

    @Column(name = "dispatched_at")
    private OffsetDateTime dispatchedAt;

    /** Estimated time of arrival, recalculated as conditions change. */
    private OffsetDateTime eta;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @Column(name = "delay_minutes", nullable = false)
    private Integer delayMinutes = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdByUser;
}
