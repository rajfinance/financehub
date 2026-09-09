-- Run once on PostgreSQL
ALTER TABLE finance_accounts ADD COLUMN IF NOT EXISTS ifsc_code VARCHAR(11);
ALTER TABLE finance_accounts ADD COLUMN IF NOT EXISTS home_branch VARCHAR(120);
