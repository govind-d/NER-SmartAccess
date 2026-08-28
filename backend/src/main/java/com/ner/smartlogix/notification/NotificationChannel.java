package com.ner.smartlogix.notification;

import com.ner.smartlogix.entity.Alert;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.AlertChannel;

/**
 * One way of reaching a user.
 *
 * <p>This interface is the reason the notification system can grow without being
 * rewritten. WebSocket is implemented now; email and SMS are stubs that log instead of
 * sending. Making them real means filling in one method - no other file changes.
 */
public interface NotificationChannel {

    AlertChannel channel();

    /** True when this channel is switched on in configuration. */
    boolean isEnabled();

    /**
     * Delivers the alert. Implementations must not throw: a failed SMS must never
     * prevent the WebSocket push or roll back the transaction that raised the alert.
     */
    void send(User recipient, Alert alert);
}
