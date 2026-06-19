package com.denis.mechanisms.logistics;

import java.util.Locale;

public enum MatchMode {
    MATERIAL_ONLY,
    EXACT_META;

    public static MatchMode parse(String value, MatchMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return MatchMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
