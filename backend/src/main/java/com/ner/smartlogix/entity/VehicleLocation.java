package com.ner.smartlogix.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

/**
 * One GPS ping from one vehicle - the only high-volume table in the system
 * (roughly one row per vehicle every ten seconds).
 *
 * <p>It stores the position twice: as a PostGIS {@code Point} for spatial queries
 * ("is this vehicle inside a danger zone?") and as two plain numbers, because the
 * JSON sent to the browser and to Leaflet needs latitude and longitude anyway and
 * reading them directly avoids parsing geometry on every websocket message.
 *
 * <p>It deliberately does not extend BaseAuditEntity: {@code recordedAt} already says
 * when the ping happened, and four extra audit columns per row would be pure waste.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vehicle_location")
public class VehicleLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point location;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "speed_kmph")
    private Double speedKmph;

    /** Direction of travel in degrees, 0 = north. Used to rotate the map marker. */
    private Double heading;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;
}
