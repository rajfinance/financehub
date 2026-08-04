package com.financehub.dtos;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ExpensesCategoriesDTO {
    Long categoryId;
    String categoryName;
    int sortOrder;
    boolean enabled;
    /** Optional upload; when empty on update, existing icon_data is kept. */
    MultipartFile iconImage;
}
