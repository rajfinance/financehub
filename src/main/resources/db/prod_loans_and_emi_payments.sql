-- =============================================================================
-- FinanceHub – Production DDL (PostgreSQL)
-- Run Script 1 first, then Script 2.
-- If public.loans already exists, use the "existing loans" block in Script 1
-- instead of CREATE TABLE.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- SCRIPT 1: loans
-- -----------------------------------------------------------------------------

-- Option A – table does NOT exist yet (new environment)
CREATE TABLE IF NOT EXISTS public.loans (
    id int4 GENERATED ALWAYS AS IDENTITY (
        INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 START 1 CACHE 1
    ) NOT NULL,
    user_id int8 NOT NULL,
    loan_account_number varchar(30) NOT NULL,
    bank_name varchar(100) NOT NULL,
    loan_type varchar(50) NOT NULL,
    loan_amount numeric(12, 2) NOT NULL,
    tenure int4 NOT NULL,
    interest_rate numeric(5, 2) NULL,
    emi_amount numeric(12, 2) NOT NULL,
    emi_date date NOT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT loans_pkey PRIMARY KEY (id),
    CONSTRAINT loans_loan_account_number_key UNIQUE (loan_account_number)
);

-- Option B – loans already exists WITHOUT primary key on id (run once, then skip)
-- ALTER TABLE public.loans ADD CONSTRAINT loans_pkey PRIMARY KEY (id);

-- If emi_date was created as timestamp, app still works; optional normalize to date:
-- ALTER TABLE public.loans ALTER COLUMN emi_date TYPE date USING emi_date::date;


-- -----------------------------------------------------------------------------
-- SCRIPT 2: loan_emi_payments (optional EMI overrides per month)
-- Requires loans_pkey on public.loans(id)
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.loan_emi_payments (
    id int4 GENERATED ALWAYS AS IDENTITY (
        INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 START 1 CACHE 1
    ) NOT NULL,
    loan_id int4 NOT NULL,
    emi_number int4 NOT NULL,
    emi_amount numeric(12, 2) NOT NULL,
    paid_on date NOT NULL,
    created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
    CONSTRAINT loan_emi_payments_pkey PRIMARY KEY (id),
    CONSTRAINT uk_loan_emi_number UNIQUE (loan_id, emi_number),
    CONSTRAINT fk_loan_emi_payments_loan
        FOREIGN KEY (loan_id) REFERENCES public.loans (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_loan_emi_payments_loan_id
    ON public.loan_emi_payments (loan_id);
