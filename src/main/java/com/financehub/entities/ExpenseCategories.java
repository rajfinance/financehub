package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "expense_categories")
public class ExpenseCategories {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "user_id", nullable=false)
    private Long userId;
    private String name;
    /** Custom uploaded icon bytes; served from /api/expenses/category-icon/{id}. */
    @Column(name = "icon_data")
    private byte[] iconData;
    @Column(name = "icon_content_type", length = 64)
    private String iconContentType;
    private boolean enabled;
    /** Display order; updated when categories are drag-reordered (0 = first). */
    @Column(name = "sort_order")
    private int sortOrder;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** URL for &lt;img src&gt;: DB icon endpoint, or placeholder when none. */
    @Transient
    public String getIconSrc() {
        if (iconData != null && iconData.length > 0) {
            return "/api/expenses/category-icon/" + id;
        }
        return "/images/category-placeholder.svg";
    }
}
