package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.RoadCondition;
import com.ner.smartlogix.enums.RoadType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Creates or replaces a road.
 *
 * <p>{@code lengthKm} is deliberately absent: the server asks PostGIS to measure the
 * geometry, so the stored length can never contradict the line on the map.
 */
public record RoadRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull RoadType roadType,
        @NotBlank String districtCode,
        @Valid @NotNull @Size(min = 2, message = "A road needs at least two points")
        List<CoordinateRequest> path,
        @NotNull RoadCondition condition,
        @PositiveOrZero @DecimalMax("90.0") Double slopeDegrees,
        @DecimalMin("0.0") @DecimalMax("1.0") Double landslideSusceptibility,
        boolean floodProne,
        @PositiveOrZero Double historicalBlockDaysPerYear) {
}
