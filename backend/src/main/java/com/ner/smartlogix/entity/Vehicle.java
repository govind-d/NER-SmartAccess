package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.VehicleType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A logistics vehicle in the fleet.
 *
 * <p>The three {@code last*} fields duplicate the newest row of
 * {@link VehicleLocation} on purpose. The live map needs the current position of every
 * vehicle in a single query; without this cache that would be a correlated subquery
 * over a table holding hundreds of thousands of GPS pings.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(name = "vehicle")
public class Vehicle extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Registration number, e.g. ML01AB1234. */
    @Column(name = "vehicle_number", nullable = false, unique = true, length = 20)
    private String vehicleNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "capacity_tons")
    private Double capacityTons;

    /** One driver drives at most one vehicle, hence the unique join column. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", unique = true)
    private User driver;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_latitude")
    private Double lastLatitude;

    @Column(name = "last_longitude")
    private Double lastLongitude;

    @Column(name = "last_seen_at")
    private OffsetDateTime lastSeenAt;
}
