package com.bonaca.backend.notifications.dto;

import com.bonaca.backend.notifications.model.Notification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id, UUID memberId, String type, String title, String body, String deepLinkTarget, boolean read, Instant createdAt) {

    /** memberId is the notification's subject (who it's about), not the recipient — see plan doc §2. */
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getSubjectMemberId(),
                notification.getType().name().toLowerCase().replace('_', '-'),
                notification.getTitle(),
                notification.getBody(),
                notification.getDeepLinkTarget(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
