package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.RoadStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Manual status change by an official. The reason is stored on the resulting alert. */
public record RoadStatusRequest(
        @NotNull RoadStatus status,
        @Size(max = 500) String reason) {
}
