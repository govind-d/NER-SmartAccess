package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.BridgeStatus;
import com.ner.smartlogix.enums.RoadCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record BridgeRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 150) String name,
        @NotBlank String roadCode,
        @Valid @NotNull CoordinateRequest location,
        @Positive Double loadCapacityTons,
        @NotNull RoadCondition condition,
        @NotNull BridgeStatus status,
        @PastOrPresent LocalDate lastInspectionDate) {
}
