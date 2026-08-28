package com.ner.smartlogix.enums;

/**
 * Delivery channel of a notification. Only WEBSOCKET is implemented now;
 * the others exist so the NotificationChannel interface can grow later.
 */
public enum AlertChannel {
    WEBSOCKET,
    EMAIL,
    SMS,
    PUSH
}
