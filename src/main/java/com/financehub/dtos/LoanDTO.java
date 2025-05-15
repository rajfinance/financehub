package com.financehub.dtos;

import lombok.Data;

import java.time.LocalDate;
@Data
public class LoanDTO {
    private Long loanId;
    private String bankName;
    private String loanType;
    private Double loanAmount;
    private LocalDate startDate;
    private Double interestRate;

    private Double emiAmount;
    private Integer tenure;
    private LocalDate nextEmiDate;

    private Double remainingPrincipal;
    private Double closingAmount;

}
