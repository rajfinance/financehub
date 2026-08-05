-- Add interest rate to credit cards (run if table already created without this column)
ALTER TABLE finance_credit_cards ADD COLUMN IF NOT EXISTS interest_rate DOUBLE PRECISION;
