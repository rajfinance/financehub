package com.financehub.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoanPreClosureDTO {
    private Long loanId;
    private LocalDate preClosureDate;
    private Double settlementAmount;
    private String preClosureType;
    private String referenceNumber;
    private Double updatedEmiAmount;
    private Integer updatedTenure;
}
