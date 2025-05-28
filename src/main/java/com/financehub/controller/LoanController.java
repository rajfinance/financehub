package com.financehub.controller;

import com.financehub.dtos.LoanDTO;
import com.financehub.services.LoanService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/loan")
public class LoanController {

    @Autowired
    LoanService loanService;
    @GetMapping("/add")
    public String manageLoanPage(@RequestParam(value = "id", required = false) Long id, Model model) {
        if (id != null) {

        }else {

        }
        List<String> bankNames = List.of("ICICI", "HDFC", "Axis");
        model.addAttribute("loan", new LoanDTO());
        model.addAttribute("bankNames", bankNames);
        return "loan/addLoan";
    }
    @PostMapping("/addLoan")
    public String addLoan(@ModelAttribute("loanDto") LoanDTO loanDto, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login";
        }

        loanService.addLoanFromDto(loanDto, userId);
        return "redirect:/loans";
    }

}
