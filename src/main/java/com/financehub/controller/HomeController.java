package com.financehub.controller;

import com.financehub.dtos.LoginDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.text.DecimalFormat;

@Controller
public class HomeController {
    @GetMapping("/")
    public String index() {
        System.out.println("inside the index method");
        return "index";
    }

    @GetMapping("/login")
    public String showLoginPage(Model model) {
        model.addAttribute("userDTO", new LoginDTO());
        return "inputs/login";
    }
    @GetMapping("/signup")
    public String showSignupPage() {
        return "inputs/signup";
    }
    @GetMapping("/home")
    public String loadHomePage(Model model,HttpSession session) {
         model.addAttribute("username", session.getAttribute("username"));
        return "login/dashboard";
    }
    @GetMapping("/professional")
    public String loadProfessionalPage(Model model) {
       // model.addAttribute("professionalData", professionalService.getData());
        return "login/professional";
    }

    @GetMapping("/investments")
    public String loadInvestmentsPage(Model model) {
       // model.addAttribute("investmentData", investmentService.getData());
        return "login/investments";
    }

    @GetMapping("/rentals")
    public String loadRentalsPage(Model model) {
        //model.addAttribute("rentalsData", rentalsService.getData());
        return "login/rentals";
    }

    @GetMapping("/loans")
    public String loadLoansPage(Model model) {
        //model.addAttribute("loansData", loansService.getData());
        return "login/loans";
    }

    @GetMapping("/expenses")
    public String loadExpensesPage(Model model) {
        //model.addAttribute("expensesData", expensesService.getData());
        return "login/expenses";
    }

}
