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
	/** Statement period without year, e.g. 01/08-31/08. */
	private String formattedPeriod;
	private LocalDate billingDate;
	private String formattedBillingDate;
	private LocalDate dueDate;
	private String formattedDueDate;
	private Double interestAmount;
	private String formattedInterestAmount;
	private Double billAmount;
	private String formattedBillAmount;
	private Double outstandingAmount;
	private String formattedOutstanding;
	private Double paidAmount;
	private String formattedPaidAmount;
	private LocalDate paidDate;
	private String formattedPaidDate;
	/** Paid, Partially paid, or Not paid. */
	private String paymentStatus;
}
