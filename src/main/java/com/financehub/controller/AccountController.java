package com.financehub.controller;

import com.financehub.services.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

	private final UserService userService;

	public AccountController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/account/change-password")
	public String changePasswordForm() {
		return "views/inputs/changePassword";
	}

	@PostMapping("/api/account/change-password")
	public String changePassword(@RequestParam("currentPassword") String currentPassword,
			@RequestParam("newPassword") String newPassword,
			@RequestParam("confirmPassword") String confirmPassword,
			RedirectAttributes redirectAttributes) {
		if (newPassword == null || newPassword.length() < 8) {
			redirectAttributes.addFlashAttribute("error", "New password must be at least 8 characters.");
			return "redirect:/account/change-password";
		}
		if (!newPassword.equals(confirmPassword)) {
			redirectAttributes.addFlashAttribute("error", "New passwords do not match.");
			return "redirect:/account/change-password";
		}
		if (userService.changePasswordForCurrentUser(currentPassword, newPassword)) {
			redirectAttributes.addFlashAttribute("success", "Password changed successfully.");
		} else {
			redirectAttributes.addFlashAttribute("error", "Current password is incorrect.");
		}
		return "redirect:/account/change-password";
	}
}
