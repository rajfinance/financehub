package com.financehub.dtos;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class FinanceInsuranceDTO {
	private Long id;
	private String policyName;
	private String insurerName;
	private String policyNumber;
	private String policyType;
	private Double premiumAmount;
	private String formattedPremium;
	private String premiumFrequency;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate nextDueDate;
	private String formattedDueDate;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate commencementDate;
	private String formattedCommencementDate;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate maturityDate;
	private String formattedMaturityDate;
	private Integer policyTermYears;
	private Integer premiumPaymentTermYears;
	private Double coverAmount;
	private String formattedCover;
	private String notes;
	private Integer commencementYear;
	/** red = due within 5 days, light = due within 15 days */
	private String dueUrgency;
}
