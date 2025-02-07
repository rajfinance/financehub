package com.financehub.dtos;

import lombok.Data;

@Data
public class ExpensesCategoriesDTO {
    Long categoryId;
    String categoryName;
    String iconPath;
    int sortOrder;
    boolean enabled;
}
