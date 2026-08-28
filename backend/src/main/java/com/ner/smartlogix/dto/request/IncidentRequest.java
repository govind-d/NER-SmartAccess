package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.IncidentType;
import com.ner.smartlogix.enums.Severity;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A field report of a disruption.
 *
 * <p>{@code clientUuid} is generated on the device before the form is stored offline.
 * Re-sending the same UUID returns the existing incident instead of creating a second
 * one, which is what makes an unreliable network safe.
 *
 * <p>{@code occurredAt} is when it happened, which may be hours before the server sees it.
 */
public record IncidentRequest(
        UUID clientUuid,
        @NotNull IncidentType incidentType,
        @NotNull Severity severity,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @PastOrPresent OffsetDateTime occurredAt) {
}
