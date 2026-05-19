package com.financehub.controller;

import com.financehub.dtos.CategoryReorderRequest;
import com.financehub.dtos.ExpenseReportDTO;
import com.financehub.dtos.ExpenseRequest;
import com.financehub.dtos.ExpensesCategoriesDTO;
import com.financehub.entities.ExpenseCategories;
import com.financehub.services.ExpensesService;
import com.financehub.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.YearMonth;
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
        int nextSort = categories.stream().mapToInt(ExpenseCategories::getSortOrder).max().orElse(0) + 1;
        model.addAttribute("nextSortOrder", nextSort);
    }
    @GetMapping("/categories")
    public String showCategories(Model model) {
        addCategoryListToModel(model);
        return "views/expenses/manageExpenseCategories";
    }
    @PostMapping("/categorySave")
    public String addCategory(@ModelAttribute ExpensesCategoriesDTO expensesCategoriesDTO, Model model) {
        try {
            expensesService.saveCategory(expensesCategoriesDTO);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("categoryError", ex.getMessage());
            addCategoryListToModel(model);
            return "views/expenses/manageExpenseCategories";
        }
        addCategoryListToModel(model);
        return "views/expenses/manageExpenseCategories";
    }

    @PostMapping("/categoryDelete")
    public String deleteCategory(@RequestParam int id,Model model) {
        expensesService.deleteCategoryByID(id);
        addCategoryListToModel(model);
        return "views/expenses/manageExpenseCategories";
    }

    @PostMapping("/categoryReorder")
    @ResponseBody
    public ResponseEntity<Void> reorderCategories(@RequestBody CategoryReorderRequest body) {
        if (body == null || body.getOrderedIds() == null || body.getOrderedIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        expensesService.reorderCategories(body.getOrderedIds());
        return ResponseEntity.ok().build();
    }
    @GetMapping("/add")
    public String addExpenses(@RequestParam(value = "id", required = false) Long id,
                              @RequestParam(value = "type", required = false) String expenseType,
                              Model model) {
        prepareAddExpensesModel(model, id, expenseType, null);
        return "views/expenses/addExpenses";
    }

    @PostMapping("/save")
    public String saveExpenses(@ModelAttribute ExpenseRequest expenseRequest, Model model) {
        boolean updated = expenseRequest.getExpenseId() != null;
        try {
            expensesService.saveExpense(expenseRequest);
            model.addAttribute("message", updated ? "Expenses Updated Successfully!" : "Expenses Saved Successfully!");
            prepareAddExpensesModel(model, updated ? expenseRequest.getExpenseId() : null, expenseRequest.getExpenseType(), null);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to save expenses: " + e.getMessage());
            prepareAddExpensesModel(model, expenseRequest.getExpenseId(), expenseRequest.getExpenseType(), expenseRequest);
        }
        return "views/expenses/addExpenses";
    }

    private void prepareAddExpensesModel(Model model, Long id, String expenseType, ExpenseRequest submitted) {
        model.addAttribute("categories", expensesService.getEnabledCategories(userService.getUserId()));

        ExpenseReportDTO dto = null;
        if (id != null) {
            dto = expensesService.getExpenseDetailsById(id);
            if (dto != null && expenseType != null) {
                dto.setExpenseType(expenseType);
            }
        } else if (submitted != null && submitted.getMonth() != null && !submitted.getMonth().isBlank()) {
            dto = buildDtoFromSubmitted(submitted);
        }

        if (dto == null) {
            return;
        }

        model.addAttribute("expenseDetails", dto);
        String type = dto.getExpenseType() != null ? dto.getExpenseType() : "plan";
        Map<Integer, Double> amounts = "plan".equalsIgnoreCase(type)
                ? dto.getPlannedExpenses()
                : dto.getActualExpenses();
        double totalExpense = amounts != null
                ? amounts.values().stream().mapToDouble(Double::doubleValue).sum()
                : 0.0;
        model.addAttribute("totalExpense", totalExpense);
    }

    private ExpenseReportDTO buildDtoFromSubmitted(ExpenseRequest submitted) {
        YearMonth ym = YearMonth.parse(submitted.getMonth());
        String type = submitted.getExpenseType() != null ? submitted.getExpenseType() : "plan";
        Map<Integer, Double> amounts = submitted.getExpenses() != null ? submitted.getExpenses() : Map.of();
        Map<Integer, Double> planned = "plan".equalsIgnoreCase(type) ? amounts : null;
        Map<Integer, Double> actual = "plan".equalsIgnoreCase(type) ? null : amounts;
        int rowId = submitted.getExpenseId() != null ? submitted.getExpenseId().intValue() : 0;
        ExpenseReportDTO dto = new ExpenseReportDTO(
                rowId,
                ym.getYear(),
                ym.getMonthValue(),
                "",
                planned,
                actual,
                0,
                0,
                ""
        );
        dto.setExpenseType(type);
        return dto;
    }

    @GetMapping("/manageExpenses")
    public String manageExpenses(Model model){
        Set<Integer> years = expensesService.getDistinctExpenseYearsForUser(userService.getUserId());
        model.addAttribute("years", years);
        return "views/expenses/manageExpenses";
    }
    @GetMapping("/manageReport")
    public String getManageExpenseReport(@RequestParam("year") int year, Model model) {
        List<ExpenseReportDTO> reports = expensesService.getExpenseReport(year);
        model.addAttribute("reports", reports);
        model.addAttribute("year",year);
        return "views/expenses/manageExpenseReport";
    }
    @GetMapping("/yearWiseActualPlan")
    public String yearWise(Model model){
        Set<Integer> years = expensesService.getDistinctExpenseYearsForUser(userService.getUserId());
        model.addAttribute("years", years);
        return "views/expenses/yearWiseActualPlan";
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
        return "views/expenses/yearWiseActualPlanReport";
    }
    @GetMapping("/yearSummaryReport")
    public String getYearSummaryReport(@RequestParam("year") int year, Model model) {
        Map<String, Object> data = expensesService.getYearWiseExpenseData(year);
        model.addAllAttributes(data);
        return "views/expenses/yearSummaryReport";
    }
    @DeleteMapping("/deleteAmount")
    public ResponseEntity<String> deleteExpense(@RequestParam Long id, @RequestParam String type) {
        expensesService.deleteExpense(id, type);
        return ResponseEntity.ok("success");
    }

}
