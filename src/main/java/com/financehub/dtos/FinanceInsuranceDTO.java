package com.financehub.dtos;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class FinanceInsuranceDTO {
	private Long id;
	private String policyName;
	private String insurerName;
	private String policyType;
	private Double premiumAmount;
	private String formattedPremium;
	private String premiumFrequency;
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate nextDueDate;
	private String formattedDueDate;
	private Double coverAmount;
	private String formattedCover;
	private String notes;
	private boolean dueSoon;
	private boolean overdue;
	private String alertLabel;
}
