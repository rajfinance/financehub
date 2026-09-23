-- Card number, expiry, CVV, statement bill amount, paid date.
-- Allows multiple payments for the same card month.
-- Safe to run more than once on PostgreSQL.

ALTER TABLE finance_credit_cards ADD COLUMN IF NOT EXISTS card_number VARCHAR(19);
ALTER TABLE finance_credit_cards ADD COLUMN IF NOT EXISTS expiry_month INT;
ALTER TABLE finance_credit_cards ADD COLUMN IF NOT EXISTS expiry_year INT;
ALTER TABLE finance_credit_cards ADD COLUMN IF NOT EXISTS cvv VARCHAR(4);

ALTER TABLE finance_credit_card_bills ADD COLUMN IF NOT EXISTS bill_amount DOUBLE PRECISION;
ALTER TABLE finance_credit_card_bills ADD COLUMN IF NOT EXISTS paid_date DATE;

ALTER TABLE finance_credit_card_bills DROP CONSTRAINT IF EXISTS uq_card_bill_period;
