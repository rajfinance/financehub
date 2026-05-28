package com.financehub.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoanSummaryDTO {
    private Long id;
    private String loanAccountNumber;
    private String bankName;
    private String loanType;
    private Double loanAmount;
    private String formattedLoanAmount;
    private Integer tenure;
    private Double interestRate;
    private Double emiAmount;
    private String formattedEmiAmount;
    private LocalDate firstEmiDate;
    private String formattedFirstEmiDate;
    private LocalDate endDate;
    private String formattedEndDate;
    /** Open while EMIs remain; Closed after the last EMI date has passed. */
    private String loanStatus;
    private boolean preClosed;
    private LocalDate preClosureDate;
    private String formattedPreClosureDate;
    private Double preClosureAmount;
    private String formattedPreClosureAmount;
    private String preClosureType;
    private String preClosureReferenceNumber;
    private String formattedLastEmiPaidDate;
}
