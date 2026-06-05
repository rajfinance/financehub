package com.financehub.controller;

import com.financehub.dtos.ClientUserDTO;
import com.financehub.entities.ClientUser;
import com.financehub.security.PasswordResetSession;
import com.financehub.dtos.DashboardChartDataDTO;
import com.financehub.services.DashboardService;
import com.financehub.services.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/api")
public class ActionController {

	private final UserService userService;
	private final DashboardService dashboardService;

	public ActionController(UserService userService, DashboardService dashboardService) {
		this.userService = userService;
		this.dashboardService = dashboardService;
	}

	@PostMapping(value = "/perform_signup", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public String performSignupForm(@Valid @ModelAttribute ClientUserDTO userDTO, BindingResult bindingResult,
			RedirectAttributes redirectAttributes) {
		if (bindingResult.hasErrors()) {
			var fieldError = bindingResult.getFieldError();
			redirectAttributes.addFlashAttribute("error",
					fieldError != null ? fieldError.getDefaultMessage() : "Invalid signup data.");
			return "redirect:/signup";
		}
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
	public String home(Model model) {
		Map<String, Integer> kpis = dashboardService.getKpiSummary();
		model.addAttribute("currentYearSalary", kpis.get("currentYearSalary"));
		model.addAttribute("currentYearExpense", kpis.get("currentYearExpense"));
		model.addAttribute("currentYearRent", kpis.get("currentYearRent"));
		model.addAttribute("currentYearNetBalance", kpis.get("currentYearNetBalance"));
		model.addAttribute("currentYearPendingLoanEmi", 0);
		return "views/login/dashboard";
	}

	@GetMapping("/home/chart-data")
	@org.springframework.web.bind.annotation.ResponseBody
	public DashboardChartDataDTO homeChartData() {
		return dashboardService.getChartData();
	}
}
