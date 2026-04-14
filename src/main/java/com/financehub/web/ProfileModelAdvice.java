package com.financehub.web;

import com.financehub.security.ClientUserPrincipal;
import com.financehub.services.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Cache-busting version for the header profile image (updates when profile is saved).
 */
@ControllerAdvice
public class ProfileModelAdvice {

	private final UserService userService;

	public ProfileModelAdvice(UserService userService) {
		this.userService = userService;
	}

	@ModelAttribute("profileAvatarVersion")
	public Long profileAvatarVersion(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof ClientUserPrincipal)) {
			return null;
		}
		Long v = userService.getProfileAvatarVersionForCurrentUser();
		return v != null ? v : 0L;
	}
}
