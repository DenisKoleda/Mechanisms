package com.denis.mechanisms.config;

import java.util.Locale;

public enum CraftingProfile {
    EASY("easy", "лёгкий"),
    NORMAL("normal", "обычный"),
    EXPENSIVE("expensive", "дорогой");

    private final String token;
    private final String russianName;

    CraftingProfile(String token, String russianName) {
        this.token = token;
        this.russianName = russianName;
    }

    public String token() {
        return token;
    }

    public String russianName() {
        return russianName;
    }

    public static CraftingProfile parse(String value, CraftingProfile fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (CraftingProfile profile : values()) {
            if (profile.token.equals(normalized) || profile.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return profile;
            }
        }
        return fallback;
    }
}
