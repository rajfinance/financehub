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
}
