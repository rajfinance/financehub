package com.financehub.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ExpenseRequest {
    private Long expenseId;
    private String month;
    private String expenseType;
    private Map<Integer, Double> expenses;

    /**
     * Binds form fields like expenses[12]=100 from multipart posts (keys may arrive as strings).
     */
    public void setExpenses(Map<?, ?> raw) {
        if (raw == null || raw.isEmpty()) {
            this.expenses = new HashMap<>();
            return;
        }
        Map<Integer, Double> normalized = new HashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Integer key;
            if (entry.getKey() instanceof Integer) {
                key = (Integer) entry.getKey();
            } else {
                key = Integer.valueOf(entry.getKey().toString().trim());
            }
            Double value = toDouble(entry.getValue());
            normalized.put(key, value);
        }
        this.expenses = normalized;
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        String s = value.toString().trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return 0.0;
        }
        return Double.parseDouble(s);
    }
}
