package com.ner.smartlogix.routing;

import com.ner.smartlogix.util.GeometryUtils;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * A straight line between the two points, plus two deliberately bent variants.
 *
 * <p>It exists so the entire routing and scoring pipeline can be developed and
 * demonstrated with no internet connection and no API key. The distances are honest
 * great-circle distances, and an average NER speed of 35 km/h reflects hill roads rather
 * than motorways, so the ETAs are plausible even though the geometry is not real.
 */
@Component
@Order(100)   // lowest priority: used only when nothing better is available
public class MockRoutingProvider implements RoutingProvider {

    private static final double AVERAGE_SPEED_KMPH = 35;

    @Override
    public List<RouteCandidate> findRoutes(double fromLat, double fromLon,
                                           double toLat, double toLon, int alternatives) {
        List<RouteCandidate> candidates = new ArrayList<>();
        double directKm = GeometryUtils.haversineKm(fromLat, fromLon, toLat, toLon);

        // Real roads are never straight; 1.35 is a reasonable detour factor for hills.
        candidates.add(build(fromLat, fromLon, toLat, toLon, directKm * 1.35, 0));

        for (int i = 1; i < Math.min(alternatives, 3); i++) {
            candidates.add(build(fromLat, fromLon, toLat, toLon,
                    directKm * (1.35 + 0.18 * i), 0.12 * i));
        }
        return candidates;
    }

    @Override
    public String providerName() {
        return "MOCK";
    }

    @Override
    public boolean isAvailable() {
        return true;   // the mock is always available; that is its purpose
    }

    /** @param bend how far the midpoint is pushed sideways, to make alternates distinct */
    private RouteCandidate build(double fromLat, double fromLon, double toLat, double toLon,
                                 double distanceKm, double bend) {
        double midLat = (fromLat + toLat) / 2 + bend * (toLon - fromLon);
        double midLon = (fromLon + toLon) / 2 - bend * (toLat - fromLat);

        List<double[]> path = List.of(
                new double[]{fromLat, fromLon},
                new double[]{midLat, midLon},
                new double[]{toLat, toLon});

        int durationMin = (int) Math.round(distanceKm / AVERAGE_SPEED_KMPH * 60);
        return new RouteCandidate(round(distanceKm), Math.max(1, durationMin), path, "MOCK");
    }

    private double round(double value) {
        return Math.round(value * 100) / 100.0;
    }
}
