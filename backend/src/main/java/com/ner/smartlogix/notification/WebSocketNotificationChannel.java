package com.ner.smartlogix.notification;

import com.ner.smartlogix.entity.Alert;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.AlertChannel;
import com.ner.smartlogix.mapper.AlertMapper;
import com.ner.smartlogix.websocket.RealtimeBroadcaster;
import com.ner.smartlogix.websocket.WsTopics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The only channel that actually delivers today: a private STOMP message per user. */
@Component
@RequiredArgsConstructor
public class WebSocketNotificationChannel implements NotificationChannel {

    private final RealtimeBroadcaster broadcaster;
    private final AlertMapper alertMapper;

    @Override
    public AlertChannel channel() {
        return AlertChannel.WEBSOCKET;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void send(User recipient, Alert alert) {
        broadcaster.sendToUser(recipient.getUsername(), WsTopics.USER_NOTIFICATIONS,
                alertMapper.toResponse(alert));
    }
}
