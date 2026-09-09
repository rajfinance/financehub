-- Monthly credit card bills. Run once on PostgreSQL.

CREATE TABLE IF NOT EXISTS finance_credit_card_bills (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    card_id             BIGINT          NOT NULL REFERENCES finance_credit_cards (id) ON DELETE CASCADE,
    bill_month          INT             NOT NULL,
    bill_year           INT             NOT NULL,
    billing_date        DATE            NOT NULL,
    due_date            DATE            NOT NULL,
    interest_amount     DOUBLE PRECISION,
    outstanding_amount  DOUBLE PRECISION NOT NULL DEFAULT 0,
    paid_amount         DOUBLE PRECISION,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_card_bill_period UNIQUE (user_id, card_id, bill_month, bill_year)
);

CREATE INDEX IF NOT EXISTS idx_finance_card_bills_user ON finance_credit_card_bills (user_id);
CREATE INDEX IF NOT EXISTS idx_finance_card_bills_card ON finance_credit_card_bills (card_id);
