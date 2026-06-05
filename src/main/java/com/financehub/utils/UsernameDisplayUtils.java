package com.financehub.utils;

/**
 * Consistent username presentation across header, dashboard, and profile screens.
 */
public final class UsernameDisplayUtils {

    private UsernameDisplayUtils() {
    }

    public static String toDisplayName(String username) {
        if (username == null) {
            return "";
        }
        String trimmed = username.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
    }

    /** Header / welcome: "Raja Kumar" when names exist; otherwise capitalized username. */
    public static String toDisplayFullName(String firstName, String lastName, String username) {
        String first = normalizeNamePart(firstName);
        String last = normalizeNamePart(lastName);
        if (!first.isEmpty() && !last.isEmpty()) {
            return toTitleCaseWord(first) + " " + toTitleCaseWord(last);
        }
        if (!first.isEmpty()) {
            return toTitleCaseWord(first);
        }
        if (!last.isEmpty()) {
            return toTitleCaseWord(last);
        }
        return toDisplayName(username);
    }

    private static String normalizeNamePart(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static String toTitleCaseWord(String word) {
        if (word.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase();
    }
}
