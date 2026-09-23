package com.financehub.controller;

import com.financehub.dtos.*;
import com.financehub.services.FinanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/api/finance")
public class FinanceController {

	private static final List<String> ACCOUNT_TYPES = List.of(
			"SAVINGS", "CURRENT", "CASH", "WALLET", "FD", "INVESTMENT");

	private static final List<String> BANK_NAMES = List.of(
			"State Bank of India",
			"Union Bank of India",
			"ICICI Bank",
			"HDFC Bank",
			"Axis Bank",
			"Kotak Mahindra Bank",
			"Bank of Baroda",
			"Canara Bank",
			"Punjab National Bank",
			"Indian Bank",
			"Yes Bank",
			"IDFC First Bank",
			"Other");

	private final FinanceService financeService;

	public FinanceController(FinanceService financeService) {
		this.financeService = financeService;
	}

	@GetMapping("/reports")
	public String hub() {
		return "views/finance/financeHub";
	}

	/* Accounts */
	@GetMapping("/accounts/add")
	public String accountForm(@RequestParam(value = "id", required = false) Long id, Model model) {
		model.addAttribute("account", id != null ? financeService.getAccountDto(id) : new FinanceAccountDTO());
		model.addAttribute("accountTypes", ACCOUNT_TYPES);
		model.addAttribute("bankNames", BANK_NAMES);
		return "views/finance/addAccount";
	}

	@PostMapping("/accounts/save")
	public String saveAccount(@ModelAttribute FinanceAccountDTO account, RedirectAttributes ra) {
		try {
			financeService.saveAccount(account);
			ra.addFlashAttribute("successMessage", "Bank account saved successfully.");
		} catch (IllegalArgumentException e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/api/finance/accounts/add" + (account.getId() != null ? "?id=" + account.getId() : "");
	}

	@GetMapping("/accountsReport")
	public String accountsReport(Model model) {
		model.addAttribute("accounts", financeService.listAccounts());
		return "views/finance/accountsReport";
	}

	@DeleteMapping("/deleteAccount")
	public ResponseEntity<String> deleteAccount(@RequestParam("id") Long id) {
		try {
			financeService.deleteAccount(id);
			return ResponseEntity.ok("success");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	/* Credit cards */
	@GetMapping("/cards/add")
	public String cardForm(@RequestParam(value = "id", required = false) Long id, Model model) {
		FinanceCreditCardDTO card = id != null ? financeService.getCreditCardDto(id) : new FinanceCreditCardDTO();
		model.addAttribute("card", card);
		model.addAttribute("bankNames", BANK_NAMES);
		model.addAttribute("expiryMonths", monthNames());
		model.addAttribute("expiryYears", cardExpiryYears(card.getExpiryYear()));
		return "views/finance/addCreditCard";
	}

	@PostMapping("/cards/save")
	public String saveCard(@ModelAttribute FinanceCreditCardDTO card, RedirectAttributes ra) {
		try {
			financeService.saveCreditCard(card);
			ra.addFlashAttribute("successMessage", "Credit card saved successfully.");
		} catch (IllegalArgumentException e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/api/finance/cards/add" + (card.getId() != null ? "?id=" + card.getId() : "");
	}

	@GetMapping("/creditCardsReport")
	public String cardsReport(Model model) {
		model.addAttribute("cards", financeService.listCreditCards());
		return "views/finance/creditCardsReport";
	}

	@DeleteMapping("/deleteCreditCard")
	public ResponseEntity<String> deleteCard(@RequestParam("id") Long id) {
		try {
			financeService.deleteCreditCard(id);
			return ResponseEntity.ok("success");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	/* Credit card monthly bills */
	@GetMapping("/cardBills/add")
	public String cardBillForm(@RequestParam(value = "id", required = false) Long id, Model model) {
		FinanceCreditCardBillDTO bill = id != null
				? financeService.getCardBillDto(id)
				: financeService.newCardBillDefaults();
		if (bill.getCardId() != null && bill.getBillMonth() != null && bill.getBillYear() != null) {
			try {
				bill.setBillingDate(financeService.resolveBillingDate(bill.getCardId(), bill.getBillMonth(), bill.getBillYear()));
				bill.setDueDate(financeService.resolveDueDate(bill.getCardId(), bill.getBillMonth(), bill.getBillYear()));
			} catch (IllegalArgumentException ignored) {
				// Card is missing a billing or due day; the form stays blank until the card is updated.
			}
		}
		model.addAttribute("bill", bill);
		model.addAttribute("cards", financeService.listCreditCards());
		model.addAttribute("monthAbbreviations", monthAbbreviations());
		model.addAttribute("years", financeService.reportYears());
		return "views/finance/addCardBill";
	}

	@GetMapping(value = "/cardBills/periodSummary", produces = "application/json")
	@ResponseBody
	public ResponseEntity<String> cardBillPeriodSummary(
			@RequestParam("cardId") Long cardId,
			@RequestParam("billMonth") Integer billMonth,
			@RequestParam("billYear") Integer billYear) {
		try {
			return ResponseEntity.ok(financeService.cardBillPeriodSummaryJson(cardId, billMonth, billYear));
		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("{\"followUp\":false}");
		}
	}

	@PostMapping("/cardBills/save")
	public String saveCardBill(@ModelAttribute FinanceCreditCardBillDTO bill, RedirectAttributes ra) {
		try {
			financeService.saveCardBill(bill);
			ra.addFlashAttribute("successMessage",
					bill.getId() != null ? "Monthly bill updated successfully." : "Payment saved successfully.");
		} catch (IllegalArgumentException e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/api/finance/cardBills/add" + (bill.getId() != null ? "?id=" + bill.getId() : "");
	}

	@GetMapping("/cardBillsReport")
	public String cardBillsReport(@RequestParam(value = "cardId", required = false) Long cardId, Model model) {
		model.addAttribute("billYearGroups", financeService.listCardBillsByYear(cardId));
		model.addAttribute("cards", financeService.listCreditCards());
		model.addAttribute("selectedCardId", cardId);
		model.addAttribute("currentYear", Year.now().getValue());
		return "views/finance/cardBillsReport";
	}

	@DeleteMapping("/deleteCardBill")
	public ResponseEntity<String> deleteCardBill(@RequestParam("id") Long id) {
		try {
			financeService.deleteCardBill(id);
			return ResponseEntity.ok("success");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	/* Insurance */
	@GetMapping("/insurance/add")
	public String insuranceForm(@RequestParam(value = "id", required = false) Long id, Model model) {
		FinanceInsuranceDTO dto = id != null ? financeService.getInsuranceDto(id) : new FinanceInsuranceDTO();
		if (dto.getNextDueDate() == null) {
			dto.setNextDueDate(LocalDate.now().plusMonths(1));
		}
		if (dto.getPremiumFrequency() == null) {
			dto.setPremiumFrequency("YEARLY");
		}
		if (dto.getPolicyType() == null) {
			dto.setPolicyType("LIFE");
		}
		model.addAttribute("policy", dto);
		model.addAttribute("policyTypes", List.of("LIFE", "HEALTH", "VEHICLE", "TERM", "ENDOWMENT", "OTHER"));
		model.addAttribute("frequencies", List.of("MONTHLY", "QUARTERLY", "HALF_YEARLY", "YEARLY"));
		return "views/finance/addInsurance";
	}

	@PostMapping("/insurance/save")
	public String saveInsurance(@ModelAttribute FinanceInsuranceDTO policy, RedirectAttributes ra) {
		try {
			financeService.saveInsurance(policy);
			ra.addFlashAttribute("successMessage", "Insurance policy saved.");
		} catch (IllegalArgumentException e) {
			ra.addFlashAttribute("errorMessage", e.getMessage());
		}
		return "redirect:/api/finance/insurance/add" + (policy.getId() != null ? "?id=" + policy.getId() : "");
	}

	@GetMapping("/insurance/markPaid")
	public String markInsurancePaid(@RequestParam("id") Long id, Model model) {
		try {
			financeService.markInsurancePaid(id);
			model.addAttribute("successMessage", "Marked paid — next due date advanced.");
		} catch (IllegalArgumentException e) {
			model.addAttribute("errorMessage", e.getMessage());
		}
		model.addAttribute("policies", financeService.listInsurance());
		return "views/finance/insuranceReport";
	}

	@GetMapping("/insuranceReport")
	public String insuranceReport(Model model) {
		model.addAttribute("policies", financeService.listInsurance());
		return "views/finance/insuranceReport";
	}

	@DeleteMapping("/deleteInsurance")
	public ResponseEntity<String> deleteInsurance(@RequestParam("id") Long id) {
		try {
			financeService.deleteInsurance(id);
			return ResponseEntity.ok("success");
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		}
	}

	/* Cash flow */
	@GetMapping("/cashFlowReport")
	public String cashFlowReport(@RequestParam(value = "year", required = false) Integer year, Model model) {
		int y = year != null ? year : Year.now().getValue();
		model.addAttribute("report", financeService.buildCashFlow(y));
		model.addAttribute("years", financeService.reportYears());
		model.addAttribute("selectedYear", y);
		return "views/finance/cashFlowReport";
	}

	/* Year-end pack */
	@GetMapping("/yearEndPackReport")
	public String yearEndPack(@RequestParam(value = "year", required = false) Integer year, Model model) {
		int y = year != null ? year : Year.now().getValue();
		model.addAttribute("pack", financeService.buildYearEndPack(y));
		model.addAttribute("years", financeService.reportYears());
		model.addAttribute("selectedYear", y);
		return "views/finance/yearEndPackReport";
	}

	private List<String> monthAbbreviations() {
		List<String> months = new ArrayList<>();
		String[] names = new DateFormatSymbols().getMonths();
		for (int i = 0; i < 12; i++) {
			months.add(names[i].substring(0, 3));
		}
		return months;
	}

	private List<String> monthNames() {
		List<String> months = new ArrayList<>();
		String[] names = new DateFormatSymbols().getMonths();
		for (int i = 0; i < 12; i++) {
			months.add(names[i]);
		}
		return months;
	}

	private List<Integer> cardExpiryYears(Integer selectedYear) {
		int current = Year.now().getValue();
		int start = current - 5;
		int end = current + 20;
		if (selectedYear != null) {
			start = Math.min(start, selectedYear);
			end = Math.max(end, selectedYear);
		}
		List<Integer> years = new ArrayList<>();
		for (int y = start; y <= end; y++) {
			years.add(y);
		}
		return years;
	}
}
