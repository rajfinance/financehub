package com.financehub.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceAlertDTO {
	private String type;
	private String title;
	private String detail;
	/** info | warn | danger */
	private String severity;
}
