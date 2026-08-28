package com.ner.smartlogix.notification;

import com.ner.smartlogix.entity.Alert;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.AlertChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Placeholder channel. It is registered and will be used the moment
 * {@code app.notifications.email.enabled=true}, but for now it only logs.
 *
 * <p>Wiring a real provider means adding spring-boot-starter-mail and replacing the body
 * of {@link #send}. Nothing else in the system needs to know.
 */
@Slf4j
@Component
public class EmailNotificationChannel implements NotificationChannel {

    @Value("${app.notifications.email.enabled:false}")
    private boolean enabled;

    @Override
    public AlertChannel channel() {
        return AlertChannel.EMAIL;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void send(User recipient, Alert alert) {
        log.info("[EMAIL STUB] would send '{}' to {}", alert.getTitle(), recipient.getEmail());
    }
}
