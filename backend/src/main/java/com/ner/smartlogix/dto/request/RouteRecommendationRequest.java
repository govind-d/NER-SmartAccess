package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.GoodsType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Ask the platform for the best way from A to B right now.
 *
 * <p>{@code avoidHighRisk} is a policy switch, not a filter on geometry: a relief convoy
 * carrying medicine may well accept a longer, safer road, while a routine consignment
 * may not care.
 */
public record RouteRecommendationRequest(
        @Valid @NotNull CoordinateRequest origin,
        @Valid @NotNull CoordinateRequest destination,
        GoodsType goodsType,
        boolean avoidHighRisk,
        List<Long> avoidRoadIds,
        OffsetDateTime departureTime) {
}
