package com.financehub.controller;

import com.financehub.entities.ClientUser;
import com.financehub.services.UserService;
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

	@GetMapping("/account/profile")
	public String profilePage(Model model) {
		Optional<ClientUser> user = userService.getCurrentClientUser();
		if (user.isEmpty()) {
			return "redirect:/login";
		}
		ClientUser u = user.get();
		model.addAttribute("username", u.getUsername());
		model.addAttribute("email", u.getEmail());
		model.addAttribute("phone", u.getPhone());
		model.addAttribute("hasProfilePhoto", u.getProfilePhoto() != null && u.getProfilePhoto().length > 0);
		return "views/inputs/updateProfile";
	}

	@PostMapping("/api/account/profile")
	public String updateProfilePost(
			@RequestParam("email") String email,
			@RequestParam("phone") String phone,
			@RequestParam(value = "photo", required = false) MultipartFile photo,
			@RequestParam(value = "removePhoto", defaultValue = "false") boolean removePhoto,
			RedirectAttributes redirectAttributes) {
		try {
			userService.updateProfile(email, phone, photo, removePhoto);
			redirectAttributes.addFlashAttribute("success", "Profile updated.");
		} catch (IllegalArgumentException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		} catch (IllegalStateException ex) {
			redirectAttributes.addFlashAttribute("error", ex.getMessage());
		} catch (IOException ex) {
			redirectAttributes.addFlashAttribute("error", "Could not process the image. Try another file.");
		}
		return "redirect:/account/profile";
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
}
