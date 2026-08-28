package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.WeatherCondition;
import java.time.OffsetDateTime;

public record WeatherResponse(
        String districtCode,
        String districtName,
        Double temperatureC,
        Double rainfallMm24h,
        Double rainfallMm72h,
        Double humidity,
        Double windSpeedKmph,
        WeatherCondition condition,
        String source,
        OffsetDateTime recordedAt) {
}
