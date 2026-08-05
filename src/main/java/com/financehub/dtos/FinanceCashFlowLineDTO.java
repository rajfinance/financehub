package com.financehub.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceCashFlowLineDTO {
	private String label;
	private String formattedAmount;
	private double amount;
	private boolean inflow;
}
