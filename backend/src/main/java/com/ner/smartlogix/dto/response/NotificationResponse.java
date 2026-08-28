package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.AlertChannel;
import java.time.OffsetDateTime;

public record NotificationResponse(
        Long id,
        AlertChannel channel,
        boolean read,
        OffsetDateTime sentAt,
        AlertResponse alert) {
}
