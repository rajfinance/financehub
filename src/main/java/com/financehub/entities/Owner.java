package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Table(name = "owners")
public class Owner {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;
    @Column(name = "address", nullable = false)
    private String address;
    @Column(name = "advance_months", nullable = false)
    private Integer advanceMonths;
    @Column(name = "advance_amount", nullable = false)
    private Double advanceAmount;
    @Column(name = "advance_date", nullable = false)
    private LocalDate advanceDate;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}
