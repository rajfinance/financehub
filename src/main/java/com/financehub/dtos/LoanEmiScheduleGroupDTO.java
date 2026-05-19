package com.financehub.dtos;

import lombok.Data;

import java.util.List;

@Data
public class LoanEmiScheduleGroupDTO {
    private LoanSummaryDTO loan;
    private List<LoanEmiScheduleRowDTO> scheduleRows;
}
