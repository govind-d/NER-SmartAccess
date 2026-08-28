package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.AlertRequest;
import com.ner.smartlogix.dto.response.AlertResponse;
import com.ner.smartlogix.entity.*;
import com.ner.smartlogix.enums.*;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.AlertMapper;
import com.ner.smartlogix.repository.AlertRepository;
import com.ner.smartlogix.repository.DistrictRepository;
import com.ner.smartlogix.repository.RoadRepository;
import com.ner.smartlogix.service.AlertService;
import com.ner.smartlogix.service.NotificationService;
import com.ner.smartlogix.util.GeometryUtils;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns events into alerts, then hands each alert to the notification service and the
 * WebSocket broadcaster.
 *
 * <p>Every {@code raise*} method funnels into {@link #persistAndDispatch}, so there is
 * exactly one place where an alert is saved, fanned out to users and pushed to browsers.
 * That is why adding SMS later means adding one channel class, not editing eight methods.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final DistrictRepository districtRepository;
    private final RoadRepository roadRepository;
    private final NotificationService notificationService;
    private final AlertMapper alertMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AlertResponse> search(Boolean active, Severity severity, Long districtId,
                                      Pageable pageable) {
        Specification<Alert> spec = (root, query, cb) -> cb.conjunction();
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (severity != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        if (districtId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("district").get("id"), districtId));
        }
        return alertRepository.findAll(spec, pageable).map(alertMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertResponse> recent(int limit) {
        return alertRepository
                .findByActiveTrueOrderByCreatedAtDesc(PageRequest.of(0, Math.min(limit, 50)))
                .map(alertMapper::toResponse)
                .getContent();
    }

    @Override
    @Transactional
    public AlertResponse createManual(AlertRequest request) {
        Alert alert = new Alert();
        alert.setAlertType(request.alertType());
        alert.setSeverity(request.severity());
        alert.setTitle(request.title());
        alert.setMessage(request.message());
        if (request.districtCode() != null) {
            districtRepository.findByCode(request.districtCode()).ifPresent(alert::setDistrict);
        }
        if (request.roadCode() != null) {
            roadRepository.findByCode(request.roadCode()).ifPresent(alert::setRoad);
        }
        if (request.expiresInHours() != null) {
            alert.setExpiresAt(OffsetDateTime.now().plusHours(request.expiresInHours()));
        }
        return persistAndDispatch(alert);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert", "id", id));
        alert.setActive(false);
        alertRepository.save(alert);
    }

    // ---------------------------------------------------------------- system alerts

    @Override
    @Transactional
    public AlertResponse raiseIncidentAlert(Incident incident) {
        AlertType type = switch (incident.getIncidentType()) {
            case LANDSLIDE -> AlertType.LANDSLIDE_RISK;
            case FLOOD -> AlertType.FLOOD_RISK;
            default -> AlertType.ROAD_BLOCKED;
        };

        Alert alert = new Alert();
        alert.setAlertType(type);
        alert.setSeverity(incident.getSeverity());
        alert.setTitle("%s reported%s".formatted(
                readable(incident.getIncidentType()),
                incident.getRoad() == null ? "" : " on " + incident.getRoad().getName()));
        alert.setMessage(incident.getDescription() == null
                ? "A %s of %s severity was reported.".formatted(
                        readable(incident.getIncidentType()), incident.getSeverity())
                : incident.getDescription());
        alert.setIncident(incident);
        alert.setRoad(incident.getRoad());
        alert.setDistrict(incident.getDistrict());
        alert.setLocation(GeometryUtils.point(incident.getLatitude(), incident.getLongitude()));
        alert.setExpiresAt(OffsetDateTime.now().plusDays(3));
        return persistAndDispatch(alert);
    }

    @Override
    @Transactional
    public AlertResponse raiseRoadStatusAlert(Road road, String reason) {
        Alert alert = new Alert();
        alert.setAlertType(road.getStatus() == RoadStatus.BLOCKED
                ? AlertType.ROAD_BLOCKED : AlertType.HIGH_RISK_ROUTE);
        alert.setSeverity(road.getStatus() == RoadStatus.BLOCKED
                ? Severity.CRITICAL : Severity.HIGH);
        alert.setTitle("%s is now %s".formatted(road.getName(), road.getStatus()));
        alert.setMessage(reason == null || reason.isBlank()
                ? "Status changed to %s.".formatted(road.getStatus()) : reason);
        alert.setRoad(road);
        alert.setDistrict(road.getDistrict());
        return persistAndDispatch(alert);
    }

    @Override
    @Transactional
    public AlertResponse raiseRiskAlert(Road road, RiskLevel newLevel, String explanation) {
        Alert alert = new Alert();
        alert.setAlertType(AlertType.HIGH_RISK_ROUTE);
        alert.setSeverity(newLevel == RiskLevel.HIGH_RISK ? Severity.HIGH : Severity.MEDIUM);
        alert.setTitle("Disruption risk on %s is now %s".formatted(road.getName(), newLevel));
        alert.setMessage(explanation);
        alert.setRoad(road);
        alert.setDistrict(road.getDistrict());
        alert.setExpiresAt(OffsetDateTime.now().plusHours(12));
        return persistAndDispatch(alert);
    }

    @Override
    @Transactional
    public AlertResponse raiseWeatherAlert(District district, WeatherData weather) {
        Alert alert = new Alert();
        alert.setAlertType(AlertType.HEAVY_RAINFALL);
        alert.setSeverity(weather.getRainfallMm24h() >= 150 ? Severity.CRITICAL : Severity.HIGH);
        alert.setTitle("Heavy rainfall in %s".formatted(district.getName()));
        alert.setMessage("%.0f mm in the last 24 hours and %.0f mm over 72 hours. Expect landslides and flooding on hill roads."
                .formatted(weather.getRainfallMm24h(), weather.getRainfallMm72h()));
        alert.setDistrict(district);
        alert.setExpiresAt(OffsetDateTime.now().plusHours(24));
        return persistAndDispatch(alert);
    }

    @Override
    @Transactional
    public AlertResponse raiseDeliveryDelayAlert(Delivery delivery, int delayMinutes,
                                                 String reason) {
        Alert alert = new Alert();
        alert.setAlertType(AlertType.DELIVERY_DELAYED);
        alert.setSeverity(delayMinutes > 240 ? Severity.HIGH : Severity.MEDIUM);
        alert.setTitle("Delivery %s delayed by %d minutes"
                .formatted(delivery.getTrackingCode(), delayMinutes));
        alert.setMessage(reason);
        alert.setDelivery(delivery);
        alert.setDistrict(delivery.getDestinationDistrict());
        alert.setExpiresAt(OffsetDateTime.now().plusDays(1));
        return persistAndDispatch(alert);
    }

    @Override
    @Transactional
    public AlertResponse raiseDangerZoneAlert(Vehicle vehicle, Road road) {
        // One warning per vehicle and road is enough; repeating it every ten seconds
        // while the truck sits there would bury every other alert on the dashboard.
        boolean alreadyWarned = alertRepository
                .findByActiveTrueAndAlertTypeAndSeverity(AlertType.DANGER_ZONE_ENTRY,
                        Severity.HIGH)
                .stream()
                .anyMatch(existing -> existing.getRoad() != null
                        && existing.getRoad().getId().equals(road.getId())
                        && existing.getMessage().contains(vehicle.getVehicleNumber()));
        if (alreadyWarned) {
            return null;
        }

        Alert alert = new Alert();
        alert.setAlertType(AlertType.DANGER_ZONE_ENTRY);
        alert.setSeverity(Severity.HIGH);
        alert.setTitle("Vehicle %s entered a dangerous area".formatted(vehicle.getVehicleNumber()));
        alert.setMessage("Vehicle %s is on %s, which is currently %s."
                .formatted(vehicle.getVehicleNumber(), road.getName(), road.getStatus()));
        alert.setRoad(road);
        alert.setDistrict(road.getDistrict());
        alert.setLocation(vehicle.getLastLatitude() == null ? null
                : GeometryUtils.point(vehicle.getLastLatitude(), vehicle.getLastLongitude()));
        alert.setExpiresAt(OffsetDateTime.now().plusHours(6));
        return persistAndDispatch(alert);
    }

    @Override
    @Transactional
    public AlertResponse raiseDistrictInaccessibleAlert(District district) {
        Alert alert = new Alert();
        alert.setAlertType(AlertType.DISTRICT_INACCESSIBLE);
        alert.setSeverity(Severity.CRITICAL);
        alert.setTitle("%s is cut off".formatted(district.getName()));
        alert.setMessage("Every monitored road into %s, %s is currently blocked."
                .formatted(district.getName(), district.getState()));
        alert.setDistrict(district);
        return persistAndDispatch(alert);
    }

    /** Save, notify the right people, and let the notification service push to browsers. */
    private AlertResponse persistAndDispatch(Alert alert) {
        Alert saved = alertRepository.save(alert);
        log.info("Alert raised: [{}] {}", saved.getSeverity(), saved.getTitle());
        notificationService.dispatch(saved);
        return alertMapper.toResponse(saved);
    }

    private String readable(IncidentType type) {
        return type.name().charAt(0) + type.name().substring(1).toLowerCase().replace('_', ' ');
    }
}
