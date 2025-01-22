package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "salary")
public class Salary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private ClientUser user;

    @Column(name = "salary_month", nullable = false)
    private int salaryMonth;

    @Column(name = "salary_year", nullable = false)
    private int salaryYear;

    @Column(name = "credit_date", nullable = false)
    private LocalDate creditDate;

    @Column(name = "salary_amount", nullable = false)
    private Double salaryAmount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
