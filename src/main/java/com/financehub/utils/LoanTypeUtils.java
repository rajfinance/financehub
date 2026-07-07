package com.financehub.utils;

import com.financehub.entities.Loan;

public final class LoanTypeUtils {

    public static final String GOLD = "Gold";

    private LoanTypeUtils() {
    }

    public static boolean isGoldLoan(String loanType) {
        return loanType != null && GOLD.equalsIgnoreCase(loanType.trim());
    }

    public static boolean isGoldLoan(Loan loan) {
        return loan != null && isGoldLoan(loan.getLoanType());
    }

    /** Fixed tenure in months for gold loans (1 year). */
    public static int goldLoanTenureMonths() {
        return 12;
    }

    /** Number of yearly interest payments within a gold loan tenure. */
    public static int goldLoanInstallmentCount() {
        return 1;
    }

    /** Installment rows built for schedules, EMI recording, and pre-closure. */
    public static int getScheduleInstallmentCount(Loan loan) {
        if (isGoldLoan(loan)) {
            return goldLoanInstallmentCount();
        }
        return loan.getTenure() != null ? loan.getTenure() : 0;
    }
}
