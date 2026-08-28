package com.ner.smartlogix.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Routes from OSRM, the Open Source Routing Machine, over OpenStreetMap data.
 *
 * <p>This is the deliberate decision not to write a pathfinding algorithm. OSRM already
 * solves shortest paths over real road geometry, including one-way streets, turn
 * restrictions and road classes. Re-implementing that would take weeks and produce
 * something worse. The platform's contribution is what happens next: scoring each
 * returned route against live NER conditions.
 *
 * <p>The URL used is the public demo server by default. For a real deployment, run OSRM
 * locally from a North-East India extract - the compose file has a profile for it.
 */
@Slf4j
@Component
@Order(10)   // preferred provider when it is reachable
public class OsrmRoutingProvider implements RoutingProvider {

    private final RestClient restClient;
    private final boolean configured;

    public OsrmRoutingProvider(RestClient.Builder builder,
                               @Value("${app.routing.osrm-base-url:}") String baseUrl,
                               @Value("${app.routing.provider:osrm}") String provider) {
        this.configured = "osrm".equalsIgnoreCase(provider)
                && baseUrl != null && !baseUrl.isBlank();
        this.restClient = configured ? builder.baseUrl(baseUrl).build() : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RouteCandidate> findRoutes(double fromLat, double fromLon,
                                           double toLat, double toLon, int alternatives) {
        if (!configured) {
            return List.of();
        }
        try {
            // OSRM wants longitude first - the opposite of how coordinates are spoken.
            String coordinates = "%f,%f;%f,%f".formatted(fromLon, fromLat, toLon, toLat);

            Map<String, Object> body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/route/v1/driving/{coords}")
                            .queryParam("alternatives", alternatives > 1)
                            .queryParam("overview", "full")
                            .queryParam("geometries", "geojson")
                            .build(coordinates))
                    .retrieve()
                    .body(Map.class);

            if (body == null || !"Ok".equals(body.get("code"))) {
                log.debug("OSRM returned no usable route");
                return List.of();
            }

            List<Map<String, Object>> routes = (List<Map<String, Object>>) body.get("routes");
            if (routes == null) {
                return List.of();
            }

            List<RouteCandidate> candidates = new ArrayList<>();
            for (Map<String, Object> route : routes) {
                double distanceKm = toDouble(route.get("distance")) / 1000.0;
                int durationMin = (int) Math.round(toDouble(route.get("duration")) / 60.0);

                Map<String, Object> geometry = (Map<String, Object>) route.get("geometry");
                List<List<Number>> coords =
                        (List<List<Number>>) (geometry == null ? null : geometry.get("coordinates"));
                if (coords == null || coords.size() < 2) {
                    continue;
                }

                List<double[]> path = new ArrayList<>(coords.size());
                for (List<Number> pair : coords) {
                    // Back from GeoJSON [lon, lat] to our [lat, lon] convention.
                    path.add(new double[]{pair.get(1).doubleValue(), pair.get(0).doubleValue()});
                }
                candidates.add(new RouteCandidate(
                        Math.round(distanceKm * 100) / 100.0,
                        Math.max(1, durationMin), path, "OSRM"));
            }
            return candidates;

        } catch (Exception ex) {
            // A routing outage must degrade to the fallback provider, never fail the request.
            log.warn("OSRM request failed, falling back: {}", ex.getMessage());
            return List.of();
        }
    }

    @Override
    public String providerName() {
        return "OSRM";
    }

    @Override
    public boolean isAvailable() {
        return configured;
    }

    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }
}
