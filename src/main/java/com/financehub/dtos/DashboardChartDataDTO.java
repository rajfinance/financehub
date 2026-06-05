package com.financehub.dtos;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class DashboardChartDataDTO {
    private Map<String, Integer> monthlySalaryData = new LinkedHashMap<>();
    private Map<String, Integer> yearlySalaryData = new LinkedHashMap<>();
    private Map<String, Integer> yearlyExpenseData = new LinkedHashMap<>();
    private Map<String, Integer> yearlyRentData = new LinkedHashMap<>();
    private Map<String, Integer> salaryData = new LinkedHashMap<>();
    private Map<String, Integer> expenseData = new LinkedHashMap<>();
    private int pendingEmi;
}
