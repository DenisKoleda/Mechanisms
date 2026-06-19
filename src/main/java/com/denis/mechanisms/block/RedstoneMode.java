package com.denis.mechanisms.block;

import java.util.Arrays;
import java.util.Locale;

public enum RedstoneMode {
    IGNORE("ignore", "игнорировать"),
    REQUIRES_POWER("requires_power", "только при сигнале"),
    REQUIRES_NO_POWER("requires_no_power", "только без сигнала");

    private final String token;
    private final String russianName;

    RedstoneMode(String token, String russianName) {
        this.token = token;
        this.russianName = russianName;
    }

    public String token() {
        return token;
    }

    public String russianName() {
        return russianName;
    }

    public RedstoneMode next() {
        RedstoneMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static RedstoneMode parse(String value, RedstoneMode fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values())
            .filter(mode -> mode.token.equals(normalized) || mode.name().toLowerCase(Locale.ROOT).equals(normalized))
            .findFirst()
            .orElse(fallback);
    }
}
