package com.financehub.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceYearEndPackDTO {
	private int year;
	private String formattedNet;
	private double net;
	private List<FinanceYearEndSectionDTO> sections = new ArrayList<>();
}
