package com.ner.smartlogix.dto.response;

import com.ner.smartlogix.enums.VehicleType;
import java.time.OffsetDateTime;

public record VehicleResponse(
        Long id,
        String vehicleNumber,
        VehicleType vehicleType,
        Double capacityTons,
        Long driverId,
        String driverName,
        String driverPhone,
        boolean active,
        Double lastLatitude,
        Double lastLongitude,
        OffsetDateTime lastSeenAt) {
}
