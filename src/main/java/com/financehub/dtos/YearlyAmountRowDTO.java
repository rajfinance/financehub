package com.financehub.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YearlyAmountRowDTO {
	private String year;
	private String formattedAmount;
	private double amount;
}
