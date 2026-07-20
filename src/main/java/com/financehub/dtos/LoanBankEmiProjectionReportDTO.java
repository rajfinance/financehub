package com.financehub.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LoanBankEmiProjectionReportDTO {
    private List<LoanSummaryDTO> eligibleLoans = new ArrayList<>();
    private List<Long> selectedLoanIds = new ArrayList<>();
    /** Bank keys in display order (e.g. AXIS, ICICI, HDFC). */
    private List<String> banks = new ArrayList<>();
    /** Monthly EMI header amounts aligned with {@link #banks}. */
    private List<String> formattedHeaderEmis = new ArrayList<>();
    private List<String> comboBanks = new ArrayList<>();
    private String comboLabel = "";
    private List<LoanBankEmiProjectionRowDTO> rows = new ArrayList<>();
}
