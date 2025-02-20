package com.financehub.dtos;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Map;

@Converter(autoApply = true)
public class ExpenseDetail implements AttributeConverter<Map<Integer, Double>, String> {

    @Override
    public String convertToDatabaseColumn(Map<Integer, Double> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Double> entry : attribute.entrySet()) {
            sb.append(entry.getKey())
                    .append(":")
                    .append(entry.getValue() == null || "null".equals(entry.getValue().toString().trim()) ? "0" : entry.getValue())
                    .append(",");
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public Map<Integer, Double> convertToEntityAttribute(String dbData) {
        Map<Integer, Double> map = new HashMap<>();
        if (dbData == null || dbData.trim().isEmpty()) {
            return map;
        }
        String[] pairs = dbData.split(",");
        for (String pair : pairs) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                try {
                    Integer key = Integer.valueOf(parts[0].trim());
                    Double value = Double.valueOf((parts[1].equals("null"))?"0":parts[1].trim());
                    map.put(key, value);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return map;
    }
}
