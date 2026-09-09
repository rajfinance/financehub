package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "finance_credit_card_bills")
public class FinanceCreditCardBill {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "card_id", nullable = false)
	private Long cardId;

	@Column(name = "bill_month", nullable = false)
	private Integer billMonth;

	@Column(name = "bill_year", nullable = false)
	private Integer billYear;

	@Column(name = "billing_date", nullable = false)
	private LocalDate billingDate;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "interest_amount")
	private Double interestAmount;

	@Column(name = "outstanding_amount", nullable = false)
	private Double outstandingAmount;

	@Column(name = "paid_amount")
	private Double paidAmount;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
