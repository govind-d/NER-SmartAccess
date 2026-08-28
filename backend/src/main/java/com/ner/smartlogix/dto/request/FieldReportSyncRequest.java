package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.IncidentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A batch of reports queued on a phone while it had no network.
 *
 * <p>The batch is processed item by item and every item gets its own outcome, so one bad
 * report never causes the other nine to be lost.
 */
public record FieldReportSyncRequest(
        @Valid @NotEmpty @Size(max = 100) List<Item> reports) {

    public record Item(
            @NotNull UUID clientUuid,
            @NotNull IncidentType reportType,
            @Size(max = 2000) String description,
            @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
            @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
            @NotNull @PastOrPresent OffsetDateTime capturedAt) {
    }
}
