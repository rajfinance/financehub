-- ============================================================================
-- FinanceHub - Loan Pre-Closure Migration (PostgreSQL)
-- Run once in production before enabling pre-closure features.
-- ============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS public.loan_preclosures (
    loan_id bigint PRIMARY KEY,
    pre_closure_date date NOT NULL,
    settlement_amount numeric(12,2) NOT NULL,
    pre_closure_type varchar(20) NOT NULL DEFAULT 'FULL',
    reference_number varchar(80),
    updated_emi_amount numeric(12,2),
    updated_tenure int4,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_loan_preclosures_loan
        FOREIGN KEY (loan_id) REFERENCES public.loans (id) ON DELETE CASCADE,
    CONSTRAINT chk_loan_preclosures_type
        CHECK (upper(pre_closure_type) IN ('FULL', 'PARTIAL'))
);

ALTER TABLE public.loan_preclosures
    ADD COLUMN IF NOT EXISTS pre_closure_type varchar(20) NOT NULL DEFAULT 'FULL';

ALTER TABLE public.loan_preclosures
    ADD COLUMN IF NOT EXISTS reference_number varchar(80);

ALTER TABLE public.loan_preclosures
    ADD COLUMN IF NOT EXISTS updated_emi_amount numeric(12,2);

ALTER TABLE public.loan_preclosures
    ADD COLUMN IF NOT EXISTS updated_tenure int4;

ALTER TABLE public.loan_preclosures
    ADD COLUMN IF NOT EXISTS created_at timestamp DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE public.loan_preclosures
    ADD COLUMN IF NOT EXISTS updated_at timestamp DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_loan_preclosures_pre_closure_date
    ON public.loan_preclosures (pre_closure_date);

-- Optional one-time backfill for old columns (if older schema exists)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'loan_preclosures' AND column_name = 'foreclosure_ref_number'
    ) THEN
        EXECUTE '
            UPDATE public.loan_preclosures
               SET reference_number = COALESCE(reference_number, foreclosure_ref_number)
             WHERE COALESCE(reference_number, '''') = ''''
               AND COALESCE(foreclosure_ref_number, '''') <> ''''
        ';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'loan_preclosures' AND column_name = 'noc_number'
    ) THEN
        EXECUTE '
            UPDATE public.loan_preclosures
               SET reference_number = COALESCE(reference_number, noc_number)
             WHERE COALESCE(reference_number, '''') = ''''
               AND COALESCE(noc_number, '''') <> ''''
        ';
    END IF;
END $$;

-- Remove legacy fallback markers: pre-closure is now stored only in loan_preclosures
DELETE FROM public.loan_emi_payments WHERE emi_number = 0;

COMMIT;
