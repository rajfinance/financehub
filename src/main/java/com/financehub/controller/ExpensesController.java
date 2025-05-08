package com.financehub.controller;

import com.financehub.dtos.ExpenseReportDTO;
import com.financehub.dtos.ExpenseRequest;
import com.financehub.dtos.ExpensesCategoriesDTO;
import com.financehub.dtos.OwnerDTO;
import com.financehub.entities.ExpenseCategories;
import com.financehub.entities.Owner;
import com.financehub.services.ExpensesService;
import com.financehub.services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.DecimalFormat;
import java.util.*;

@Controller
@RequestMapping("/api/expenses")
public class ExpensesController {
    @Autowired
    public ExpensesService expensesService;
   @Autowired
   public UserService userService;
    private void addCategoryListToModel(Model model) {
        List<ExpenseCategories> categories = expensesService.getAllCategories(userService.getUserId());
        model.addAttribute("categories", categories);
    }
    @GetMapping("/categories")
    public String showCategories(Model model) {
        addCategoryListToModel(model);
        return "expenses/manageExpenseCategories";
    }
    @PostMapping("/categorySave")
    public String addCategory(@ModelAttribute ExpensesCategoriesDTO expensesCategoriesDTO,Model model) {
        expensesService.saveCategory(expensesCategoriesDTO);
        addCategoryListToModel(model);
        return "expenses/manageExpenseCategories";
    }

    @PostMapping("/categoryDelete")
    public String deleteCategory(@RequestParam int id,Model model) {
        expensesService.deleteCategoryByID(id);
        addCategoryListToModel(model);
        return "expenses/manageExpenseCategories";
    }
    @GetMapping("/add")
    public String addExpenses(@RequestParam(value = "id", required = false) Long id,@RequestParam(value="type", required = false) String expenseType, Model model) {
        double totalExpense =0;
        List<ExpenseCategories> categories = expensesService.getEnabledCategories(userService.getUserId());
        model.addAttribute("categories", categories);
        ExpenseReportDTO dto = expensesService.getExpenseDetailsById(id);
        if (dto != null) {
            dto.setExpenseType(expenseType);
            model.addAttribute("expenseDetails", dto);
            if(expenseType.equalsIgnoreCase("plan")) {
                totalExpense = dto.getPlannedExpenses().values().stream().mapToDouble(Double::doubleValue).sum();
            }
            else{
                totalExpense = dto.getActualExpenses().values().stream().mapToDouble(Double::doubleValue).sum();
            }
            model.addAttribute("totalExpense", totalExpense);
        }
        return "expenses/addExpenses";
    }

    @PostMapping("/save")
    public String saveExpenses(@ModelAttribute ExpenseRequest expenseRequest, RedirectAttributes redirectAttributes) {
        try {
            expensesService.saveExpense(expenseRequest);
            if(expenseRequest.getExpenseId()!=null){
                redirectAttributes.addFlashAttribute("message", "Expenses Updated Successfully!");
            }else {
                redirectAttributes.addFlashAttribute("message", "Expenses Saved Successfully!");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save expenses: " + e.getMessage());
        }
        return "redirect:/api/expenses/add";
    }

    @GetMapping("/manageExpenses")
    public String manageExpenses(Model model){
        Set<Integer> years = expensesService.getDistinctExpenseYearsForUser(userService.getUserId());
        model.addAttribute("years", years);
        return "expenses/manageExpenses";
    }
    @GetMapping("/manageReport")
    public String getManageExpenseReport(@RequestParam("year") int year, Model model) {
        List<ExpenseReportDTO> reports = expensesService.getExpenseReport(year);
        model.addAttribute("reports", reports);
        model.addAttribute("year",year);
        return "expenses/manageExpenseReport";
    }
    @GetMapping("/yearWiseActualPlan")
    public String yearWise(Model model){
        Set<Integer> years = expensesService.getDistinctExpenseYearsForUser(userService.getUserId());
        model.addAttribute("years", years);
        return "expenses/yearWiseActualPlan";
    }
    @GetMapping("/yearWiseActualPlanReport")
    public String getYearWiseActualPlan(@RequestParam("year") int year, Model model) {
        List<ExpenseReportDTO> reportData = expensesService.getYearlyPlanActual(userService.getUserId(), year);

        Map<String, String> monthlyPlanTotalMap = new HashMap<>();
        Map<String, String> monthlyActualTotalMap = new HashMap<>();
        DecimalFormat df = new DecimalFormat("#,##0");
        for (int i = 1; i <= 12; i++) {
            monthlyPlanTotalMap.put(String.valueOf(i), "0");
            monthlyActualTotalMap.put(String.valueOf(i), "0");
        }

        for (ExpenseReportDTO data : reportData) {
            String month = String.valueOf(data.getMonth());
            int planAmount = (int) Math.floor(data.getPlanAmount());
            int actualAmount = (int) Math.floor(data.getActualAmount());

            int currentPlanTotal = monthlyPlanTotalMap.containsKey(month)
                    ? Integer.parseInt(monthlyPlanTotalMap.get(month).replace(",", ""))
                    : 0;
            int currentActualTotal = monthlyActualTotalMap.containsKey(month)
                    ? Integer.parseInt(monthlyActualTotalMap.get(month).replace(",", ""))
                    : 0;

            monthlyPlanTotalMap.put(month, df.format(currentPlanTotal + planAmount));
            monthlyActualTotalMap.put(month, df.format(currentActualTotal + actualAmount));
        }
        model.addAttribute("monthlyPlanTotalMap", monthlyPlanTotalMap);
        model.addAttribute("monthlyActualTotalMap", monthlyActualTotalMap);
        model.addAttribute("reportData", reportData);
        model.addAttribute("year", year);
        return "expenses/yearWiseActualPlanReport";
    }
    @GetMapping("/yearSummaryReport")
    public String getYearSummaryReport(@RequestParam("year") int year, Model model) {
        Map<String, Object> data = expensesService.getYearWiseExpenseData(year);
        model.addAllAttributes(data);
        return "expenses/yearSummaryReport";
    }
    @DeleteMapping("/deleteAmount")
    public ResponseEntity<String> deleteExpense(@RequestParam Long id, @RequestParam String type) {
        expensesService.deleteExpense(id, type);
        return ResponseEntity.ok("success");
    }

}
