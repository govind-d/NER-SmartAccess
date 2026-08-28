package com.ner.smartlogix.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record DistrictRequest(
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 60) String state,
        @Valid @NotNull CoordinateRequest centroid,
        @PositiveOrZero Integer population,
        @PositiveOrZero Double areaSqKm) {
}
