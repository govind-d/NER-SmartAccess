package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.WeatherCondition;
import jakarta.validation.constraints.*;

/**
 * An ad-hoc "what if" prediction.
 *
 * <p>Give it a road and optionally override the weather, and the engine answers as though
 * that weather were real. This is the endpoint to demonstrate in a viva: change the
 * rainfall from 20 to 200 and watch LOW_RISK become HIGH_RISK with an explanation.
 */
public record RiskPredictionRequest(
        @NotNull Long roadId,
        @PositiveOrZero @DecimalMax("1000.0") Double rainfall24h,
        @PositiveOrZero @DecimalMax("2000.0") Double rainfall72h,
        WeatherCondition weatherCondition,
        @Min(0) @Max(4) Integer trafficCongestionLevel) {
}
