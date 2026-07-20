package com.financehub.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class LoanBankEmiProjectionRowDTO {
    private String date;
    /** Remaining balances aligned with report banks order. */
    private List<String> formattedAmounts = new ArrayList<>();
    /** Foreclosure pay amounts aligned with report banks order. */
    private List<String> formattedPays = new ArrayList<>();
    private String formattedTotalAmount;
    private String formattedComboPayAmount;
    private String formattedTotalPayAmount;
}
