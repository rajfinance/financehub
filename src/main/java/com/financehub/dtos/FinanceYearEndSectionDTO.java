package com.financehub.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceYearEndSectionDTO {
	private String title;
	private String formattedTotal;
	private double total;
	private boolean inflow;
}
