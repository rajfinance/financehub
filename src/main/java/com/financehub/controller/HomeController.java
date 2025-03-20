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
        return "index";
    }
    @GetMapping("/error")
    public String error() {
        return "error";
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
    public String loadHomePage(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) {
            return "redirect:/login";
        }
        model.addAttribute("username", username);
        return "login/dashboard";
    }

    @GetMapping("/professional")
    public String loadProfessionalPage(Model model) {
        return "views/login/professional";
    }

    @GetMapping("/investments")
    public String loadInvestmentsPage(Model model) {
        return "views/login/investments";
    }

    @GetMapping("/rentals")
    public String loadRentalsPage(Model model) {
        return "views/login/rentals";
    }

    @GetMapping("/loans")
    public String loadLoansPage(Model model) {
        return "views/login/loans";
    }

    @GetMapping("/expenses")
    public String loadExpensesPage(Model model) {
        return "views/login/expenses";
    }
}