-- Schéma H2 équivalent à V1/V2 (Flyway est désactivé en test - voir application-test.yml).
-- H2 en mode PostgreSQL supporte gen_random_uuid() nativement depuis H2 2.x.
-- @Sql réexécute ce script avant CHAQUE méthode de test : on repart d'un schéma vide à
-- chaque fois pour garantir l'isolation des tests entre eux.

DROP TABLE IF EXISTS audit_log;
DROP TABLE IF EXISTS transaction;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS app_user;

CREATE TABLE app_user (
    id                    UUID DEFAULT random_uuid() PRIMARY KEY,
    email                 VARCHAR(150) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    full_name             VARCHAR(100) NOT NULL,
    role                  VARCHAR(30)  NOT NULL,
    enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
    account_non_locked    BOOLEAN      NOT NULL DEFAULT TRUE,
    failed_login_attempts INT          NOT NULL DEFAULT 0,
    last_login_at         TIMESTAMP,
    created_at            TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE account (
    id          UUID DEFAULT random_uuid() PRIMARY KEY,
    iban        VARCHAR(34) NOT NULL UNIQUE,
    owner_id    UUID NOT NULL REFERENCES app_user (id),
    balance     NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency    VARCHAR(3) NOT NULL DEFAULT 'XAF',
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    version     BIGINT NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE transaction (
    id                  UUID DEFAULT random_uuid() PRIMARY KEY,
    account_id          UUID NOT NULL REFERENCES account (id),
    transfer_reference  UUID NOT NULL,
    type                VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    amount              NUMERIC(19,4) NOT NULL,
    balance_after       NUMERIC(19,4) NOT NULL,
    description         VARCHAR(255),
    counterparty_iban   VARCHAR(34),
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE audit_log (
    id           UUID DEFAULT random_uuid() PRIMARY KEY,
    actor_email  VARCHAR(150) NOT NULL,
    action       VARCHAR(100) NOT NULL,
    details      VARCHAR(500),
    success      BOOLEAN NOT NULL,
    source_ip    VARCHAR(45) NOT NULL,
    occurred_at  TIMESTAMP NOT NULL DEFAULT now()
);
