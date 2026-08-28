package com.ner.smartlogix.event;

import com.ner.smartlogix.dto.request.RoadStatusRequest;
import com.ner.smartlogix.dto.response.RoadStatusChangeResponse;
import com.ner.smartlogix.entity.Delivery;
import com.ner.smartlogix.entity.Incident;
import com.ner.smartlogix.entity.Road;
import com.ner.smartlogix.enums.DeliveryStatus;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.enums.Severity;
import com.ner.smartlogix.repository.DeliveryRepository;
import com.ner.smartlogix.repository.DistrictRepository;
import com.ner.smartlogix.repository.IncidentRepository;
import com.ner.smartlogix.repository.RoadRepository;
import com.ner.smartlogix.service.AlertService;
import com.ner.smartlogix.service.DistrictService;
import com.ner.smartlogix.service.RoadService;
import com.ner.smartlogix.websocket.RealtimeBroadcaster;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The consequences of an incident, in one file.
 *
 * <p>This is the flagship flow of the whole platform. A field officer reports a landslide
 * and, without anybody clicking anything else:
 * <ol>
 *   <li>the affected road is closed or marked high risk;</li>
 *   <li>district accessibility is recalculated, and a district with no way in raises a
 *       CRITICAL alert;</li>
 *   <li>an alert is created and pushed to every browser;</li>
 *   <li>consignments already travelling that road are flagged DELAYED so a manager can
 *       re-route them.</li>
 * </ol>
 *
 * <p>The listeners run inside the caller's transaction, so either the incident and all of
 * its consequences are stored, or none of them are. That consistency is worth more here
 * than the throughput an asynchronous listener would buy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentImpactListener {

    private final IncidentRepository incidentRepository;
    private final RoadRepository roadRepository;
    private final DeliveryRepository deliveryRepository;
    private final DistrictRepository districtRepository;
    private final RoadService roadService;
    private final DistrictService districtService;
    private final AlertService alertService;
    private final RealtimeBroadcaster broadcaster;

    /** Step 1: a serious incident closes the road it happened on. */
    @EventListener
    @Order(10)
    @Transactional
    public void closeAffectedRoad(IncidentReportedEvent event) {
        Incident incident = incidentRepository.findById(event.incidentId()).orElse(null);
        if (incident == null || incident.getRoad() == null) {
            return;
        }
        Road road = incident.getRoad();

        RoadStatus newStatus = switch (incident.getSeverity()) {
            case CRITICAL -> RoadStatus.BLOCKED;
            case HIGH -> RoadStatus.HIGH_RISK;
            case MEDIUM -> RoadStatus.PARTIALLY_ACCESSIBLE;
            case LOW -> null;   // a minor report should not close a national highway
        };
        if (newStatus == null || road.getStatus() == newStatus) {
            return;
        }
        // An already blocked road must not be "upgraded" to merely high risk by a
        // second, less severe report.
        if (road.getStatus() == RoadStatus.BLOCKED && newStatus != RoadStatus.BLOCKED) {
            return;
        }

        roadService.changeStatus(road.getId(), new RoadStatusRequest(newStatus,
                "Automatic: %s reported (%s)".formatted(
                        incident.getIncidentType(), incident.getSeverity())));
        log.info("Incident #{} set road {} to {}", incident.getId(), road.getCode(), newStatus);
    }

    /** Step 2: every incident raises an alert, however small. */
    @EventListener
    @Order(20)
    @Transactional
    public void raiseAlert(IncidentReportedEvent event) {
        incidentRepository.findById(event.incidentId())
                .ifPresent(alertService::raiseIncidentAlert);
    }

    /**
     * Reacts to any road status change, whoever caused it - the listener above, or an
     * official pressing a button in the dashboard.
     */
    @EventListener
    @Transactional
    public void onRoadStatusChanged(RoadStatusChangedEvent event) {
        // Recolour the line on every open map, immediately.
        broadcaster.roadStatusChanged(new RoadStatusChangeResponse(
                event.roadId(), event.roadCode(), event.roadName(),
                event.previousStatus(), event.newStatus(), event.reason(),
                OffsetDateTime.now()));

        roadRepository.findById(event.roadId()).ifPresent(road -> {
            if (event.newStatus() == RoadStatus.BLOCKED
                    || event.newStatus() == RoadStatus.HIGH_RISK) {
                alertService.raiseRoadStatusAlert(road, event.reason());
            }
        });

        // Recompute how reachable the district now is, and shout if it is cut off.
        var accessibility = districtService.evaluateAccessibility(event.districtId());
        if (accessibility.accessibilityLevel()
                == com.ner.smartlogix.enums.AccessibilityLevel.CUT_OFF) {
            districtRepository.findById(event.districtId())
                    .ifPresent(alertService::raiseDistrictInaccessibleAlert);
        }

        if (event.newStatus() == RoadStatus.BLOCKED) {
            flagAffectedDeliveries(event.roadId(), event.roadName());
        }
    }

    /**
     * Any consignment currently travelling a road that just closed is marked DELAYED so
     * that a manager sees it on the dashboard and can assign an alternate route.
     */
    private void flagAffectedDeliveries(Long roadId, String roadName) {
        List<Delivery> affected = deliveryRepository.findActiveDeliveriesUsingRoad(roadId);
        for (Delivery delivery : affected) {
            if (delivery.getStatus() == DeliveryStatus.DELAYED) {
                continue;
            }
            delivery.setStatus(DeliveryStatus.DELAYED);
            deliveryRepository.save(delivery);
            alertService.raiseDeliveryDelayAlert(delivery, delivery.getDelayMinutes(),
                    "Route blocked at " + roadName + ". An alternate route is needed.");
            log.info("Delivery {} flagged DELAYED: {} is blocked",
                    delivery.getTrackingCode(), roadName);
        }
    }

    /** Delay events raised elsewhere (the overdue job, a manual status change). */
    @EventListener
    @Transactional
    public void onDeliveryDelayed(DeliveryDelayedEvent event) {
        deliveryRepository.findById(event.deliveryId()).ifPresent(delivery ->
                alertService.raiseDeliveryDelayAlert(delivery, event.delayMinutes(),
                        event.reason()));
    }

    /** The AI engine moved a road to a new risk level. */
    @EventListener
    @Transactional
    public void onRiskLevelChanged(RiskLevelChangedEvent event) {
        roadRepository.findById(event.roadId()).ifPresent(road -> {
            if (event.newLevel() == com.ner.smartlogix.enums.RiskLevel.HIGH_RISK) {
                alertService.raiseRiskAlert(road, event.newLevel(), event.explanation());
                // A high predicted risk downgrades an open road to HIGH_RISK, but never
                // re-opens a road that a human or an incident has already blocked.
                if (road.getStatus() == RoadStatus.OPEN) {
                    roadService.changeStatus(road.getId(), new RoadStatusRequest(
                            RoadStatus.HIGH_RISK,
                            "Automatic: predicted disruption risk is high"));
                }
            }
        });
    }

    /** Kept for readability of the severity switch above. */
    @SuppressWarnings("unused")
    private boolean isSerious(Severity severity) {
        return severity == Severity.HIGH || severity == Severity.CRITICAL;
    }
}
