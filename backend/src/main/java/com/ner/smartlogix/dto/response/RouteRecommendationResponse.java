package com.ner.smartlogix.dto.response;

import java.util.List;

/**
 * The full answer: the route to take, the ones that came second, and - just as
 * importantly - the ones that were thrown out and why.
 *
 * <p>Showing rejected candidates is what turns a black box into a decision aid. An
 * official can see that the direct road was discarded because it is blocked, rather than
 * wondering why the system suggested a detour.
 */
public record RouteRecommendationResponse(
        RouteResponse recommended,
        List<RouteResponse> alternates,
        Explanation explanation) {

    public record Explanation(String formula, String modelName,
                              List<Rejection> rejectedCandidates) {
    }

    public record Rejection(String reason, List<String> details) {
    }
}
