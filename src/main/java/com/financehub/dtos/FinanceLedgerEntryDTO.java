package com.financehub.dtos;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class FinanceLedgerEntryDTO {
	private Long id;
	private Long accountId;
	private String accountName;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate entryDate;
	private String entryType;
	private Double amount;
	private String formattedAmount;
	private String description;
}
