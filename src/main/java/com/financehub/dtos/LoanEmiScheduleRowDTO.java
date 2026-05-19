package com.financehub.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoanEmiScheduleRowDTO {
    private Long loanId;
    private String loanAccountNumber;
    private String bankName;
    private String loanType;
    private int emiNumber;
    private LocalDate dueDate;
    private String formattedDueDate;
    private LocalDate deductionDate;
    private String formattedDeductionDate;
    private double emiAmount;
    private String formattedEmiAmount;
    /** Completed if EMI/deduction date is today or in the past; otherwise Pending. */
    private String emiStatus;
    /** True when a manual EMI record exists; false when using recurring schedule from loan. */
    private boolean recorded;
    private Long overrideId;
}
