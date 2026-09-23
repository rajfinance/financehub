package com.financehub.dtos;

import lombok.Data;

@Data
public class FinanceCreditCardDTO {
	private Long id;
	private String cardName;
	private String bankName;
	private String cardNumber;
	private Integer expiryMonth;
	private Integer expiryYear;
	private String formattedExpiry;
	private String cvv;
	private String lastFour;
	/** Bank - card name - last 4 digits, for bill dropdowns. */
	private String displayLabel;
	private Double creditLimit;
	private String formattedCreditLimit;
	private Double outstandingBalance;
	private String formattedOutstanding;
	private Integer billingDay;
	private Integer dueDay;
	private String notes;
}
