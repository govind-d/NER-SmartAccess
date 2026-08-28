package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.VehicleType;
import jakarta.validation.constraints.*;

public record VehicleRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z]{1,3}[0-9]{4}$",
                 message = "Vehicle number must look like AS01AB1234")
        String vehicleNumber,
        @NotNull VehicleType vehicleType,
        @Positive Double capacityTons,
        Long driverId) {
}
