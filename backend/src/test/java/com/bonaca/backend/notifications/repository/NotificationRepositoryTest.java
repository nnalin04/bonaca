package com.bonaca.backend.notifications.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bonaca.backend.notifications.model.Notification;
import com.bonaca.backend.notifications.model.NotificationType;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void findByRecipientMemberIdOrderByCreatedAtDescReturnsOnlyThatRecipientsRowsNewestFirst() {
        UUID recipientId = UUID.randomUUID();
        UUID otherRecipientId = UUID.randomUUID();
        Notification older = notificationRepository.saveAndFlush(new Notification(
                recipientId, recipientId, NotificationType.SUBSCRIPTION, "Me", "Older", "/subscription", null));
        Notification newer = notificationRepository.saveAndFlush(new Notification(
                recipientId, recipientId, NotificationType.PAYMENT_REQUEST, "Me", "Newer", "/subscription", null));
        notificationRepository.saveAndFlush(new Notification(
                otherRecipientId, otherRecipientId, NotificationType.SUBSCRIPTION, "Other", "Not mine", "/subscription", null));

        var result = notificationRepository.findByRecipientMemberIdOrderByCreatedAtDesc(recipientId);

        assertThat(result).extracting(Notification::getId).containsExactly(newer.getId(), older.getId());
    }

    @Test
    void existsByRecipientMemberIdAndSourceInsightIdDistinguishesByBothFields() {
        UUID recipientId = UUID.randomUUID();
        UUID insightId = UUID.randomUUID();
        notificationRepository.saveAndFlush(new Notification(
                recipientId,
                recipientId,
                NotificationType.METRIC_ANOMALY,
                "Dad",
                "Anomaly",
                "/member/dad",
                insightId));

        assertThat(notificationRepository.existsByRecipientMemberIdAndSourceInsightId(recipientId, insightId)).isTrue();
        assertThat(notificationRepository.existsByRecipientMemberIdAndSourceInsightId(recipientId, UUID.randomUUID()))
                .isFalse();
        assertThat(notificationRepository.existsByRecipientMemberIdAndSourceInsightId(UUID.randomUUID(), insightId))
                .isFalse();
    }

    @Test
    void existsByRecipientMemberIdAndTypeDistinguishesByBothFields() {
        UUID recipientId = UUID.randomUUID();
        notificationRepository.saveAndFlush(new Notification(
                recipientId, recipientId, NotificationType.SUBSCRIPTION, "Me", "Lapsed", "/subscription", null));

        assertThat(notificationRepository.existsByRecipientMemberIdAndType(recipientId, NotificationType.SUBSCRIPTION))
                .isTrue();
        assertThat(notificationRepository.existsByRecipientMemberIdAndType(recipientId, NotificationType.PAYMENT_REQUEST))
                .isFalse();
    }
}
