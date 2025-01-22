package com.financehub.dtos;

import lombok.Data;

import java.util.List;
@Data
public class RentSummaryDTO {
    private List<RentPaymentDTO> payments;
    private String totalAmount;
    public RentSummaryDTO(List<RentPaymentDTO> payments, String totalAmount) {
        this.payments = payments;
        this.totalAmount = totalAmount;
    }
}
