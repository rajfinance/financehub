package com.financehub.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LoanEmiPaymentDTO {
    private Long id;
    private Long loanId;
    private Integer emiNumber;
    private Double emiAmount;
    private LocalDate paidOn;
    private Boolean preClosureSelected;
    private LocalDate preClosureDate;
    private Double preClosureAmount;
    private String preClosureType;
    private String preClosureReferenceNumber;
    private Double partialUpdatedEmiAmount;
    private Integer partialUpdatedTenure;
}
