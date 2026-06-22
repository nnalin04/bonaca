CREATE TABLE metric_readings (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id        UUID NOT NULL REFERENCES members (id),
    metric_type      VARCHAR(40) NOT NULL,
    metric_value     NUMERIC(10,3) NOT NULL,
    unit             VARCHAR(20) NOT NULL,
    recorded_at      TIMESTAMPTZ NOT NULL,
    source_device_id VARCHAR(120),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_metric_readings_member_type_recorded ON metric_readings (member_id, metric_type, recorded_at DESC);

CREATE TABLE metric_baselines (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id       UUID NOT NULL REFERENCES members (id),
    metric_type     VARCHAR(40) NOT NULL,
    baseline_mean   NUMERIC(10,3) NOT NULL,
    baseline_stddev NUMERIC(10,3) NOT NULL,
    valid_day_count INT NOT NULL,
    computed_at     TIMESTAMPTZ NOT NULL,
    UNIQUE (member_id, metric_type)
);

CREATE TABLE insights (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id      UUID NOT NULL REFERENCES members (id),
    metric_type    VARCHAR(40),
    generated_text VARCHAR(280) NOT NULL,
    kind           VARCHAR(20) NOT NULL,
    insight_date   DATE NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (member_id, metric_type, insight_date)
);
