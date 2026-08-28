package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.request.RouteRecommendationRequest;
import com.ner.smartlogix.dto.response.RouteRecommendationResponse;
import com.ner.smartlogix.dto.response.RouteResponse;

public interface RouteService {

    /** The main entry point: candidates from a routing engine, ranked by our risk model. */
    RouteRecommendationResponse recommend(RouteRecommendationRequest request);

    RouteResponse findById(Long routeId);

    /** Re-scores a stored route against current conditions. */
    RouteResponse recalculate(Long routeId);
}
