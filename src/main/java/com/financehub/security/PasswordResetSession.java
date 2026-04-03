package com.financehub.security;

import jakarta.servlet.http.HttpSession;

public final class PasswordResetSession {

	public static final String USER_ID = "PASSWORD_RESET_USER_ID";
	public static final String EXPIRES_AT_MS = "PASSWORD_RESET_EXPIRES_AT_MS";
	private static final long TTL_MS = 15 * 60 * 1000L;

	private PasswordResetSession() {
	}

	public static void start(HttpSession session, int userId) {
		session.setAttribute(USER_ID, userId);
		session.setAttribute(EXPIRES_AT_MS, System.currentTimeMillis() + TTL_MS);
	}

	public static void clear(HttpSession session) {
		session.removeAttribute(USER_ID);
		session.removeAttribute(EXPIRES_AT_MS);
	}

	public static boolean isValid(HttpSession session) {
		Object uid = session.getAttribute(USER_ID);
		Object exp = session.getAttribute(EXPIRES_AT_MS);
		if (!(uid instanceof Integer) || !(exp instanceof Long)) {
			return false;
		}
		if (System.currentTimeMillis() > (Long) exp) {
			clear(session);
			return false;
		}
		return true;
	}

	public static int getUserId(HttpSession session) {
		return (Integer) session.getAttribute(USER_ID);
	}
}
