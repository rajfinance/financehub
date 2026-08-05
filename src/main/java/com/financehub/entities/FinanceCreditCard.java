package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "finance_credit_cards")
public class FinanceCreditCard {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "card_name", nullable = false, length = 120)
	private String cardName;

	@Column(name = "bank_name", length = 120)
	private String bankName;

	@Column(name = "credit_limit")
	private Double creditLimit;

	@Column(name = "outstanding_balance", nullable = false)
	private Double outstandingBalance;

	@Column(name = "billing_day")
	private Integer billingDay;

	@Column(name = "due_day")
	private Integer dueDay;

	@Column(length = 500)
	private String notes;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
