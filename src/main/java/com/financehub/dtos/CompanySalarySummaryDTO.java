package com.financehub.dtos;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CompanySalarySummaryDTO {
    private Long companyId;
    private String companyName;
    private double totalAmount;
    private String formattedTotal;
    private int entryCount;
    /** Year → raw total (for PDF / calculations). */
    private Map<Integer, Double> yearTotals = new LinkedHashMap<>();
    /** Year → Indian-formatted amount (for UI). */
    private Map<Integer, String> yearWiseTotals = new LinkedHashMap<>();
}
