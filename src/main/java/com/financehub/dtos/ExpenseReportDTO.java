package com.financehub.dtos;

import lombok.Data;

import java.util.Map;

@Data
public class ExpenseReportDTO {
    private int id;
    private int expenseYear;
    private int month;
    private String monthStr;
    private String expenseType;
    private Map<Integer, Double> plannedExpenses;
    private Map<Integer, Double> actualExpenses;
    private double planAmount;
    private double actualAmount;
    private String planAmountStr;
    private String actualAmountStr;
    private String category;
    public ExpenseReportDTO(int id, int expenseYear, int month,String monthStr,Map<Integer, Double> plannedExpenses,Map<Integer, Double> actualExpenses,double planAmount,double actualAmount,String category) {
        this.id = id;
        this.expenseYear = expenseYear;
        this.month = month;
        this.monthStr = monthStr;
        this.plannedExpenses = plannedExpenses;
        this.actualExpenses = actualExpenses;
        this.planAmount = planAmount;
        this.actualAmount = actualAmount;
        this.category = category;
    }
}
