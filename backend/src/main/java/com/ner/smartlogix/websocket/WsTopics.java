package com.ner.smartlogix.websocket;

/** Every STOMP destination in one place, so a typo cannot silently break a subscription. */
public final class WsTopics {

    public static final String VEHICLES = "/topic/vehicles";
    public static final String VEHICLE = "/topic/vehicles/";          // + vehicleId
    public static final String INCIDENTS = "/topic/incidents";
    public static final String ALERTS = "/topic/alerts";
    public static final String ALERTS_DISTRICT = "/topic/alerts/district/";  // + code
    public static final String ROAD_STATUS = "/topic/roads/status";
    public static final String DELIVERY = "/topic/deliveries/";       // + deliveryId
    public static final String USER_NOTIFICATIONS = "/queue/notifications";

    private WsTopics() {
    }
}
