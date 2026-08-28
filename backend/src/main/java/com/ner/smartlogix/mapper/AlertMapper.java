package com.ner.smartlogix.mapper;

import com.ner.smartlogix.dto.response.AlertResponse;
import com.ner.smartlogix.dto.response.NotificationResponse;
import com.ner.smartlogix.entity.Alert;
import com.ner.smartlogix.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class AlertMapper {

    public AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getAlertType(),
                alert.getSeverity(),
                alert.getTitle(),
                alert.getMessage(),
                alert.getDistrict() == null ? null : alert.getDistrict().getCode(),
                alert.getDistrict() == null ? null : alert.getDistrict().getName(),
                alert.getRoad() == null ? null : alert.getRoad().getCode(),
                alert.getIncident() == null ? null : alert.getIncident().getId(),
                alert.getDelivery() == null ? null : alert.getDelivery().getId(),
                alert.getLocation() == null ? null : alert.getLocation().getY(),
                alert.getLocation() == null ? null : alert.getLocation().getX(),
                alert.isActive(),
                alert.getCreatedAt(),
                alert.getExpiresAt());
    }

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getChannel(),
                notification.isReadFlag(),
                notification.getSentAt(),
                notification.getAlert() == null ? null : toResponse(notification.getAlert()));
    }
}
