-- Schéma initial SecureBank.
-- Types stricts : NUMERIC(19,4) pour toute somme d'argent (jamais de FLOAT/DOUBLE),
-- UUID pour tous les identifiants (évite l'énumération séquentielle d'IDENTITY/SERIAL).

CREATE TABLE app_user (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 VARCHAR(150) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(100) NOT NULL,
    role                  VARCHAR(30)  NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked    BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    last_login_at         TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE account (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    iban        VARCHAR(34) NOT NULL UNIQUE,
    owner_id    UUID NOT NULL REFERENCES app_user (id),
    balance     NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency    VARCHAR(3) NOT NULL DEFAULT 'XAF',
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_account_owner_id ON account (owner_id);

CREATE TABLE transaction (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id          UUID NOT NULL REFERENCES account (id),
    transfer_reference  UUID NOT NULL,
    type                VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    balance_after       NUMERIC(19,4) NOT NULL,
    description         VARCHAR(255),
    counterparty_iban   VARCHAR(34),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_transaction_account_id_created_at ON transaction (account_id, created_at DESC);
CREATE INDEX idx_transaction_transfer_reference ON transaction (transfer_reference);

CREATE TABLE audit_log (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_email  VARCHAR(150) NOT NULL,
    action       VARCHAR(100) NOT NULL,
    details      VARCHAR(500),
    success      BOOLEAN NOT NULL,
    source_ip    VARCHAR(45) NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at DESC);
