package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.IncidentType;
import com.ner.smartlogix.enums.SyncStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The raw form a field officer filled in, possibly with no network.
 *
 * <p>Why this exists alongside {@link Incident}: an incident is a verified fact that
 * changes road status and triggers alerts, while a field report is simply what somebody
 * typed on a phone. Officials review reports and promote the credible ones into
 * incidents, and the audit trail of what was submitted survives either way.
 *
 * <p>{@code capturedAt} is when the officer filled the form; {@code syncedAt} is when
 * the server received it. The gap between the two is the offline period, and keeping
 * both is what makes the offline module honest.
 */
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
@Entity
@Table(name = "field_report")
public class FieldReport extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Generated on the device, unique, makes re-sync idempotent. */
    @Column(name = "client_uuid", nullable = false, unique = true)
    private UUID clientUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private IncidentType reportType;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "photo_path", length = 255)
    private String photoPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status", nullable = false, length = 15)
    private SyncStatus syncStatus = SyncStatus.SYNCED;

    @Column(name = "captured_at", nullable = false)
    private OffsetDateTime capturedAt;

    @Column(name = "synced_at")
    private OffsetDateTime syncedAt;

    /** Set once an official promotes this report into a real incident. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;
}
