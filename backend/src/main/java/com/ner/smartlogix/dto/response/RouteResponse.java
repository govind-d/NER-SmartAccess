package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.RiskLevel;
import java.util.List;

/** One scored route, with the geometry Leaflet needs and the reasons a human needs. */
public record RouteResponse(
        Long routeId,
        double distanceKm,
        int estimatedDurationMin,
        double riskScore,
        RiskLevel riskLevel,
        String provider,
        List<double[]> path,
        List<SegmentResponse> segments,
        List<String> warnings) {

    public record SegmentResponse(int sequenceNo, String roadCode, String roadName,
                                  double distanceKm, int durationMin, RiskLevel riskLevel,
                                  String roadStatus) {
    }
}
