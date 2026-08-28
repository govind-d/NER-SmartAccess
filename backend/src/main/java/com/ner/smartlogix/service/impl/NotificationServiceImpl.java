package com.ner.smartlogix.service.impl;

import com.ner.smartlogix.dto.response.NotificationResponse;
import com.ner.smartlogix.entity.Alert;
import com.ner.smartlogix.entity.Notification;
import com.ner.smartlogix.entity.User;
import com.ner.smartlogix.enums.*;
import com.ner.smartlogix.exception.ResourceNotFoundException;
import com.ner.smartlogix.mapper.AlertMapper;
import com.ner.smartlogix.notification.NotificationChannel;
import com.ner.smartlogix.repository.NotificationRepository;
import com.ner.smartlogix.repository.UserRepository;
import com.ner.smartlogix.security.SecurityUtils;
import com.ner.smartlogix.service.NotificationService;
import com.ner.smartlogix.websocket.RealtimeBroadcaster;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides WHO should hear about an alert, then delivers it through every enabled channel.
 *
 * <p>Spring injects a {@code List<NotificationChannel>} containing every implementation it
 * can find. Adding an SMS channel later therefore changes nothing in this class - that is
 * the open/closed principle doing real work rather than appearing in a textbook.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final List<NotificationChannel> channels;
    private final RealtimeBroadcaster broadcaster;
    private final AlertMapper alertMapper;

    @Override
    @Transactional
    public void dispatch(Alert alert) {
        if (alert == null) {
            return;
        }

        // 1. Everybody with an operational overview sees everything.
        Set<User> recipients = new LinkedHashSet<>();
        recipients.addAll(userRepository.findEnabledByRole(RoleName.ADMIN));
        recipients.addAll(userRepository.findEnabledByRole(RoleName.AUTHORITY_OFFICIAL));
        recipients.addAll(userRepository.findEnabledByRole(RoleName.LOGISTICS_MANAGER));

        // 2. People stationed in the affected district, whatever their role.
        if (alert.getDistrict() != null) {
            recipients.addAll(userRepository.findByDistrictId(alert.getDistrict().getId()));
        }

        // 3. The driver personally, when the alert is about their consignment or vehicle.
        if (alert.getDelivery() != null && alert.getDelivery().getVehicle() != null
                && alert.getDelivery().getVehicle().getDriver() != null) {
            recipients.add(alert.getDelivery().getVehicle().getDriver());
        }

        for (User recipient : recipients) {
            if (!recipient.isEnabled()) {
                continue;
            }
            for (NotificationChannel channel : channels) {
                if (!channel.isEnabled()) {
                    continue;
                }
                deliver(recipient, alert, channel);
            }
        }

        // The public map layer gets the alert too, for anyone watching without an account.
        broadcaster.alertRaised(alertMapper.toResponse(alert),
                alert.getDistrict() == null ? null : alert.getDistrict().getCode());
    }

    /**
     * One delivery attempt, recorded whether it worked or not. A failure is logged and
     * stored as FAILED rather than thrown, so one broken channel cannot stop the others
     * or roll back the alert itself.
     */
    private void deliver(User recipient, Alert alert, NotificationChannel channel) {
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setAlert(alert);
        notification.setChannel(channel.channel());
        notification.setSentAt(OffsetDateTime.now());
        try {
            channel.send(recipient, alert);
            notification.setStatus(NotificationStatus.SENT);
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("Channel {} failed for user {}: {}",
                    channel.channel(), recipient.getUsername(), ex.getMessage());
        }
        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> findMine(boolean unreadOnly, Pageable pageable) {
        Long userId = currentUserId();
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByUserIdAndReadFlagFalseOrderBySentAtDesc(
                        userId, pageable)
                : notificationRepository.findByUserIdOrderBySentAtDesc(userId, pageable);
        return page.map(alertMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long unreadCount() {
        return notificationRepository.countByUserIdAndReadFlagFalse(currentUserId());
    }

    @Override
    @Transactional
    public void markRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notification", "id", notificationId));
        // Ownership check: reading someone else's notifications is not allowed even for
        // an administrator, because it is their inbox, not shared data.
        if (!notification.getUser().getId().equals(currentUserId())) {
            throw new AccessDeniedException("This notification belongs to another user");
        }
        notification.setReadFlag(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead(currentUserId());
    }

    private Long currentUserId() {
        return SecurityUtils.currentUserId()
                .orElseThrow(() -> new AccessDeniedException("Not authenticated"));
    }
}
