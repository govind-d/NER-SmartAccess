package com.ner.smartlogix.event;

/** Published when a delivery is marked DELAYED, whether by a person or by the
 *  overdue-detection job. */
public record DeliveryDelayedEvent(Long deliveryId, int delayMinutes, String reason) {
}
