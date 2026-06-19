package com.denis.mechanisms.block;

import java.util.Arrays;
import java.util.Locale;

public enum TrashMode {
    DISABLED("disabled", "выключен"),
    FILTERED_ONLY("filtered_only", "только фильтр"),
    ACCEPT_ALL("accept_all", "принимать все");

    private final String token;
    private final String russianName;

    TrashMode(String token, String russianName) {
        this.token = token;
        this.russianName = russianName;
    }

    public String token() {
        return token;
    }

    public String russianName() {
        return russianName;
    }

    public TrashMode next() {
        TrashMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static TrashMode parse(String value, TrashMode fallback) {
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
