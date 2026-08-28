package com.ner.smartlogix.entity;

import com.ner.smartlogix.enums.AccessibilityLevel;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A snapshot of how reachable a district was at a point in time.
 *
 * <p>Kept as history rather than a single column on District so the dashboard can show
 * "Dima Hasao was cut off for 4 days in June" instead of only the situation right now.
 * The road counters are stored with the snapshot because recounting them for a past
 * date would be impossible once road statuses have moved on.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "accessibility_status")
public class AccessibilityStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false)
    private District district;

    @Enumerated(EnumType.STRING)
    @Column(name = "accessibility_level", nullable = false, length = 25)
    private AccessibilityLevel accessibilityLevel;

    @Column(name = "open_roads", nullable = false)
    private Integer openRoads = 0;

    @Column(name = "blocked_roads", nullable = false)
    private Integer blockedRoads = 0;

    @Column(name = "high_risk_roads", nullable = false)
    private Integer highRiskRoads = 0;

    @Column(columnDefinition = "text")
    private String remarks;

    @Column(name = "evaluated_at", nullable = false)
    private OffsetDateTime evaluatedAt;
}
