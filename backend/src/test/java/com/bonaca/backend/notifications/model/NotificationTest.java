package com.bonaca.backend.notifications.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @Test
    void accessorsReflectConstructorArgumentsForASystemGeneratedNotification() {
        UUID recipientId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();

        Notification notification = new Notification(
                recipientId,
                subjectId,
                NotificationType.METRIC_ANOMALY,
                "Dad",
                "Heart rate has been higher than usual today.",
                "/member/dad/metric/heart_rate",
                insightId);

        assertThat(notification.getRecipientMemberId()).isEqualTo(recipientId);
        assertThat(notification.getSubjectMemberId()).isEqualTo(subjectId);
        assertThat(notification.getType()).isEqualTo(NotificationType.METRIC_ANOMALY);
        assertThat(notification.getTitle()).isEqualTo("Dad");
        assertThat(notification.getBody()).isEqualTo("Heart rate has been higher than usual today.");
        assertThat(notification.getDeepLinkTarget()).isEqualTo("/member/dad/metric/heart_rate");
        assertThat(notification.getSourceInsightId()).isEqualTo(insightId);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getCreatedAt()).isNotNull();
        assertThat(notification.getId()).isNull();
    }

    @Test
    void sourceInsightIdIsNullForNonAnomalyNotifications() {
        UUID memberId = UUID.randomUUID();

        Notification notification = new Notification(
                memberId, memberId, NotificationType.SUBSCRIPTION, "Me", "Subscription lapsed.", "/subscription", null);

        assertThat(notification.getSourceInsightId()).isNull();
    }

    @Test
    void markReadSetsReadTrue() {
        UUID memberId = UUID.randomUUID();
        Notification notification = new Notification(
                memberId, memberId, NotificationType.PAYMENT_REQUEST, "Me", "Please pay.", "/subscription/payment-gateway", null);

        notification.markRead();

        assertThat(notification.isRead()).isTrue();
    }
}
