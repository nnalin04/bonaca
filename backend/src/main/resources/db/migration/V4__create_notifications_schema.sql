CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_member_id UUID NOT NULL REFERENCES members (id),
    subject_member_id   UUID NOT NULL REFERENCES members (id),
    notification_type   VARCHAR(40) NOT NULL,
    title               VARCHAR(120) NOT NULL,
    body                VARCHAR(280) NOT NULL,
    deep_link_target    VARCHAR(200) NOT NULL,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    source_insight_id   UUID REFERENCES insights (id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notifications_recipient_created ON notifications (recipient_member_id, created_at DESC);
