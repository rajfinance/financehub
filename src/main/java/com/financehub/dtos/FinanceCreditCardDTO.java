package com.financehub.dtos;

import lombok.Data;

@Data
public class FinanceCreditCardDTO {
	private Long id;
	private String cardName;
	private String bankName;
	private Double creditLimit;
	private String formattedCreditLimit;
	private Double outstandingBalance;
	private String formattedOutstanding;
	private Integer billingDay;
	private Integer dueDay;
	private Double interestRate;
	private String formattedInterestRate;
	private String notes;
	private boolean dueSoon;
}
