package com.financehub.controller;

import com.financehub.dtos.ClientUserDTO;
import com.financehub.dtos.LoginDTO;
import com.financehub.services.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/api")
public class ActionController {
    @Autowired
    private UserService userService;

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
        return "login/home";
    }

    @RequestMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
