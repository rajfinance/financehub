package com.financehub.controller;

import com.financehub.dtos.LoanDTO;
import com.financehub.dtos.LoanBankEmiProjectionReportDTO;
import com.financehub.dtos.LoanEmiPaymentDTO;
import com.financehub.dtos.LoanSummaryDTO;
import com.financehub.services.LoanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/api/loan")
public class LoanController {

    private static final List<String> BANK_NAMES = List.of(
            "Axis Bank", "ICICI Bank", "HDFC Bank", "State Bank of India", "Kotak Mahindra Bank", "Other");

    private static final List<String> LOAN_TYPES = List.of(
            "Personal", "Home", "Vehicle", "Education", "Business", "Gold", "Other");

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/addLoan")
    public String addLoanForm(Model model) {
        model.addAttribute("loan", new LoanDTO());
        model.addAttribute("bankNames", BANK_NAMES);
        model.addAttribute("loanTypes", LOAN_TYPES);
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

    @GetMapping("/recordEmi")
    public String recordEmiForm(@RequestParam(value = "id", required = false) Long id,
                                @RequestParam(value = "loanId", required = false) Long loanId,
                                @RequestParam(value = "emiNumber", required = false) Integer emiNumber,
                                Model model) {
        List<LoanSummaryDTO> loans = loanService.getLoansForCurrentUser();
        model.addAttribute("loans", loans);
        if (id != null) {
            model.addAttribute("emiPayment", loanService.getEmiPaymentById(id));
        } else {
            LoanEmiPaymentDTO dto = new LoanEmiPaymentDTO();
            if (loanId != null) {
                dto.setLoanId(loanId);
                dto.setEmiNumber(emiNumber);
                loanService.prefillEmiPayment(dto);
            }
            model.addAttribute("emiPayment", dto);
        }
        return "views/loan/recordEmi";
    }

    @PostMapping("/recordEmi")
    public String saveEmiPayment(@ModelAttribute("emiPayment") LoanEmiPaymentDTO dto,
                                 RedirectAttributes redirectAttributes) {
        try {
            loanService.saveEmiPayment(dto);
            redirectAttributes.addFlashAttribute("successMessage", "EMI payment recorded successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/api/loan/recordEmi";
    }

    @GetMapping("/reports")
    public String loanReportsHub() {
        return "views/loan/loanReport";
    }

    @GetMapping("/loansReport")
    public String loansSummaryReport(Model model) {
        model.addAttribute("loans", loanService.getLoansForCurrentUser());
        return "views/loan/loansSummaryReport";
    }

    @GetMapping("/loanEmiReport")
    public String loanEmiScheduleReport(@RequestParam(value = "year", required = false) Integer year,
                                        @RequestParam(value = "loanId", required = false) Long loanId,
                                        Model model) {
        int selectedYear = year != null ? year : LocalDate.now().getYear();
        model.addAttribute("loans", loanService.getLoansForCurrentUser());
        model.addAttribute("selectedLoanId", loanId);
        model.addAttribute("scheduleGroups", loanService.getEmiScheduleGroups(selectedYear, loanId));
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("years", loanService.getScheduleYearsForUser());
        model.addAttribute("yearTotal", loanService.getFormattedYearTotal(selectedYear, loanId));
        model.addAttribute("yearPendingTotal", loanService.getFormattedYearPendingAmount(selectedYear, loanId));
        return "views/loan/loanEmiScheduleReport";
    }

    @GetMapping("/loanBankProjectionReport")
    public String loanBankProjectionReport(Model model) {
        LoanBankEmiProjectionReportDTO report = loanService.getBankNextMonthProjectionReport();
        model.addAttribute("projectionReport", report);
        return "views/loan/loanBankProjectionReport";
    }

    @DeleteMapping("/deleteLoan")
    public ResponseEntity<String> deleteLoan(@RequestParam("id") Long id) {
        try {
            loanService.deleteLoan(id);
            return ResponseEntity.ok("success");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/deleteEmiPayment")
    public ResponseEntity<String> deleteEmiPayment(@RequestParam("id") Long id) {
        try {
            loanService.deleteEmiPayment(id);
            return ResponseEntity.ok("success");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
