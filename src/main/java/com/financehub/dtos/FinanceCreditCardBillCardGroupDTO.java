package com.financehub.dtos;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FinanceCreditCardBillCardGroupDTO {
	private Long cardId;
	private String cardName;
	private List<FinanceCreditCardBillDTO> bills = new ArrayList<>();
	private String formattedTotalBill;
	private String formattedTotalPaid;
	private String formattedTotalInterest;
}
