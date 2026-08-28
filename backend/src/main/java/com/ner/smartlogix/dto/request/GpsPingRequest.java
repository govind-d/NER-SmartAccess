package com.ner.smartlogix.dto.request;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

/** One position report from a vehicle, sent by REST or over STOMP. */
public record GpsPingRequest(
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @PositiveOrZero @DecimalMax("200.0") Double speedKmph,
        @DecimalMin("0.0") @DecimalMax("360.0") Double heading,
        OffsetDateTime recordedAt) {
}
