package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.IncidentType;
import com.ner.smartlogix.enums.SyncStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record FieldReportResponse(
        Long id,
        UUID clientUuid,
        IncidentType reportType,
        String description,
        Double latitude,
        Double longitude,
        String reportedByName,
        SyncStatus syncStatus,
        OffsetDateTime capturedAt,
        OffsetDateTime syncedAt,
        Long incidentId) {
}
