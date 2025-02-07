package com.financehub.controller;

import com.financehub.dtos.ExpensesCategoriesDTO;
import com.financehub.dtos.OwnerDTO;
import com.financehub.entities.ExpenseCategories;
import com.financehub.entities.Owner;
import com.financehub.services.ExpensesService;
import com.financehub.services.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/expenses")
public class ExpensesController {
    @Autowired
    public ExpensesService expensesService;
   @Autowired
   public UserService userService;
    @GetMapping("/categories")
    public String showCategories(Model model) {
        List<ExpenseCategories> categories = expensesService.getAllCategories(userService.getUserId());
        model.addAttribute("categories", categories);
        return "expenses/manageExpenseCategories";
    }
    @PostMapping("/save")
    @ResponseBody
    public String addCategory(@RequestParam ExpensesCategoriesDTO expensesCategoriesDTO,
                                       HttpSession session) {
        expensesService.saveCategory(expensesCategoriesDTO);
        return "redirect:/categories/list";
    }

    @PostMapping("/delete")
    @ResponseBody
    public String deleteCategory(@RequestParam int id) {
        categoryService.deleteCategory(id);
        return "Deleted";
    }
    @GetMapping("/add")
    public String addExpenses(@RequestParam(value = "id", required = false) Long id, Model model) {

        return "expenses/addExpenses";
    }

}
