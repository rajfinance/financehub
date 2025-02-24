package com.financehub.dtos;

import lombok.Data;
import java.util.Map;

@Data
public class ExpenseRequest {
    private Integer expenseId;
    private String month;
    private String expenseType;
    private Map<Integer, Double> expenses;
}
