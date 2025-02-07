package com.financehub.dtos;

import lombok.Data;

@Data
public class ExpensesCategoriesDTO {
    String name;
    String iconPath;
    int sortOrder;
    boolean enabled;
}
