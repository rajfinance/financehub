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
}
