package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.AlertType;
import com.ner.smartlogix.enums.Severity;
import java.time.OffsetDateTime;

public record AlertResponse(
        Long id,
        AlertType alertType,
        Severity severity,
        String title,
        String message,
        String districtCode,
        String districtName,
        String roadCode,
        Long incidentId,
        Long deliveryId,
        Double latitude,
        Double longitude,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime expiresAt) {
}
