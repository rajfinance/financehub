package com.financehub.controller;

import com.financehub.dtos.LoanDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/loan")
public class LoanController {

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

}
