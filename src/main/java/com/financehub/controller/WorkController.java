package com.financehub.controller;

import com.financehub.dtos.CompanyDTO;
import com.financehub.dtos.SalaryDTO;
import com.financehub.services.WorkService;
import com.financehub.utils.FormatterUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.OutputStream;
import java.text.DateFormatSymbols;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/api/work")
public class WorkController {
    @Autowired
    private WorkService workService;
    @Autowired
    private FormatterUtils formatterUtils;

    @GetMapping("/addExperience")
    public String addCompanyForm(Model model, @RequestParam(value = "action", required = false, defaultValue = "add") String action,
                                 @RequestParam(value = "id", required = false) Long id) {
        if ("edit".equals(action) && id != null) {
            CompanyDTO company = workService.getExperienceById(id);
            model.addAttribute("company", company);
        }
        model.addAttribute("action", action);
        return "inputs/addExperience";
    }

    @PostMapping("/addExperience")
    public String addCompany(@ModelAttribute CompanyDTO companyDTO, RedirectAttributes redirectAttributes) {
        if (companyDTO.getCompanyId() != null) {
            CompanyDTO existingCompany = workService.getExperienceById(companyDTO.getCompanyId());
            if (existingCompany == null) {
                redirectAttributes.addFlashAttribute("error", "Experience not found.");
                return "redirect:/api/work/addExperience";
            }
            boolean isDateChanged = false;
            if (!companyDTO.getFromDate().equals(existingCompany.getFromDate()) ||
                    (companyDTO.getToDate() != null ? !companyDTO.getToDate().equals(existingCompany.getToDate()) : existingCompany.getToDate() != null)) {
                isDateChanged = true;
            }
            if (isDateChanged) {
                String datesCheck = workService.checkExpDates(companyDTO, redirectAttributes);
                if (datesCheck != null) return datesCheck;
            }
        } else {
            String datesCheck = workService.checkExpDates(companyDTO, redirectAttributes);
            if (datesCheck != null) return datesCheck;
        }

        workService.addCompany(companyDTO);
        if (companyDTO.getCompanyId() != null) {
            redirectAttributes.addFlashAttribute("successMessage", "Company updated successfully: " + companyDTO.getCompanyName());
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Company added successfully: " + companyDTO.getCompanyName());
        }
        return "redirect:/api/work/addExperience";
    }

    @GetMapping("/addSalary")
    public String addSalaryForm(Model model, @RequestParam(value = "action", required = false, defaultValue = "add") String action,
                                @RequestParam(value = "id", required = false) Long id) {

        if ("edit".equals(action) && id != null) {
            SalaryDTO salary = workService.getSalaryById(id);
            model.addAttribute("salary", salary);
        }
        List<String> monthAbbreviations = new ArrayList<>();
        String[] months = new DateFormatSymbols().getMonths();
        for (int i = 0; i < 12; i++) {
            monthAbbreviations.add(months[i].substring(0, 3));
        }
        List<CompanyDTO> companies = workService.getCompaniesByUserName();
        model.addAttribute("companies", companies);
        model.addAttribute("monthAbbreviations", monthAbbreviations);
        model.addAttribute("action", action);
        return "inputs/addSalary";
    }
    @PostMapping("/addSalary")
    public String salaryFormAction(@ModelAttribute SalaryDTO salaryDTO,RedirectAttributes redirectAttributes) {
        boolean isUpdate = salaryDTO.getSalaryId() != null;
        workService.addSalary(salaryDTO);
        if (isUpdate) {
            redirectAttributes.addFlashAttribute("successMessage", "Salary updated successfully");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Salary added successfully");
        }
        return "redirect:/api/work/addSalary";
    }

    @GetMapping("/workReport")
    public String getWorkReport(Model model) {
        List<CompanyDTO> companies = workService.getCompaniesByUserName();
        model.addAttribute("companies", companies);
        return "reports/professionalReport";
    }

    @GetMapping("/expReport")
    public String getCompanyReport(Model model) {
        List<CompanyDTO> companies = workService.getCompaniesByUserName();

        for (CompanyDTO company : companies) {
            if (company.getFromDate() != null) {
                company.setFormattedFromDate(formatterUtils.formatDate(company.getFromDate()));
            }
            if (company.getToDate() != null) {
                company.setFormattedToDate(formatterUtils.formatDate(company.getToDate()));
            }
        }
        String totExperience = formatterUtils.getTotalExp(companies);
        model.addAttribute("companies", companies);
        model.addAttribute("totalExp",totExperience);
        return "reports/experienceReport";
    }
@GetMapping("/salaryReport")
public String getSalaryReport(Model model) {
    Map<Integer, List<SalaryDTO>> salaries = workService.getAllSalaries();
    Map<Integer, String> yearWiseTotals = new HashMap<>();
    double totalSum = 0.0;

    for (Map.Entry<Integer, List<SalaryDTO>> entry : salaries.entrySet()) {
        Integer year = entry.getKey();
        List<SalaryDTO> salaryList = entry.getValue();
        double yearTotal = 0.0;

        for (SalaryDTO salary : salaryList) {
            if (salary.getDateCredited() != null) {
                salary.setFormattedDateCredited(formatterUtils.formatDate(salary.getDateCredited()));
            }
            if (salary.getSalaryAmount() != null) {
                double salaryAmount = salary.getSalaryAmount();
                salary.setFormattedSalaryAmount(formatterUtils.formatInIndianStyle(salaryAmount));
                yearTotal += salaryAmount;
            }
            salary.setMonthName(formatterUtils.getMonthName(salary.getMonth()));
        }
        yearWiseTotals.put(year, formatterUtils.formatInIndianStyle(yearTotal));
        totalSum += yearTotal;
    }

    model.addAttribute("salaries", salaries);
    model.addAttribute("yearWiseTotals", yearWiseTotals);
    model.addAttribute("totalSum", formatterUtils.formatInIndianStyle(totalSum));
    return "reports/salaryReport";
}
@DeleteMapping("/deleteExperience")
public ResponseEntity<String> deleteCompany(@RequestParam("id") Long id) {
    CompanyDTO company = workService.getExperienceById(id);
    if (company == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("error");
    }
    boolean hasSalaries = workService.hasSalariesForCompany(id);
    if (hasSalaries) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("Please delete salary entries for this company first.");
    }
    workService.deleteCompany(id);
    return ResponseEntity.ok("success");
}
@DeleteMapping("/deleteSalary")
public ResponseEntity<String> deleteSalary(@RequestParam("id") Long id) {
        SalaryDTO salary = workService.getSalaryById(id);
        if (salary == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("error");
        }
        workService.deleteSalary(id);
        return ResponseEntity.ok("success");
    }
}
