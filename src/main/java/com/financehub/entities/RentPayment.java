package com.financehub.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "rent_payments")
public class RentPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private Owner owner;
    @Column(name = "rent_period_start", nullable = false)
    private LocalDate rentPeriodStart;
    @Column(name = "rent_period_end", nullable = false)
    private LocalDate rentPeriodEnd;
    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;
    @Column(name = "rent_amount", nullable = false)
    private Double amount;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
