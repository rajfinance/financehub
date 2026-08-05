package com.financehub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates captcha on login POST before Spring Security authenticates credentials.
 */
@Component
public class LoginCaptchaFilter extends OncePerRequestFilter {

	private final CaptchaService captchaService;

	public LoginCaptchaFilter(CaptchaService captchaService) {
		this.captchaService = captchaService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getServletPath();
		return !( "POST".equalsIgnoreCase(request.getMethod())
				&& "/api/perform_login".equals(path) );
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String answer = request.getParameter("captchaAnswer");
		if (!captchaService.validateAndConsume(request.getSession(true), answer)) {
			response.sendRedirect(request.getContextPath() + "/login?captchaError");
			return;
		}
		filterChain.doFilter(request, response);
	}
}
