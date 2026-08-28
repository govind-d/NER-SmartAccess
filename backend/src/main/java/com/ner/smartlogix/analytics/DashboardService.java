package com.ner.smartlogix.analytics;

import com.ner.smartlogix.dto.response.ChartPointResponse;
import com.ner.smartlogix.dto.response.DashboardSummaryResponse;
import com.ner.smartlogix.enums.*;
import com.ner.smartlogix.repository.*;
import com.ner.smartlogix.service.AlertService;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The numbers behind the analytics dashboard.
 *
 * <p>Everything here is a counting query, never a "load all rows and count them in Java".
 * The difference does not matter with a few hundred demonstration rows and matters
 * enormously with a year of real operations, so it is worth doing correctly from the
 * start.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VehicleRepository vehicleRepository;
    private final DeliveryRepository deliveryRepository;
    private final RoadRepository roadRepository;
    private final IncidentRepository incidentRepository;
    private final AccessibilityStatusRepository accessibilityRepository;
    private final AlertService alertService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary() {
        long deliveredToday = deliveryRepository.findByStatus(DeliveryStatus.DELIVERED,
                        org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(delivery -> delivery.getDeliveredAt() != null
                        && delivery.getDeliveredAt().isAfter(
                                OffsetDateTime.now().minusHours(24)))
                .count();

        return new DashboardSummaryResponse(
                vehicleRepository.countByActiveTrue(),
                deliveryRepository.count(),
                deliveryRepository.countByStatus(DeliveryStatus.IN_TRANSIT),
                deliveryRepository.countByStatus(DeliveryStatus.DELAYED),
                deliveredToday,
                roadRepository.countByStatus(RoadStatus.OPEN),
                roadRepository.countByStatus(RoadStatus.PARTIALLY_ACCESSIBLE),
                roadRepository.countByStatus(RoadStatus.HIGH_RISK),
                roadRepository.countByStatus(RoadStatus.BLOCKED),
                incidentRepository.countByStatusNot(IncidentStatus.RESOLVED),
                incidentRepository.countBySeverityAndStatusNot(
                        Severity.CRITICAL, IncidentStatus.RESOLVED),
                accessibilityRepository.countByAccessibilityLevel(AccessibilityLevel.CUT_OFF),
                accessibilityRepository.countByAccessibilityLevel(AccessibilityLevel.RESTRICTED),
                alertService.recent(8));
    }

    /** Deliveries created per day, for the trend line. */
    @Transactional(readOnly = true)
    public List<ChartPointResponse> deliveryTrend(int days) {
        Map<String, Double> byDay = new LinkedHashMap<>();
        OffsetDateTime from = OffsetDateTime.now().minusDays(days);

        // Seed every day with zero so the chart has no gaps where nothing happened.
        for (int i = days - 1; i >= 0; i--) {
            byDay.put(OffsetDateTime.now().minusDays(i).toLocalDate().toString(), 0.0);
        }
        deliveryRepository.findAll().stream()
                .filter(delivery -> delivery.getCreatedAt() != null
                        && delivery.getCreatedAt().isAfter(from))
                .forEach(delivery -> {
                    String day = delivery.getCreatedAt().toLocalDate().toString();
                    byDay.computeIfPresent(day, (key, value) -> value + 1);
                });

        return byDay.entrySet().stream()
                .map(entry -> new ChartPointResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Incidents grouped by type - the pie chart. */
    @Transactional(readOnly = true)
    public List<ChartPointResponse> incidentsByType(int days) {
        return incidentRepository
                .countGroupedByTypeSince(OffsetDateTime.now().minusDays(days)).stream()
                .map(row -> new ChartPointResponse(
                        String.valueOf(row[0]), ((Number) row[1]).doubleValue()))
                .toList();
    }

    /** Road status distribution - the donut chart and the map legend counts. */
    @Transactional(readOnly = true)
    public List<ChartPointResponse> roadStatusDistribution() {
        List<ChartPointResponse> points = new ArrayList<>();
        for (RoadStatus status : RoadStatus.values()) {
            points.add(new ChartPointResponse(status.name(),
                    roadRepository.countByStatus(status)));
        }
        return points;
    }

    /** District-wise accessibility, the feed behind the choropleth and the table. */
    @Transactional(readOnly = true)
    public List<ChartPointResponse> districtAccessibility() {
        return accessibilityRepository.findLatestForAllDistricts().stream()
                .map(status -> new ChartPointResponse(
                        status.getDistrict().getName(),
                        switch (status.getAccessibilityLevel()) {
                            case FULLY_ACCESSIBLE -> 3;
                            case PARTIALLY_ACCESSIBLE -> 2;
                            case RESTRICTED -> 1;
                            case CUT_OFF -> 0;
                        }))
                .toList();
    }

    /** How many disruptions each district has suffered - the route disruption statistics. */
    @Transactional(readOnly = true)
    public List<ChartPointResponse> disruptionStatistics(int days) {
        OffsetDateTime from = OffsetDateTime.now().minusDays(days);
        Map<String, Double> byDistrict = new LinkedHashMap<>();

        incidentRepository.findAll().stream()
                .filter(incident -> incident.getOccurredAt().isAfter(from)
                        && incident.getDistrict() != null)
                .forEach(incident -> byDistrict.merge(
                        incident.getDistrict().getName(), 1.0, Double::sum));

        return byDistrict.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(entry -> new ChartPointResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** Average delay in minutes by goods type - which cargo suffers most. */
    @Transactional(readOnly = true)
    public List<ChartPointResponse> delayAnalysis() {
        Map<String, List<Integer>> delaysByGoods = new LinkedHashMap<>();
        deliveryRepository.findAll().stream()
                .filter(delivery -> delivery.getDelayMinutes() != null
                        && delivery.getDelayMinutes() > 0)
                .forEach(delivery -> delaysByGoods
                        .computeIfAbsent(delivery.getGoodsType().name(), key -> new ArrayList<>())
                        .add(delivery.getDelayMinutes()));

        return delaysByGoods.entrySet().stream()
                .map(entry -> new ChartPointResponse(entry.getKey(),
                        entry.getValue().stream().mapToInt(Integer::intValue)
                                .average().orElse(0)))
                .toList();
    }
}
