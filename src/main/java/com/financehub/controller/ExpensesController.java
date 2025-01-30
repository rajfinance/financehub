package com.financehub.controller;

import com.financehub.dtos.OwnerDTO;
import com.financehub.entities.Owner;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/expenses")
public class ExpensesController {
    @GetMapping("/add")
    public String addExpenses(@RequestParam(value = "id", required = false) Long id, Model model) {

        return "expenses/addExpenses";
    }
}
