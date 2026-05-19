package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "loan_emi_payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_loan_emi_number", columnNames = {"loan_id", "emi_number"})
})
public class LoanEmiPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    /** Installment number within loan tenure (1 = first EMI). */
    @Column(name = "emi_number", nullable = false)
    private Integer emiNumber;

    @Column(name = "emi_amount", nullable = false)
    private Double emiAmount;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    @Column(name = "created_at", columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
