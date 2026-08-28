package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.request.RouteRecommendationRequest;
import com.ner.smartlogix.dto.response.RouteRecommendationResponse;
import com.ner.smartlogix.dto.response.RouteResponse;
import com.ner.smartlogix.entity.Road;
import com.ner.smartlogix.entity.Route;
import com.ner.smartlogix.entity.RouteSegment;
import com.ner.smartlogix.enums.RoadStatus;
import com.ner.smartlogix.exception.BusinessRuleException;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.repository.RoadRepository;
import com.ner.smartlogix.repository.RouteRepository;
import com.ner.smartlogix.routing.RouteCandidate;
import com.ner.smartlogix.routing.RouteRiskScorer;
import com.ner.smartlogix.routing.RoutingProvider;
import com.ner.smartlogix.service.RouteService;
import com.ner.smartlogix.util.GeometryUtils;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.LineString;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Smart route recommendation: the piece that ties routing, GIS and the AI engine together.
 *
 * <p>The sequence is:
 * <ol>
 *   <li>ask a {@link RoutingProvider} for up to three candidate routes;</li>
 *   <li>for each candidate, ask PostGIS which of our monitored roads it passes through
 *       ({@code ST_DWithin} against the route's own geometry);</li>
 *   <li>score every candidate with {@link RouteRiskScorer};</li>
 *   <li>discard anything crossing a BLOCKED road - not penalise it, discard it;</li>
 *   <li>return the best, the alternates, and the reasons for every rejection.</li>
 * </ol>
 *
 * <p>Step 4 is a hard filter for a reason. A blocked road is not "expensive", it is
 * impassable, and a scoring system that merely penalises it will eventually recommend it
 * when every other option looks worse.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    /** How far from the route line a monitored road still counts as being on it. */
    private static final double ROUTE_MATCH_TOLERANCE_METERS = 250;
    private static final int CANDIDATES_REQUESTED = 3;

    private final List<RoutingProvider> routingProviders;
    private final RouteRiskScorer riskScorer;
    private final RoadRepository roadRepository;
    private final RouteRepository routeRepository;

    @Override
    @Transactional
    public RouteRecommendationResponse recommend(RouteRecommendationRequest request) {
        double fromLat = request.origin().latitude();
        double fromLon = request.origin().longitude();
        double toLat = request.destination().latitude();
        double toLon = request.destination().longitude();

        List<RouteCandidate> candidates = fetchCandidates(fromLat, fromLon, toLat, toLon);
        if (candidates.isEmpty()) {
            throw new BusinessRuleException(
                    "No route could be computed between these two points");
        }

        int fastest = candidates.stream()
                .mapToInt(RouteCandidate::durationMin).min().orElse(1);

        List<RouteRiskScorer.ScoredRoute> scored = new ArrayList<>();
        List<RouteRecommendationResponse.Rejection> rejections = new ArrayList<>();

        for (RouteCandidate candidate : candidates) {
            List<Road> roadsUsed = matchRoads(candidate);

            // Explicit user exclusions, e.g. "not the road I just came down".
            if (request.avoidRoadIds() != null && roadsUsed.stream()
                    .anyMatch(road -> request.avoidRoadIds().contains(road.getId()))) {
                rejections.add(new RouteRecommendationResponse.Rejection(
                        "EXCLUDED_BY_REQUEST", List.of("Uses a road the caller asked to avoid")));
                continue;
            }

            RouteRiskScorer.ScoredRoute result = riskScorer.score(candidate, roadsUsed, fastest);

            if (result.blocked()) {
                rejections.add(new RouteRecommendationResponse.Rejection(
                        "BLOCKED_ROAD", result.warnings()));
                continue;
            }
            if (request.avoidHighRisk() && hasHighRiskRoad(roadsUsed)) {
                rejections.add(new RouteRecommendationResponse.Rejection(
                        "HIGH_RISK_ROAD_AVOIDED", result.warnings()));
                continue;
            }
            scored.add(result);
        }

        if (scored.isEmpty()) {
            // Everything was filtered out. Rather than answering "no route", fall back to
            // the least bad option and flag it loudly: a relief convoy needs an answer.
            log.warn("Every candidate route was rejected; returning the least risky one");
            List<RouteRiskScorer.ScoredRoute> fallback = candidates.stream()
                    .map(candidate -> riskScorer.score(candidate, matchRoads(candidate), fastest))
                    .sorted(Comparator.comparingDouble(RouteRiskScorer.ScoredRoute::score))
                    .toList();
            if (fallback.isEmpty()) {
                throw new BusinessRuleException("No usable route is available right now");
            }
            RouteResponse only = persistAndConvert(fallback.get(0), false, null);
            return new RouteRecommendationResponse(only, List.of(), explanation(rejections));
        }

        scored.sort(Comparator.comparingDouble(RouteRiskScorer.ScoredRoute::score));

        RouteResponse best = persistAndConvert(scored.get(0), false, null);
        List<RouteResponse> alternates = new ArrayList<>();
        for (int i = 1; i < scored.size(); i++) {
            alternates.add(persistAndConvert(scored.get(i), true, best.routeId()));
        }

        log.info("Recommended a {} km route scoring {} with {} alternate(s)",
                best.distanceKm(), best.riskScore(), alternates.size());
        return new RouteRecommendationResponse(best, alternates, explanation(rejections));
    }

    @Override
    @Transactional(readOnly = true)
    public RouteResponse findById(Long routeId) {
        Route route = routeRepository.findWithSegmentsById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));
        return toResponse(route);
    }

    @Override
    @Transactional
    public RouteResponse recalculate(Long routeId) {
        Route route = routeRepository.findWithSegmentsById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", "id", routeId));

        List<Road> roads = route.getSegments().stream()
                .map(RouteSegment::getRoad)
                .filter(java.util.Objects::nonNull)
                .toList();

        RouteCandidate candidate = new RouteCandidate(route.getTotalDistanceKm(),
                route.getEstimatedDurationMin(), List.of(), route.getProvider());
        RouteRiskScorer.ScoredRoute rescored =
                riskScorer.score(candidate, roads, route.getEstimatedDurationMin());

        route.setRiskScore(rescored.score());
        route.setRiskLevel(rescored.riskLevel());
        route.setComputedAt(OffsetDateTime.now());
        return toResponse(routeRepository.save(route));
    }

    // ------------------------------------------------------------------ internals

    /** Tries each provider in priority order and takes the first that answers. */
    private List<RouteCandidate> fetchCandidates(double fromLat, double fromLon,
                                                 double toLat, double toLon) {
        for (RoutingProvider provider : routingProviders) {
            if (!provider.isAvailable()) {
                continue;
            }
            List<RouteCandidate> candidates = provider.findRoutes(
                    fromLat, fromLon, toLat, toLon, CANDIDATES_REQUESTED);
            if (!candidates.isEmpty()) {
                log.debug("{} returned {} candidate route(s)",
                        provider.providerName(), candidates.size());
                return candidates;
            }
        }
        return List.of();
    }

    /**
     * Which monitored roads does this candidate run along? PostGIS answers by comparing
     * the route line with every road geometry, using the GiST index.
     */
    private List<Road> matchRoads(RouteCandidate candidate) {
        if (candidate.path().size() < 2) {
            return List.of();
        }
        LineString line = GeometryUtils.lineString(candidate.path());
        return roadRepository.findAlongRoute(GeometryUtils.toWkt(line),
                ROUTE_MATCH_TOLERANCE_METERS);
    }

    private boolean hasHighRiskRoad(List<Road> roads) {
        return roads.stream().anyMatch(road -> road.getStatus() == RoadStatus.HIGH_RISK
                || road.getCurrentRiskLevel() == com.ner.smartlogix.enums.RiskLevel.HIGH_RISK);
    }

    /** Stores the route with its segments so a delivery can be attached to it later. */
    private RouteResponse persistAndConvert(RouteRiskScorer.ScoredRoute scored,
                                            boolean isAlternate, Long parentRouteId) {
        RouteCandidate candidate = scored.candidate();

        Route route = new Route();
        route.setName("%.1f km via %s".formatted(candidate.distanceKm(), candidate.provider()));
        route.setTotalDistanceKm(candidate.distanceKm());
        route.setEstimatedDurationMin(candidate.durationMin());
        route.setRiskScore(scored.score());
        route.setRiskLevel(scored.riskLevel());
        route.setProvider(candidate.provider());
        route.setAlternate(isAlternate);
        route.setComputedAt(OffsetDateTime.now());
        if (candidate.path().size() >= 2) {
            route.setGeom(GeometryUtils.lineString(candidate.path()));
        }
        if (parentRouteId != null) {
            routeRepository.findById(parentRouteId).ifPresent(route::setParentRoute);
        }

        int sequence = 1;
        for (Road road : scored.roadsUsed()) {
            RouteSegment segment = new RouteSegment();
            segment.setRoad(road);
            segment.setSequenceNo(sequence++);
            segment.setDistanceKm(road.getLengthKm() == null ? 0 : road.getLengthKm());
            segment.setDurationMin((int) Math.round(
                    (road.getLengthKm() == null ? 0 : road.getLengthKm()) / 35.0 * 60));
            segment.setSegmentRiskScore(switch (road.getCurrentRiskLevel()) {
                case HIGH_RISK -> 100.0;
                case MEDIUM_RISK -> 50.0;
                case LOW_RISK -> 0.0;
            });
            route.addSegment(segment);
        }

        Route saved = routeRepository.save(route);
        return new RouteResponse(saved.getId(), saved.getTotalDistanceKm(),
                saved.getEstimatedDurationMin(), saved.getRiskScore(), saved.getRiskLevel(),
                saved.getProvider(), candidate.path(), segmentResponses(saved),
                scored.warnings());
    }

    private RouteResponse toResponse(Route route) {
        List<double[]> path = new ArrayList<>();
        if (route.getGeom() != null) {
            for (var coordinate : route.getGeom().getCoordinates()) {
                path.add(new double[]{coordinate.y, coordinate.x});
            }
        }
        return new RouteResponse(route.getId(), route.getTotalDistanceKm(),
                route.getEstimatedDurationMin(), route.getRiskScore(), route.getRiskLevel(),
                route.getProvider(), path, segmentResponses(route), List.of());
    }

    private List<RouteResponse.SegmentResponse> segmentResponses(Route route) {
        return route.getSegments().stream()
                .map(segment -> new RouteResponse.SegmentResponse(
                        segment.getSequenceNo(),
                        segment.getRoad() == null ? null : segment.getRoad().getCode(),
                        segment.getRoad() == null ? "unmonitored" : segment.getRoad().getName(),
                        segment.getDistanceKm(),
                        segment.getDurationMin(),
                        segment.getRoad() == null ? null : segment.getRoad().getCurrentRiskLevel(),
                        segment.getRoad() == null ? null : segment.getRoad().getStatus().name()))
                .toList();
    }

    private RouteRecommendationResponse.Explanation explanation(
            List<RouteRecommendationResponse.Rejection> rejections) {
        return new RouteRecommendationResponse.Explanation(
                "0.35*travelTime + 0.20*weather + 0.20*roadStatus "
                        + "+ 0.15*incidents + 0.10*predictedDisruption (lower is better)",
                "rule-engine-v1",
                rejections);
    }
}
