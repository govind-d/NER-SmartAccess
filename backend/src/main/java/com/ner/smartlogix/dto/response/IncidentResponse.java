package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.IncidentStatus;
import com.ner.smartlogix.enums.IncidentType;
import com.ner.smartlogix.enums.Severity;
import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentResponse(
        Long id,
        UUID clientUuid,
        IncidentType incidentType,
        Severity severity,
        String description,
        Double latitude,
        Double longitude,
        String roadCode,
        String roadName,
        String districtCode,
        String districtName,
        String reportedByName,
        String photoUrl,
        IncidentStatus status,
        OffsetDateTime occurredAt,
        OffsetDateTime reportedAt,
        OffsetDateTime verifiedAt) {
}
