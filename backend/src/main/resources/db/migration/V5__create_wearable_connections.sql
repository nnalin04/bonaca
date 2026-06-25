CREATE TABLE wearable_connections (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id        UUID NOT NULL UNIQUE REFERENCES members (id),
    spike_user_id    VARCHAR(120) NOT NULL,
    provider         VARCHAR(40),
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    connect_url      TEXT,
    connected_at     TIMESTAMPTZ,
    last_synced_at   TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wearable_connections_member ON wearable_connections (member_id);
CREATE INDEX idx_wearable_connections_spike_user ON wearable_connections (spike_user_id);
