package com.financehub.dtos;

import lombok.Data;

import java.time.LocalDate;
@Data
public class LoanDTO {
    private Long id;
    private String loanAccountNumber;
    private String bankName;
    private String loanType;
    private Double loanAmount;
    private Integer tenure;
    private Double interestRate;
    private Double emiAmount;
    private LocalDate emiDate;
}
