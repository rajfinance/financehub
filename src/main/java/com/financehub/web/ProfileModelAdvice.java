package com.financehub.web;



import com.financehub.dtos.ProfileHeaderContext;

import com.financehub.security.ClientUserPrincipal;

import com.financehub.services.UserService;

import com.financehub.utils.UsernameDisplayUtils;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.ControllerAdvice;

import org.springframework.web.bind.annotation.ModelAttribute;

import org.springframework.web.context.request.RequestAttributes;

import org.springframework.web.context.request.RequestContextHolder;



/**

 * Cache-busting version for the header profile image (updates when profile is saved).

 */

@ControllerAdvice

public class ProfileModelAdvice {



	private static final String HEADER_CONTEXT_ATTR = ProfileModelAdvice.class.getName() + ".headerContext";



	private final UserService userService;



	public ProfileModelAdvice(UserService userService) {

		this.userService = userService;

	}



	@ModelAttribute("displayUsername")

	public String displayUsername(Authentication authentication) {

		return resolveHeader(authentication).getDisplayUsername();

	}



	@ModelAttribute("displayFullName")

	public String displayFullName(Authentication authentication) {

		return resolveHeader(authentication).getDisplayFullName();

	}



	@ModelAttribute("profileAvatarVersion")

	public Long profileAvatarVersion(Authentication authentication) {

		if (authentication == null || !(authentication.getPrincipal() instanceof ClientUserPrincipal)) {

			return null;

		}

		return resolveHeader(authentication).getProfileAvatarVersion();

	}



	private ProfileHeaderContext resolveHeader(Authentication authentication) {

		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

		if (requestAttributes != null) {

			Object cached = requestAttributes.getAttribute(HEADER_CONTEXT_ATTR, RequestAttributes.SCOPE_REQUEST);

			if (cached instanceof ProfileHeaderContext) {

				return (ProfileHeaderContext) cached;

			}

		}



		ProfileHeaderContext headerContext = buildHeaderContext(authentication);

		if (requestAttributes != null) {

			requestAttributes.setAttribute(HEADER_CONTEXT_ATTR, headerContext, RequestAttributes.SCOPE_REQUEST);

		}

		return headerContext;

	}



	private ProfileHeaderContext buildHeaderContext(Authentication authentication) {

		if (authentication == null || !authentication.isAuthenticated()) {

			return new ProfileHeaderContext("", "", 0L);

		}

		if (authentication.getPrincipal() instanceof ClientUserPrincipal) {

			ClientUserPrincipal principal = (ClientUserPrincipal) authentication.getPrincipal();

			return new ProfileHeaderContext(

					principal.getDisplayUsername(),

					principal.getDisplayFullName(),

					principal.getProfileAvatarVersion());

		}

		return userService.getProfileHeaderContext()

				.orElseGet(() -> new ProfileHeaderContext(

						UsernameDisplayUtils.toDisplayName(authentication.getName()),

						UsernameDisplayUtils.toDisplayName(authentication.getName()),

						0L));

	}

}


