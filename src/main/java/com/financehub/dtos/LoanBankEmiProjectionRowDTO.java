package com.financehub.dtos;

import lombok.Data;

@Data
public class LoanBankEmiProjectionRowDTO {
    private String date;
    private long axisAmount;
    private long iciciAmount;
    private long hdfcAmount;
    private long totalAmount;
    private long axisPayAmount;
    private long iciciPayAmount;
    private long hdfcPayAmount;
    private long axisAndHdfcPayAmount;
    private long totalPayAmount;
}
