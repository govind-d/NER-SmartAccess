package com.ner.smartlogix.notification;

import com.ner.smartlogix.entity.Alert;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.AlertChannel;
import com.ner.smartlogix.enums.Severity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Placeholder channel for SMS, which matters more than email in the NER: a field officer
 * in a valley often has a weak 2G signal that carries a text message but not a web app.
 * Only CRITICAL alerts would be sent, because SMS costs money per message.
 */
@Slf4j
@Component
public class SmsNotificationChannel implements NotificationChannel {

    @Value("${app.notifications.sms.enabled:false}")
    private boolean enabled;

    @Override
    public AlertChannel channel() {
        return AlertChannel.SMS;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void send(User recipient, Alert alert) {
        if (alert.getSeverity() != Severity.CRITICAL) {
            return;
        }
        log.info("[SMS STUB] would text '{}' to {}", alert.getTitle(), recipient.getPhone());
    }
}
