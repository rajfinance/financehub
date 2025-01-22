package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "client")
    private String client;

    @Column(name = "project")
    private String project;

    @Column(name = "experience_from", nullable = false)
    private LocalDate experienceFrom;

    @Column(name = "experience_to")
    private LocalDate experienceTo;

    @Column(name = "is_current_company")
    private Boolean isCurrentCompany;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
