package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.RoadStatus;
import java.time.OffsetDateTime;

/** Payload broadcast on /topic/roads/status so the map can recolour a line instantly. */
public record RoadStatusChangeResponse(
        Long roadId,
        String roadCode,
        String roadName,
        RoadStatus previousStatus,
        RoadStatus newStatus,
        String reason,
        OffsetDateTime changedAt) {
}
