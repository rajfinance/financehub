package com.financehub.security;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * Simple free math captcha stored in the HTTP session (no third-party / paid email or CAPTCHA APIs).
 */
@Service
public class CaptchaService {

	public static final String SESSION_ANSWER_KEY = "fh.captcha.answer";
	public static final String SESSION_QUESTION_KEY = "fh.captcha.question";

	private final SecureRandom random = new SecureRandom();

	public void issueChallenge(HttpSession session) {
		int a = 2 + random.nextInt(8); // 2..9
		int b = 1 + random.nextInt(9); // 1..9
		session.setAttribute(SESSION_QUESTION_KEY, a + " + " + b);
		session.setAttribute(SESSION_ANSWER_KEY, a + b);
	}

	public String currentQuestion(HttpSession session) {
		Object q = session.getAttribute(SESSION_QUESTION_KEY);
		if (q == null) {
			issueChallenge(session);
			q = session.getAttribute(SESSION_QUESTION_KEY);
		}
		return String.valueOf(q);
	}

	public boolean validateAndConsume(HttpSession session, String userAnswer) {
		Object expectedObj = session.getAttribute(SESSION_ANSWER_KEY);
		session.removeAttribute(SESSION_ANSWER_KEY);
		session.removeAttribute(SESSION_QUESTION_KEY);
		if (expectedObj == null || userAnswer == null || userAnswer.isBlank()) {
			return false;
		}
		try {
			int expected;
			if (expectedObj instanceof Integer) {
				expected = (Integer) expectedObj;
			} else {
				expected = Integer.parseInt(String.valueOf(expectedObj));
			}
			int actual = Integer.parseInt(userAnswer.trim());
			return expected == actual;
		} catch (NumberFormatException ex) {
			return false;
		}
	}
}
