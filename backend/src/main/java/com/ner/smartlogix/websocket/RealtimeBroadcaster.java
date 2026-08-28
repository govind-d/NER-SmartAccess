package com.ner.smartlogix.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * The one place that pushes messages to browsers.
 *
 * <p>Services depend on this small class instead of on {@link SimpMessagingTemplate}
 * directly, which keeps the destination strings in one file and means a service can be
 * unit-tested by mocking a single method.
 *
 * <p>Every send is wrapped in a try/catch: a browser that disconnected mid-broadcast
 * must never roll back the database transaction that produced the message.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcast(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception ex) {
            log.warn("Could not broadcast to {}: {}", destination, ex.getMessage());
        }
    }

    /** Sends to one user; Spring rewrites the destination to /user/{name}/queue/... */
    public void sendToUser(String username, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(username, destination, payload);
        } catch (Exception ex) {
            log.warn("Could not send to user {}: {}", username, ex.getMessage());
        }
    }

    public void vehicleMoved(Long vehicleId, Object payload) {
        broadcast(WsTopics.VEHICLES, payload);
        broadcast(WsTopics.VEHICLE + vehicleId, payload);
    }

    public void incidentReported(Object payload) {
        broadcast(WsTopics.INCIDENTS, payload);
    }

    public void alertRaised(Object payload, String districtCode) {
        broadcast(WsTopics.ALERTS, payload);
        if (districtCode != null) {
            broadcast(WsTopics.ALERTS_DISTRICT + districtCode, payload);
        }
    }

    public void roadStatusChanged(Object payload) {
        broadcast(WsTopics.ROAD_STATUS, payload);
    }

    public void deliveryUpdated(Long deliveryId, Object payload) {
        broadcast(WsTopics.DELIVERY + deliveryId, payload);
    }
}
