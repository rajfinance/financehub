package com.financehub.dtos;

import lombok.Data;

import java.util.List;

@Data
public class LoanBankEmiProjectionReportDTO {
    private long axisHeaderAmount;
    private long iciciHeaderAmount;
    private long hdfcHeaderAmount;
    private List<LoanBankEmiProjectionRowDTO> rows;
}
