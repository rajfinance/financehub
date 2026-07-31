package com.financehub.controller;

import com.financehub.entities.ClientUser;
import com.financehub.services.UserService;
import com.financehub.utils.UsernameDisplayUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.Optional;

@Controller
public class AccountController {

	private final UserService userService;
	private final ResourceLoader resourceLoader;

	public AccountController(UserService userService, ResourceLoader resourceLoader) {
		this.userService = userService;
		this.resourceLoader = resourceLoader;
	}

	@GetMapping("/api/account")
	public String profileHub() {
		return "views/account/profileHub";
	}

	@GetMapping("/account/change-password")
	public String changePasswordFullPage() {
		return "views/inputs/changePassword";
	}

	@GetMapping("/api/account/change-password")
	public String changePasswordFragment() {
		return "views/account/changePasswordPanel";
	}

	@PostMapping("/api/account/change-password")
	public String changePassword(@RequestParam("currentPassword") String currentPassword,
			@RequestParam("newPassword") String newPassword,
			@RequestParam("confirmPassword") String confirmPassword,
			Model model,
			RedirectAttributes redirectAttributes,
			HttpServletRequest request) {
		if (newPassword == null || newPassword.length() < 8) {
			return passwordResult(false, "New password must be at least 8 characters.", model, redirectAttributes, request);
		}
		if (!newPassword.equals(confirmPassword)) {
			return passwordResult(false, "New passwords do not match.", model, redirectAttributes, request);
		}
		if (userService.changePasswordForCurrentUser(currentPassword, newPassword)) {
			return passwordResult(true, "Password changed successfully.", model, redirectAttributes, request);
		}
		return passwordResult(false, "Current password is incorrect.", model, redirectAttributes, request);
	}

	@GetMapping("/account/profile")
	public String profileFullPage(Model model) {
		if (!populateProfileModel(model)) {
			return "redirect:/login";
		}
		return "views/inputs/updateProfile";
	}

	@GetMapping("/api/account/profile")
	public String profileFragment(Model model) {
		if (!populateProfileModel(model)) {
			return "redirect:/login";
		}
		return "views/account/updateProfilePanel";
	}

	@PostMapping("/api/account/profile")
	public String updateProfilePost(
			@RequestParam("firstName") String firstName,
			@RequestParam("lastName") String lastName,
			@RequestParam("email") String email,
			@RequestParam("phone") String phone,
			@RequestParam(value = "photo", required = false) MultipartFile photo,
			@RequestParam(value = "removePhoto", defaultValue = "false") boolean removePhoto,
			Model model,
			RedirectAttributes redirectAttributes,
			HttpServletRequest request) {
		try {
			userService.updateProfile(firstName, lastName, email, phone, photo, removePhoto);
			return profileResult(true, "Profile updated.", model, redirectAttributes, request);
		} catch (IllegalArgumentException | IllegalStateException ex) {
			return profileResult(false, ex.getMessage(), model, redirectAttributes, request);
		} catch (IOException ex) {
			return profileResult(false, "Could not process the image. Try another file.", model, redirectAttributes, request);
		}
	}

	@GetMapping("/account/profile-photo")
	public ResponseEntity<byte[]> profilePhoto() throws IOException {
		long uid = userService.getUserId();
		if (uid <= 0) {
			return ResponseEntity.status(401).build();
		}
		Optional<ClientUser> opt = userService.getCurrentClientUser();
		if (opt.isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		ClientUser u = opt.get();
		byte[] body;
		MediaType mediaType;
		if (u.getProfilePhoto() != null && u.getProfilePhoto().length > 0) {
			body = u.getProfilePhoto();
			mediaType = MediaType.parseMediaType(
					Optional.ofNullable(u.getProfilePhotoContentType()).orElse(MediaType.IMAGE_JPEG_VALUE));
		} else {
			Resource res = resourceLoader.getResource("classpath:/static/images/signin.png");
			if (!res.exists()) {
				res = resourceLoader.getResource("classpath:/static/images/financehublogo.png");
			}
			if (!res.exists()) {
				return ResponseEntity.notFound().build();
			}
			body = res.getContentAsByteArray();
			String filename = res.getFilename();
			mediaType = filename != null && filename.toLowerCase().endsWith(".png")
					? MediaType.IMAGE_PNG
					: MediaType.IMAGE_JPEG;
		}
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore().mustRevalidate().cachePrivate())
				.header(HttpHeaders.CONTENT_LENGTH, Integer.toString(body.length))
				.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
				.contentType(mediaType)
				.body(body);
	}

	private boolean populateProfileModel(Model model) {
		Optional<ClientUser> user = userService.getCurrentClientUser();
		if (user.isEmpty()) {
			return false;
		}
		ClientUser u = user.get();
		model.addAttribute("username", u.getUsername());
		model.addAttribute("displayUsername", UsernameDisplayUtils.toDisplayName(u.getUsername()));
		model.addAttribute("firstName", u.getFirstName() != null ? u.getFirstName() : "");
		model.addAttribute("lastName", u.getLastName() != null ? u.getLastName() : "");
		model.addAttribute("email", u.getEmail());
		model.addAttribute("phone", u.getPhone());
		model.addAttribute("hasProfilePhoto", u.getProfilePhoto() != null && u.getProfilePhoto().length > 0);
		return true;
	}

	private String passwordResult(boolean success, String message, Model model,
			RedirectAttributes redirectAttributes, HttpServletRequest request) {
		if (isAjax(request)) {
			model.addAttribute(success ? "success" : "error", message);
			return "views/account/changePasswordPanel";
		}
		redirectAttributes.addFlashAttribute(success ? "success" : "error", message);
		return "redirect:/account/change-password";
	}

	private String profileResult(boolean success, String message, Model model,
			RedirectAttributes redirectAttributes, HttpServletRequest request) {
		if (isAjax(request)) {
			populateProfileModel(model);
			model.addAttribute(success ? "success" : "error", message);
			return "views/account/updateProfilePanel";
		}
		redirectAttributes.addFlashAttribute(success ? "success" : "error", message);
		return "redirect:/account/profile";
	}

	private boolean isAjax(HttpServletRequest request) {
		return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}
}
