package com.ner.smartlogix.enums;

/**
 * Delivery lifecycle. The legal transitions are enforced in the service layer
 * (Phase 6): CREATED -> IN_TRANSIT -> (DELAYED &lt;-&gt; IN_TRANSIT) -> DELIVERED.
 */
public enum DeliveryStatus {
    CREATED,
    IN_TRANSIT,
    DELAYED,
    DELIVERED
}
