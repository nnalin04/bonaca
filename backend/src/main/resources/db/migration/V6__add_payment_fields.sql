ALTER TABLE subscriptions
    ADD COLUMN razorpay_subscription_id VARCHAR(60),
    ADD COLUMN razorpay_plan_id         VARCHAR(60),
    ADD COLUMN next_billing_at          TIMESTAMPTZ;

CREATE TABLE payment_events (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id               UUID NOT NULL REFERENCES accounts (id),
    razorpay_event_id        VARCHAR(60) NOT NULL UNIQUE,
    event_type               VARCHAR(60) NOT NULL,
    razorpay_subscription_id VARCHAR(60),
    razorpay_payment_id      VARCHAR(60),
    payload                  TEXT NOT NULL,
    processed_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_events_account ON payment_events (account_id);
