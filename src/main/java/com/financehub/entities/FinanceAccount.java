package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "finance_accounts")
public class FinanceAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, length = 120)
	private String name;

	/** BANK, CASH, WALLET, INVESTMENT */
	@Column(name = "account_type", nullable = false, length = 20)
	private String accountType;

	@Column(name = "bank_name", length = 120)
	private String bankName;

	@Column(name = "account_mask", length = 40)
	private String accountMask;

	@Column(name = "current_balance", nullable = false)
	private Double currentBalance;

	@Column(length = 500)
	private String notes;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
