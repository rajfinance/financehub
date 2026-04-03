package com.financehub.controller;

import com.financehub.dtos.ClientUserDTO;
import com.financehub.entities.ClientUser;
import com.financehub.security.PasswordResetSession;
import com.financehub.services.ExpensesService;
import com.financehub.services.RentalService;
import com.financehub.services.UserService;
import com.financehub.services.WorkService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.financehub.security.ClientUserPrincipal;

import java.time.LocalDate;
import java.time.Year;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
@RequestMapping("/api")
public class ActionController {

	private final UserService userService;
	private final WorkService workService;
	private final RentalService rentalService;
	private final ExpensesService expensesService;

	public ActionController(UserService userService, WorkService workService,
			RentalService rentalService, ExpensesService expensesService) {
		this.userService = userService;
		this.workService = workService;
		this.rentalService = rentalService;
		this.expensesService = expensesService;
	}

	@PostMapping(value = "/perform_signup", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String performSignupForm(@ModelAttribute ClientUserDTO userDTO, RedirectAttributes redirectAttributes) {
		if (userDTO.getPassword() != null && !userDTO.getPassword().equals(userDTO.getConfirmPassword())) {
			redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
			return "redirect:/signup";
		}
		Map<String, String> response = userService.handleSignup(userDTO);

		if (response.containsKey("error")) {
			redirectAttributes.addFlashAttribute("error", response.get("error"));
			return "redirect:/signup";
		}
		redirectAttributes.addFlashAttribute("success", response.get("success"));
		return "redirect:/signup";
	}

	@PostMapping(value = "/password-reset/request", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String requestPasswordReset(@RequestParam("username") String username,
			@RequestParam("email") String email,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		Optional<ClientUser> user = userService.findByUsernameAndEmail(username.trim(), email.trim());
		if (user.isEmpty()) {
			redirectAttributes.addFlashAttribute("error", "Username and email do not match any account.");
			return "redirect:/forgotPassword";
		}
		PasswordResetSession.start(session, user.get().getId());
		redirectAttributes.addFlashAttribute("success", "Identity verified. Choose a new password.");
		return "redirect:/password-reset/confirm";
	}

	@PostMapping(value = "/password-reset/complete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String completePasswordReset(@RequestParam("password") String password,
			@RequestParam("confirm_password") String confirmPassword,
			HttpSession session,
			RedirectAttributes redirectAttributes) {
		if (!PasswordResetSession.isValid(session)) {
			redirectAttributes.addFlashAttribute("error", "Reset session expired or invalid. Start again.");
			return "redirect:/forgotPassword";
		}
		if (password == null || password.length() < 8) {
			redirectAttributes.addFlashAttribute("error", "Password must be at least 8 characters.");
			return "redirect:/password-reset/confirm";
		}
		if (!password.equals(confirmPassword)) {
			redirectAttributes.addFlashAttribute("error", "Passwords do not match.");
			return "redirect:/password-reset/confirm";
		}
		int userId = PasswordResetSession.getUserId(session);
		userService.updatePasswordByUserId(userId, password);
		PasswordResetSession.clear(session);
		redirectAttributes.addFlashAttribute("success", "Password updated. You can sign in.");
		return "redirect:/login";
	}

	@GetMapping("/home")
	public String home(@AuthenticationPrincipal ClientUserPrincipal principal, Model model) {
		String username = principal != null ? principal.getUsername() : "";
		model.addAttribute("username", username);

		int currentYear = Year.now().getValue();
		int currentMonth = LocalDate.now().getMonthValue();
		if (currentMonth == 1) {
			currentMonth = 12;
			currentYear -= 1;
		} else {
			currentMonth -= 1;
		}

		Map<String, Integer> monthlySal = workService.getMonthlySalaryData(currentYear);
		model.addAttribute("monthlySalaryData", monthlySal);

		Map<String, Integer> yearlySal = workService.getYearlySalaryData();
		Map<String, Integer> yearlyExp = expensesService.getYearlyExpenseData();
		Set<String> allYears = new HashSet<>();
		allYears.addAll(yearlySal.keySet());
		allYears.addAll(yearlyExp.keySet());

		for (String year : allYears) {
			yearlySal.putIfAbsent(year, 0);
			yearlyExp.putIfAbsent(year, 0);
		}
		model.addAttribute("yearlySalaryData", yearlySal);
		model.addAttribute("yearlyExpenseData", yearlyExp);

		Map<String, Integer> yearlyRent = rentalService.getYearlyRentData();
		model.addAttribute("yearlyRentData", yearlyRent);

		Map<String, Integer> monthlyExpenseData = expensesService.getMonthlyExpenseData(currentYear);
		model.addAttribute("salaryData", monthlySal);
		model.addAttribute("expenseData", monthlyExpenseData);

		Map<String, Integer> categoryData = expensesService.getCurrentMonthCategoryData(currentYear, currentMonth);
		model.addAttribute("categoryData", categoryData);

		return "views/login/dashboard";
	}
}
