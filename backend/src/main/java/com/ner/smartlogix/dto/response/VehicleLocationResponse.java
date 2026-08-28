package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.DeliveryStatus;
import java.time.OffsetDateTime;

/**
 * A live position. This is also the payload broadcast on {@code /topic/vehicles}, which
 * is why it carries the delivery context: the map marker shows what the vehicle is
 * carrying without a second request.
 */
public record VehicleLocationResponse(
        Long vehicleId,
        String vehicleNumber,
        Double latitude,
        Double longitude,
        Double speedKmph,
        Double heading,
        Long deliveryId,
        DeliveryStatus deliveryStatus,
        OffsetDateTime recordedAt) {
}
