package com.ner.smartlogix.routing;

import java.util.List;

/**
 * Something that can compute driving routes.
 *
 * <p>Deliberately narrow. Real road geometry and turn-by-turn shortest paths are a solved
 * problem, so the platform delegates them to OSRM or OpenRouteService and spends its own
 * effort on the part nobody else can do: scoring those routes against live NER conditions.
 *
 * <p>Three implementations exist - OSRM, an internal graph search over our own monitored
 * roads for when OSRM is unreachable, and a straight-line mock for offline development.
 */
public interface RoutingProvider {

    /**
     * @param alternatives how many distinct routes to ask for; providers may return fewer
     * @return best route first; an empty list when no route could be computed
     */
    List<RouteCandidate> findRoutes(double fromLat, double fromLon,
                                    double toLat, double toLon, int alternatives);

    String providerName();

    /** Lets the service fall back to another provider when this one is unavailable. */
    boolean isAvailable();
}
