package com.financehub.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceCashFlowReportDTO {
	private int year;
	private String formattedInflow;
	private String formattedOutflow;
	private String formattedNet;
	private double inflow;
	private double outflow;
	private double net;
	private List<FinanceCashFlowLineDTO> lines = new ArrayList<>();
}
