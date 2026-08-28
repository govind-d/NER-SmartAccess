package com.ner.smartlogix.service;

import com.ner.smartlogix.dto.response.NotificationResponse;
import com.ner.smartlogix.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    /** Fans one alert out to every user who should see it, through every enabled channel. */
    void dispatch(Alert alert);

    Page<NotificationResponse> findMine(boolean unreadOnly, Pageable pageable);

    long unreadCount();

    void markRead(Long notificationId);

    void markAllRead();
}
