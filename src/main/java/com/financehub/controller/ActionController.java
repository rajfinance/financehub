package com.financehub.controller;

import com.financehub.dtos.ClientUserDTO;
import com.financehub.dtos.LoginDTO;
import com.financehub.services.ExpensesService;
import com.financehub.services.RentalService;
import com.financehub.services.UserService;
import com.financehub.services.WorkService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.Month;
import java.time.Year;
import java.util.Map;

@Controller
@RequestMapping("/api")
public class ActionController {
    @Autowired
    private UserService userService;
    @Autowired
    private WorkService workService;
    @Autowired
    private RentalService rentalService;
    @Autowired
    private ExpensesService expensesService;
    @PostMapping(value = "/perform_signup", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String performSignupForm(@ModelAttribute ClientUserDTO userDTO, RedirectAttributes redirectAttributes) {
        Map<String, String> response = userService.handleSignup(userDTO);

        if (response.containsKey("error")) {
            redirectAttributes.addFlashAttribute("error", response.get("error"));
            return "redirect:/signup";
        } else {
            redirectAttributes.addFlashAttribute("success", response.get("success"));
            return "redirect:/signup";
        }
    }

    @PostMapping("/perform_login")
    public String handleLogin(LoginDTO loginDTO, Model model,HttpSession session) {
        boolean isAuthenticated = userService.authenticate(loginDTO.getUsername(), loginDTO.getPassword());
        if (true){//isAuthenticated) {
            session.setAttribute("username", loginDTO.getUsername());
            session.setAttribute("loggedIn", true);
            return "redirect:/api/home";
        } else {
            model.addAttribute("error", "Invalid username or password");
            return "inputs/login";
        }
    }

    @GetMapping("/home")
    public String home(HttpSession session,Model model) {
        String username = (String) session.getAttribute("username");
        Boolean loggedIn = (Boolean) session.getAttribute("loggedIn");
        if (username == null || !Boolean.TRUE.equals(loggedIn)) {
            return "redirect:/login";
        }
        model.addAttribute("username", username);

        int currentYear = Year.now().getValue();
        int currentMonth = LocalDate.now().getMonthValue();
        if (currentMonth == 1) {
            currentMonth = 12;
            currentYear -= 1;
        } else {
            currentMonth -= 1;
        }

        Map<String,Integer> monthlySal = workService.getMonthlySalaryData(currentYear);
        model.addAttribute("monthlySalaryData", monthlySal);

        Map<String,Integer> yearlySal = workService.getYearlySalaryData();
        model.addAttribute("yearlySalaryData", yearlySal);

        Map<String,Integer> yearlyRent = rentalService.getYearlyRentData();
        model.addAttribute("yearlyRentData", yearlyRent);

        Map<String, Integer> monthlySalaryData = workService.getMonthlySalaryData(currentYear);
        Map<String, Integer> monthlyExpenseData = expensesService.getMonthlyExpenseData(currentYear);

        model.addAttribute("salaryData", monthlySalaryData);
        model.addAttribute("expenseData", monthlyExpenseData);

        Map<String,Integer> categoryData = expensesService.getCurrentMonthCategoryData(currentYear,currentMonth);
        model.addAttribute("categoryData", categoryData);

        return "login/dashboard";
    }

    @RequestMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
