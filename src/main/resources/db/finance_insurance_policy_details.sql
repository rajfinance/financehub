-- Extra insurance policy fields. Safe to run more than once on PostgreSQL.

ALTER TABLE finance_insurance_policies ADD COLUMN IF NOT EXISTS policy_number VARCHAR(80);
ALTER TABLE finance_insurance_policies ADD COLUMN IF NOT EXISTS commencement_date DATE;
ALTER TABLE finance_insurance_policies ADD COLUMN IF NOT EXISTS maturity_date DATE;
ALTER TABLE finance_insurance_policies ADD COLUMN IF NOT EXISTS policy_term_years INT;
ALTER TABLE finance_insurance_policies ADD COLUMN IF NOT EXISTS premium_payment_term_years INT;
