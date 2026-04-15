package com.financehub.dtos;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ExpensesCategoriesDTO {
    Long categoryId;
    String categoryName;
    String iconPath;
    int sortOrder;
    boolean enabled;
    /** Optional upload; when empty, {@code iconPath} is kept (or default for new categories). */
    MultipartFile iconImage;
}
