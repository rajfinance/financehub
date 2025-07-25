package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "loan_account_number", nullable = false, length = 30, unique = true)
    private String loanAccountNumber;

    @Column(name = "bank_name", nullable = false, length = 100)
    private String bankName;

    @Column(name = "loan_type", nullable = false, length = 50)
    private String loanType;

    @Column(name = "loan_amount", nullable = false, precision = 12)
    private Double loanAmount;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "interest_rate", precision = 5)
    private Double interestRate;

    @Column(name = "emi_amount", nullable = false, precision = 12)
    private Double emiAmount;

    @Column(name = "tenure", nullable = false)
    private Integer tenure;

    @Column(name = "emi_date", nullable = false)
    private LocalDate emiDate;

    @Column(name = "created_at", columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

}
