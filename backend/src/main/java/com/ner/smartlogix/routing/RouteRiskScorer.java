package com.ner.smartlogix.routing;

import com.ner.smartlogix.config.RiskProperties;
import com.ner.smartlogix.entity.Road;
import com.ner.smartlogix.entity.WeatherData;
import com.ner.smartlogix.enums.RiskLevel;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.repository.IncidentRepository;
import com.ner.smartlogix.repository.WeatherDataRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Turns "this route is 103 km long" into "this route is a bad idea today".
 *
 * <p>This class is the platform's actual contribution to routing. OSRM finds paths;
 * this decides which of them a truck carrying medicines should take in the middle of a
 * monsoon, using the roads the platform monitors along each candidate.
 *
 * <pre>
 *   routeScore = w1*travelTime + w2*weather + w3*roadStatus
 *              + w4*recentIncidents + w5*predictedDisruption
 * </pre>
 *
 * <p>Lower is better. The weights live in application.yml, not here, so the balance
 * between speed and safety can be retuned without a recompile - and a relief operation
 * can legitimately weight safety far above speed.
 */
@Component
@RequiredArgsConstructor
public class RouteRiskScorer {

    private final RiskProperties properties;
    private final IncidentRepository incidentRepository;
    private final WeatherDataRepository weatherRepository;

    /** The verdict on one candidate route. */
    public record ScoredRoute(
            RouteCandidate candidate,
            double score,
            RiskLevel riskLevel,
            List<Road> roadsUsed,
            List<String> warnings,
            boolean blocked) {
    }

    /**
     * @param roadsAlongRoute the monitored roads this candidate passes through, already
     *                        matched by PostGIS
     * @param fastestMinutes  the quickest candidate under consideration, used to score
     *                        travel time relatively rather than in absolute minutes
     */
    public ScoredRoute score(RouteCandidate candidate, List<Road> roadsAlongRoute,
                             int fastestMinutes) {
        List<String> warnings = new ArrayList<>();
        boolean blocked = false;

        // ---- 1. Travel time, relative to the best option available -------------------
        // A route 50% slower than the fastest scores 50; twice as slow scores 100.
        double timeRatio = fastestMinutes <= 0 ? 1
                : (double) candidate.durationMin() / fastestMinutes;
        double travelTimeScore = Math.min(100, Math.max(0, (timeRatio - 1) * 100));

        // ---- 2. Road status ---------------------------------------------------------
        double roadScore = 0;
        for (Road road : roadsAlongRoute) {
            switch (road.getStatus()) {
                case BLOCKED -> {
                    blocked = true;
                    warnings.add("Passes %s, which is BLOCKED".formatted(road.getName()));
                    roadScore += 100;
                }
                case HIGH_RISK -> {
                    warnings.add("Passes %s, currently HIGH_RISK".formatted(road.getName()));
                    roadScore += 60;
                }
                case PARTIALLY_ACCESSIBLE -> roadScore += 30;
                case OPEN -> roadScore += 0;
            }
        }
        roadScore = roadsAlongRoute.isEmpty() ? 20   // unmonitored road: mild uncertainty
                : Math.min(100, roadScore / roadsAlongRoute.size());

        // ---- 3. Predicted disruption, from the AI engine -----------------------------
        double disruptionScore = 0;
        for (Road road : roadsAlongRoute) {
            disruptionScore += switch (road.getCurrentRiskLevel()) {
                case HIGH_RISK -> 100;
                case MEDIUM_RISK -> 50;
                case LOW_RISK -> 0;
            };
        }
        disruptionScore = roadsAlongRoute.isEmpty() ? 20
                : disruptionScore / roadsAlongRoute.size();

        // ---- 4. Weather along the route ---------------------------------------------
        double weatherScore = roadsAlongRoute.stream()
                .map(road -> weatherRepository.findFirstByDistrictIdOrderByRecordedAtDesc(
                        road.getDistrict().getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .mapToDouble(this::weatherPenalty)
                .average()
                .orElse(10);

        // ---- 5. Recent incidents ----------------------------------------------------
        OffsetDateTime since = OffsetDateTime.now().minusDays(7);
        double incidentScore = roadsAlongRoute.stream()
                .mapToDouble(road -> Math.min(100,
                        incidentRepository.countRecentByRoad(road.getId(), since) * 35))
                .average()
                .orElse(0);

        RiskProperties.RouteWeights weights = properties.getRoute();
        double score = weights.getTravelTime() * travelTimeScore
                + weights.getWeather() * weatherScore
                + weights.getRoad() * roadScore
                + weights.getIncident() * incidentScore
                + weights.getDisruption() * disruptionScore;

        return new ScoredRoute(candidate, round(score), toLevel(score),
                roadsAlongRoute, warnings, blocked);
    }

    private double weatherPenalty(WeatherData weather) {
        double rain = weather.getRainfallMm24h();
        if (rain >= 204.5) {
            return 100;
        }
        if (rain >= 115.5) {
            return 75;
        }
        if (rain >= 64.5) {
            return 50;
        }
        if (rain >= 15.6) {
            return 25;
        }
        return 5;
    }

    private RiskLevel toLevel(double score) {
        if (score >= properties.getThresholds().getHigh()) {
            return RiskLevel.HIGH_RISK;
        }
        if (score >= properties.getThresholds().getMedium()) {
            return RiskLevel.MEDIUM_RISK;
        }
        return RiskLevel.LOW_RISK;
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
