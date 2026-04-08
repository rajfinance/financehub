package com.financehub.controller;

import com.financehub.security.PasswordResetSession;
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
        return "views/index";
    }

    @GetMapping("/services")
    public String services() {
        return "views/services";
    }

    @GetMapping("/contact")
    public String contact() {
        return "views/contact";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "views/inputs/login";
    }

    @GetMapping("/forgotPassword")
    public String showForgotPassword() {
        return "views/inputs/forgotPassword";
    }

    @GetMapping("/password-reset/confirm")
    public String passwordResetConfirm(HttpSession session) {
        if (!PasswordResetSession.isValid(session)) {
            return "redirect:/forgotPassword";
        }
        return "views/inputs/passwordResetConfirm";
    }

    @PostMapping("/api/calculate")
    public String calculate(
            @RequestParam("axis") double axis,
            @RequestParam("icici") double icici,
            @RequestParam("hdfc") double hdfc,
            @RequestParam("cc") double cc,
            @RequestParam("givnamnt") double givnamnt,
            Model model) {
        DecimalFormat indianFormat = new DecimalFormat("##,##,##,##0");
        axis = Math.ceil((axis * 0.05) + (axis * 0.05) * 0.12 + axis);
        hdfc = Math.ceil((hdfc * 0.04) + (hdfc * 0.04) * 0.12 + hdfc);
        StringBuilder resultMessage = new StringBuilder();
        resultMessage.append("<table><tr><td style=\"text-align: left;\">Total Loan: ").append("</td><td style=\"text-align: right;\">").append(indianFormat.format(Math.ceil(axis + hdfc + icici))).append("\n");
        resultMessage.append("</td></tr><tr><td style=\"text-align: left;\">Credit Card: ").append("</td><td style=\"text-align: right;\">").append(indianFormat.format(cc)).append("\n");
        resultMessage.append("</td></tr><tr><td style=\"text-align: left;\">Given Amount: ").append("</td><td style=\"text-align: right;\">").append(indianFormat.format(givnamnt)).append("\n");

        double result = Math.ceil(axis + hdfc + icici + cc + givnamnt);
        resultMessage.append("</td></tr><tr><td style=\"text-align: left;\">Total Amount: ").append("</td><td style=\"text-align: right;\">").append(indianFormat.format(result)).append("\n");
        double peracre = Math.ceil(result / 1.85);
        resultMessage.append("</td></tr><tr><td style=\"text-align: left;\">Per Acre : ").append("</td><td style=\"text-align: right;\">").append(indianFormat.format(peracre)).append("</td></tr></table>");

        model.addAttribute("result", resultMessage.toString().replaceAll("\n", "<br/>"));
        return "views/index";
    }

    @GetMapping("/signup")
    public String showSignupPage() {
        return "views/inputs/signup";
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