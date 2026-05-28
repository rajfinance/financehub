package com.financehub.controller;

import com.financehub.dtos.LoanDTO;
import com.financehub.dtos.LoanBankEmiProjectionReportDTO;
import com.financehub.dtos.LoanEmiPaymentDTO;
import com.financehub.dtos.LoanPreClosureDTO;
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
        model.addAttribute("isEdit", false);
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

    @GetMapping("/editLoan")
    public String editLoanForm(@RequestParam("loanId") Long loanId, Model model) {
        model.addAttribute("loan", loanService.getLoanForEdit(loanId));
        model.addAttribute("bankNames", BANK_NAMES);
        model.addAttribute("loanTypes", LOAN_TYPES);
        model.addAttribute("isEdit", true);
        return "views/loan/addLoan";
    }

    @PostMapping("/updateLoan")
    public String updateLoan(@ModelAttribute("loan") LoanDTO loanDTO, RedirectAttributes redirectAttributes) {
        try {
            loanService.updateLoanFromDto(loanDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Loan updated successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/api/loan/editLoan?loanId=" + loanDTO.getId();
    }

    @GetMapping("/recordEmi")
    public String recordEmiForm(@RequestParam(value = "id", required = false) Long id,
                                @RequestParam(value = "loanId", required = false) Long loanId,
                                @RequestParam(value = "emiNumber", required = false) Integer emiNumber,
                                Model model) {
        List<LoanSummaryDTO> loans = loanService.getLoansForCurrentUser();
        model.addAttribute("loans", loans);
        if (id != null) {
            LoanEmiPaymentDTO dto = loanService.getEmiPaymentById(id);
            loanService.prefillEmiPayment(dto);
            if (dto.getPreClosureType() == null) {
                dto.setPreClosureType("FULL");
            }
            model.addAttribute("emiPayment", dto);
        } else {
            LoanEmiPaymentDTO dto = new LoanEmiPaymentDTO();
            dto.setPreClosureType("FULL");
            dto.setPreClosureSelected(Boolean.FALSE);
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
    public String loanEmiScheduleReport(@RequestParam(value = "year", required = false) String year,
                                        @RequestParam(value = "loanId", required = false) Long loanId,
                                        Model model) {
        Integer selectedYear;
        if (year == null || year.isBlank()) {
            selectedYear = LocalDate.now().getYear();
        } else if ("all".equalsIgnoreCase(year)) {
            selectedYear = loanId != null ? null : LocalDate.now().getYear();
        } else {
            selectedYear = Integer.valueOf(year);
        }
        model.addAttribute("loans", loanService.getLoansForCurrentUser());
        model.addAttribute("selectedLoanId", loanId);
        model.addAttribute("scheduleGroups", loanService.getEmiScheduleGroups(selectedYear, loanId));
        model.addAttribute("selectedYear", selectedYear);
        model.addAttribute("selectedYearLabel", selectedYear == null ? "All Years" : selectedYear.toString());
        model.addAttribute("allowAllYears", loanId != null);
        model.addAttribute("years",
                loanId != null ? loanService.getScheduleYearsForLoan(loanId) : loanService.getScheduleYearsForUser());
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

    @GetMapping("/preCloseLoan")
    public String preCloseLoanForm(@RequestParam("loanId") Long loanId, Model model) {
        LoanSummaryDTO loan = loanService.getLoanSummaryById(loanId);
        LoanPreClosureDTO preClosure = loanService.getPreClosureDetails(loanId);
        model.addAttribute("loan", loan);
        model.addAttribute("preClosure", preClosure);
        model.addAttribute("remainingEmiCount",
                loanService.getRemainingEmiCountFromDate(loanId, preClosure.getPreClosureDate()));
        model.addAttribute("remainingPendingAmount",
                loanService.getFormattedRemainingPendingAmountFromDate(loanId, preClosure.getPreClosureDate()));
        return "views/loan/preCloseLoan";
    }

    @PostMapping("/preCloseLoan")
    public String preCloseLoan(@ModelAttribute("preClosure") LoanPreClosureDTO dto,
                               RedirectAttributes redirectAttributes) {
        try {
            loanService.savePreClosure(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Loan pre-closure saved successfully.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/api/loan/preCloseLoan?loanId=" + dto.getLoanId();
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
