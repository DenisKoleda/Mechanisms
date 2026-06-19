package com.denis.mechanisms.logistics;

import java.util.Arrays;
import java.util.Locale;

public enum RouteMode {
    PRIORITY_FIRST("priority_first", "приоритет"),
    NEAREST("nearest", "ближайший"),
    ROUND_ROBIN("round_robin", "по очереди"),
    SPLIT_EVENLY("split_evenly", "равномерно");

    private final String token;
    private final String russianName;

    RouteMode(String token, String russianName) {
        this.token = token;
        this.russianName = russianName;
    }

    public String token() {
        return token;
    }

    public String russianName() {
        return russianName;
    }

    public RouteMode next() {
        RouteMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static RouteMode parse(String value, RouteMode fallback) {
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
