package com.bonaca.backend.notifications.repository;

import com.bonaca.backend.notifications.model.Notification;
import com.bonaca.backend.notifications.model.NotificationType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByRecipientMemberIdOrderByCreatedAtDesc(UUID recipientMemberId);

    boolean existsByRecipientMemberIdAndSourceInsightId(UUID recipientMemberId, UUID sourceInsightId);

    boolean existsByRecipientMemberIdAndType(UUID recipientMemberId, NotificationType type);
}
