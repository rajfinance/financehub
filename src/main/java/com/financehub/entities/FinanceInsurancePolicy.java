package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "finance_insurance_policies")
public class FinanceInsurancePolicy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "policy_name", nullable = false, length = 150)
	private String policyName;

	@Column(name = "insurer_name", length = 120)
	private String insurerName;

	@Column(name = "policy_type", nullable = false, length = 40)
	private String policyType;

	@Column(name = "premium_amount", nullable = false)
	private Double premiumAmount;

	/** YEARLY, HALF_YEARLY, QUARTERLY, MONTHLY */
	@Column(name = "premium_frequency", nullable = false, length = 20)
	private String premiumFrequency;

	@Column(name = "next_due_date", nullable = false)
	private LocalDate nextDueDate;

	@Column(name = "cover_amount")
	private Double coverAmount;

	@Column(length = 500)
	private String notes;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
