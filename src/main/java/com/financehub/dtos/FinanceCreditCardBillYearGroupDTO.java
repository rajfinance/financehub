package com.financehub.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceCreditCardBillYearGroupDTO {
	private Integer year;
	private List<FinanceCreditCardBillCardGroupDTO> cardGroups = new ArrayList<>();
	private String formattedTotalBill;
	private String formattedTotalPaid;
	private String formattedTotalInterest;
}
