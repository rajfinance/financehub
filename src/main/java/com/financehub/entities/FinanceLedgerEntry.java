package com.financehub.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "finance_ledger_entries")
public class FinanceLedgerEntry {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "account_id", nullable = false)
	private Long accountId;

	@Column(name = "entry_date", nullable = false)
	private LocalDate entryDate;

	/** CREDIT or DEBIT */
	@Column(name = "entry_type", nullable = false, length = 10)
	private String entryType;

	@Column(nullable = false)
	private Double amount;

	@Column(length = 255)
	private String description;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
}
