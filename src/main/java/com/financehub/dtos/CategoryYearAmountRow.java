package com.financehub.dtos;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class CategoryYearAmountRow {
    private String category;
    /** Year → formatted amount (empty years omitted from map; UI shows 0). */
    private Map<Integer, String> amountsByYear = new LinkedHashMap<>();
    private String total;
}
