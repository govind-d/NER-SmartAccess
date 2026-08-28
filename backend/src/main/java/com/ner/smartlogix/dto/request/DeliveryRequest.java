package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.GoodsType;
import jakarta.validation.constraints.*;

public record DeliveryRequest(
        Long vehicleId,
        @NotBlank String sourceDistrictCode,
        @NotBlank String destinationDistrictCode,
        @Size(max = 150) String sourceLabel,
        @Size(max = 150) String destinationLabel,
        @NotNull GoodsType goodsType,
        @Positive Double weightTons) {
}
