package com.ner.smartlogix.dto.request;

import com.ner.smartlogix.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;

/** Moves a delivery through its lifecycle. Illegal transitions are refused with 422. */
public record DeliveryStatusRequest(@NotNull DeliveryStatus status) {
}
