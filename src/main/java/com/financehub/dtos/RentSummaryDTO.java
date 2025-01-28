package com.financehub.dtos;

import lombok.Data;

import java.util.List;
@Data
public class RentSummaryDTO {
    private List<RentPaymentDTO> payments;
    private String totalAmount;
    private String totalPeriod;
    public RentSummaryDTO(List<RentPaymentDTO> payments, String totalAmount,String totalPeriod) {
        this.payments = payments;
        this.totalAmount = totalAmount;
        this.totalPeriod = totalPeriod;
    }
}
