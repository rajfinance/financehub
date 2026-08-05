package com.financehub.dtos;

import lombok.Data;

@Data
public class FinanceAccountDTO {
	private Long id;
	private String name;
	private String accountType;
	private String bankName;
	private String accountMask;
	private Double currentBalance;
	private String formattedBalance;
	private String notes;
}
