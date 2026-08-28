package com.ner.smartlogix.ml.feature;

import com.ner.smartlogix.entity.Bridge;
import com.ner.smartlogix.entity.Road;
import com.ner.smartlogix.entity.WeatherData;
import com.ner.smartlogix.enums.RoadCondition;
import com.ner.smartlogix.enums.WeatherCondition;
import com.ner.smartlogix.repository.BridgeRepository;
import com.ner.smartlogix.repository.IncidentRepository;
import com.ner.smartlogix.repository.WeatherDataRepository;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Gathers everything the risk engine needs about one road, from four different tables.
 *
 * <p>This class is reused unchanged by the future machine-learning model. Feature
 * extraction is usually more than half the work in an ML system, and keeping it separate
 * from the predictor is what makes swapping the predictor cheap.
 */
@Component
@RequiredArgsConstructor
public class FeatureExtractor {

    private static final List<Month> MONSOON_MONTHS =
            List.of(Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER);
    private static final int NEVER = 999;

    private final WeatherDataRepository weatherRepository;
    private final IncidentRepository incidentRepository;
    private final BridgeRepository bridgeRepository;

    public RiskFeatureVector extract(Road road) {
        return extract(road, null);
    }

    /**
     * @param overrideWeather lets the API answer "what would the risk be if 200 mm fell
     *                        tomorrow?" - a what-if question, without touching the
     *                        stored observations
     */
    public RiskFeatureVector extract(Road road, WeatherData overrideWeather) {
        WeatherData weather = overrideWeather != null ? overrideWeather
                : weatherRepository
                        .findFirstByDistrictIdOrderByRecordedAtDesc(road.getDistrict().getId())
                        .orElse(null);

        OffsetDateTime thirtyDaysAgo = OffsetDateTime.now().minusDays(30);
        int recentIncidents = (int) incidentRepository
                .countRecentByRoad(road.getId(), thirtyDaysAgo);

        int daysSinceLast = incidentRepository
                .findFirstByRoadIdOrderByOccurredAtDesc(road.getId())
                .map(incident -> (int) java.time.Duration
                        .between(incident.getOccurredAt(), OffsetDateTime.now()).toDays())
                .orElse(NEVER);

        RoadCondition worstBridge = bridgeRepository.findByRoadId(road.getId()).stream()
                .map(Bridge::getCondition)
                .max(Comparator.comparingInt(Enum::ordinal))   // DAMAGED is the worst
                .orElse(null);

        return new RiskFeatureVector(
                road.getId(),
                weather == null ? 0 : weather.getRainfallMm24h(),
                weather == null ? 0 : weather.getRainfallMm72h(),
                weather == null ? WeatherCondition.CLEAR : weather.getCondition(),
                road.getSlopeDegrees() == null ? 0 : road.getSlopeDegrees(),
                road.getLandslideSusceptibility() == null ? 0 : road.getLandslideSusceptibility(),
                road.isFloodProne(),
                road.getCondition(),
                road.getStatus(),
                recentIncidents,
                daysSinceLast,
                road.getHistoricalBlockDaysPerYear() == null
                        ? 0 : road.getHistoricalBlockDaysPerYear(),
                estimateCongestion(road),
                MONSOON_MONTHS.contains(OffsetDateTime.now().getMonth()),
                worstBridge);
    }

    /**
     * Congestion is mocked until a real traffic feed exists. It is derived from road type
     * so the number is at least plausible, and it is isolated in one method so that
     * plugging in real data later changes nothing else.
     */
    private int estimateCongestion(Road road) {
        return switch (road.getRoadType()) {
            case NATIONAL_HIGHWAY -> 2;
            case STATE_HIGHWAY -> 2;
            case DISTRICT_ROAD -> 1;
            case RURAL_ROAD -> 0;
            case MOUNTAIN_PASS -> 3;
        };
    }
}
