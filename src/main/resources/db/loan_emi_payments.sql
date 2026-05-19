-- Run this script on PostgreSQL (FinanceHub database).
--
-- Your loans table was created with UNIQUE(loan_account_number) but without
-- PRIMARY KEY(id). PostgreSQL requires a unique/PK on loans.id for a FK.

-- Step 1: Add primary key on loans.id (skip if you already have loans_pkey)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON c.conrelid = t.oid
        JOIN pg_namespace n ON t.relnamespace = n.oid
        WHERE n.nspname = 'public'
          AND t.relname = 'loans'
          AND c.contype = 'p'
    ) THEN
        ALTER TABLE public.loans ADD CONSTRAINT loans_pkey PRIMARY KEY (id);
    END IF;
END $$;

-- Step 2: Optional EMI overrides (only when amount or deduction date differs)
CREATE TABLE IF NOT EXISTS public.loan_emi_payments (
    id int4 GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    loan_id int4 NOT NULL,
    emi_number int4 NOT NULL,
    emi_amount numeric(12, 2) NOT NULL,
    paid_on date NOT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_loan_emi_number UNIQUE (loan_id, emi_number),
    CONSTRAINT fk_loan_emi_payments_loan
        FOREIGN KEY (loan_id) REFERENCES public.loans (id) ON DELETE CASCADE
);
