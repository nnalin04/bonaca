package com.bonaca.backend.notifications.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @Column(name = "recipient_member_id", nullable = false)
    private UUID recipientMemberId;

    @Column(name = "subject_member_id", nullable = false)
    private UUID subjectMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "body", nullable = false, length = 280)
    private String body;

    @Column(name = "deep_link_target", nullable = false, length = 200)
    private String deepLinkTarget;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    /** Set only for METRIC_ANOMALY notifications, sourced from an Insight row — see InsightGenerationService::ANOMALY. */
    @Column(name = "source_insight_id")
    private UUID sourceInsightId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    public Notification(
            UUID recipientMemberId,
            UUID subjectMemberId,
            NotificationType type,
            String title,
            String body,
            String deepLinkTarget,
            UUID sourceInsightId) {
        this.recipientMemberId = recipientMemberId;
        this.subjectMemberId = subjectMemberId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.deepLinkTarget = deepLinkTarget;
        this.sourceInsightId = sourceInsightId;
    }

    public void markRead() {
        this.read = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRecipientMemberId() {
        return recipientMemberId;
    }

    public UUID getSubjectMemberId() {
        return subjectMemberId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getDeepLinkTarget() {
        return deepLinkTarget;
    }

    public boolean isRead() {
        return read;
    }

    public UUID getSourceInsightId() {
        return sourceInsightId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
