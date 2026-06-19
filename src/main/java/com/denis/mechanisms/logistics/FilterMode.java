package com.denis.mechanisms.logistics;

import java.util.Locale;

public enum FilterMode {
    WHITELIST,
    BLACKLIST;

    public static FilterMode parse(String value, FilterMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return FilterMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
