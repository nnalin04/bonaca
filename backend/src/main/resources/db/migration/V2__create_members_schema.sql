CREATE TABLE accounts (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_member_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE members (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id     UUID NOT NULL REFERENCES accounts (id),
    user_id        UUID NOT NULL UNIQUE REFERENCES users (id),
    role           VARCHAR(20) NOT NULL,
    name           VARCHAR(120) NOT NULL,
    nickname       VARCHAR(120),
    pinned         BOOLEAN NOT NULL DEFAULT FALSE,
    hidden         BOOLEAN NOT NULL DEFAULT FALSE,
    status_message VARCHAR(280),
    gender         VARCHAR(40),
    dob            DATE,
    height_cm      INT,
    weight_kg      INT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_members_account_id ON members (account_id);

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_owner_member FOREIGN KEY (owner_member_id) REFERENCES members (id);

CREATE TABLE invites (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inviter_account_id UUID NOT NULL REFERENCES accounts (id),
    phone_number       VARCHAR(20) NOT NULL,
    role_offered       VARCHAR(20) NOT NULL,
    status             VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    accepted_member_id UUID REFERENCES members (id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_invites_phone_number_status ON invites (phone_number, status);
CREATE INDEX idx_invites_inviter_account_id ON invites (inviter_account_id);

CREATE TABLE sharing_grants (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    granter_member_id  UUID NOT NULL REFERENCES members (id),
    grantee_member_id  UUID NOT NULL REFERENCES members (id),
    scope              VARCHAR(20) NOT NULL,
    visible            BOOLEAN NOT NULL DEFAULT TRUE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (granter_member_id, grantee_member_id, scope)
);

CREATE INDEX idx_sharing_grants_grantee ON sharing_grants (grantee_member_id);
CREATE INDEX idx_sharing_grants_granter ON sharing_grants (granter_member_id);

CREATE TABLE subscriptions (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id    UUID NOT NULL UNIQUE REFERENCES accounts (id),
    status        VARCHAR(20) NOT NULL,
    trial_ends_at TIMESTAMPTZ,
    renewed_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
