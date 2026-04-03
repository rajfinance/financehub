package com.financehub.controller;

import com.financehub.dtos.LoanDTO;
import com.financehub.services.LoanService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/api/loan")
public class LoanController {

	private static final List<String> BANK_NAMES = List.of(
			"Axis Bank", "ICICI Bank", "HDFC Bank", "State Bank of India", "Kotak Mahindra Bank", "Other");

	private final LoanService loanService;

	public LoanController(LoanService loanService) {
		this.loanService = loanService;
	}

	@GetMapping("/addLoan")
	public String addLoanForm(Model model) {
		model.addAttribute("loan", new LoanDTO());
		model.addAttribute("bankNames", BANK_NAMES);
		return "views/loan/addLoan";
	}

	@PostMapping("/addLoan")
	public String addLoan(@ModelAttribute("loan") LoanDTO loanDTO, RedirectAttributes redirectAttributes) {
		try {
			loanService.addLoanFromDto(loanDTO);
			redirectAttributes.addFlashAttribute("successMessage", "Loan saved successfully.");
		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/api/loan/addLoan";
	}
}
