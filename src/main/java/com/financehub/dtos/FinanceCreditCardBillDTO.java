package com.financehub.dtos;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FinanceCreditCardBillDTO {
	private Long id;
	private Long cardId;
	private String cardName;
	private Integer billMonth;
	private Integer billYear;
	private LocalDate billingDate;
	private String formattedBillingDate;
	private LocalDate dueDate;
	private String formattedDueDate;
	private Double interestAmount;
	private String formattedInterestAmount;
	private Double outstandingAmount;
	private String formattedOutstanding;
	private Double paidAmount;
	private String formattedPaidAmount;
	private boolean dueSoon;
}
