package com.ner.smartlogix.routing;

import java.util.List;

/**
 * One possible way of getting from A to B, as returned by a routing engine and before
 * any risk analysis has been applied.
 *
 * @param path ordered [latitude, longitude] pairs describing the line on the map
 */
public record RouteCandidate(
        double distanceKm,
        int durationMin,
        List<double[]> path,
        String provider) {
}
