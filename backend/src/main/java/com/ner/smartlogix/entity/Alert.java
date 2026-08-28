package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.AlertType;
import com.ner.smartlogix.enums.Severity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

/**
 * Something the platform decided people need to know about right now.
 *
 * <p>An alert is created by the system (an incident was reported, rainfall crossed a
 * threshold, a delivery slipped) or manually by an official. All four foreign keys are
 * nullable because different alert types point at different things: a rainfall alert
 * has a district, a blocked-road alert has a road, a delay alert has a delivery.
 *
 * <p>An alert is the message itself; a {@link Notification} is one delivery of that
 * message to one user. One alert therefore fans out into many notifications.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(name = "alert")
public class Alert extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 30)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Severity severity;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id")
    private District district;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "road_id")
    private Road road;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    @Column(columnDefinition = "geometry(Point,4326)")
    private Point location;

    /** Alerts are deactivated, never deleted - the history is evidence. */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;
}
