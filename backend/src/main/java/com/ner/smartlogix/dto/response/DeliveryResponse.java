package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.DeliveryStatus;
import com.ner.smartlogix.enums.GoodsType;
import java.time.OffsetDateTime;

public record DeliveryResponse(
        Long id,
        String trackingCode,
        Long vehicleId,
        String vehicleNumber,
        String driverName,
        Long routeId,
        String sourceDistrictCode,
        String sourceDistrictName,
        String destinationDistrictCode,
        String destinationDistrictName,
        String sourceLabel,
        String destinationLabel,
        GoodsType goodsType,
        Double weightTons,
        DeliveryStatus status,
        OffsetDateTime dispatchedAt,
        OffsetDateTime eta,
        OffsetDateTime deliveredAt,
        Integer delayMinutes) {
}
