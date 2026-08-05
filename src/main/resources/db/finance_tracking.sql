-- Manual finance tracking (no bank APIs). Run once on PostgreSQL.

CREATE TABLE IF NOT EXISTS finance_accounts (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    name            VARCHAR(120)    NOT NULL,
    account_type    VARCHAR(20)     NOT NULL,
    bank_name       VARCHAR(120),
    account_mask    VARCHAR(40),
    current_balance DOUBLE PRECISION NOT NULL DEFAULT 0,
    notes           VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_finance_accounts_user ON finance_accounts (user_id);

CREATE TABLE IF NOT EXISTS finance_ledger_entries (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    account_id      BIGINT          NOT NULL REFERENCES finance_accounts (id) ON DELETE CASCADE,
    entry_date      DATE            NOT NULL,
    entry_type      VARCHAR(10)     NOT NULL,
    amount          DOUBLE PRECISION NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_finance_ledger_user ON finance_ledger_entries (user_id);
CREATE INDEX IF NOT EXISTS idx_finance_ledger_account ON finance_ledger_entries (account_id);

CREATE TABLE IF NOT EXISTS finance_credit_cards (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT          NOT NULL,
    card_name            VARCHAR(120)    NOT NULL,
    bank_name            VARCHAR(120),
    credit_limit         DOUBLE PRECISION,
    outstanding_balance  DOUBLE PRECISION NOT NULL DEFAULT 0,
    billing_day          INT,
    due_day              INT,
    interest_rate        DOUBLE PRECISION,
    notes                VARCHAR(500),
    created_at           TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_finance_cards_user ON finance_credit_cards (user_id);

-- If finance_credit_cards already exists without interest_rate, run:
-- ALTER TABLE finance_credit_cards ADD COLUMN IF NOT EXISTS interest_rate DOUBLE PRECISION;

CREATE TABLE IF NOT EXISTS finance_insurance_policies (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            BIGINT          NOT NULL,
    policy_name        VARCHAR(150)    NOT NULL,
    insurer_name       VARCHAR(120),
    policy_type        VARCHAR(40)     NOT NULL,
    premium_amount     DOUBLE PRECISION NOT NULL,
    premium_frequency  VARCHAR(20)     NOT NULL,
    next_due_date      DATE            NOT NULL,
    cover_amount       DOUBLE PRECISION,
    notes              VARCHAR(500),
    created_at         TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_finance_insurance_user ON finance_insurance_policies (user_id);
CREATE INDEX IF NOT EXISTS idx_finance_insurance_due ON finance_insurance_policies (user_id, next_due_date);
