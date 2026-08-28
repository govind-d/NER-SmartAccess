package com.ner.smartlogix.dto.request;

import jakarta.validation.constraints.*;

/** A single latitude/longitude pair, validated to be on the planet. */
public record CoordinateRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
}
