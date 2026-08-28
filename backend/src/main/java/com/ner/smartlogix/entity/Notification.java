package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.AlertChannel;
import com.ner.smartlogix.enums.NotificationStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One delivery of one {@link Alert} to one user through one channel.
 *
 * <p>Only the WEBSOCKET channel is implemented in Phase 6. EMAIL, SMS and PUSH exist in
 * the enum so that adding them later is a new implementation of an interface rather
 * than a database migration.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id")
    private Alert alert;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AlertChannel channel = AlertChannel.WEBSOCKET;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private NotificationStatus status = NotificationStatus.SENT;

    @Column(name = "read_flag", nullable = false)
    private boolean readFlag = false;

    @Column(name = "sent_at", nullable = false)
    private OffsetDateTime sentAt;
}
