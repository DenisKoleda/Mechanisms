package com.denis.mechanisms.block;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum MechanismBlockType {
    EXTRACTOR("extractor", "Extractor", "Экстрактор", NamedTextColor.GOLD),
    PIPE("pipe", "Pipe", "Труба I", NamedTextColor.AQUA, 1),
    PIPE_FAST("pipe_fast", "Fast Pipe", "Труба II", NamedTextColor.GREEN, 2, "fast_pipe", "pipe2", "pipe_2"),
    PIPE_EXPRESS("pipe_express", "Express Pipe", "Труба III", NamedTextColor.LIGHT_PURPLE, 3, "express_pipe", "pipe3", "pipe_3"),
    ROUTER("router", "Router", "Маршрутизатор", NamedTextColor.YELLOW),
    INSERTER("inserter", "Inserter", "Инсертер", NamedTextColor.GREEN),
    OVERFLOW("overflow", "Overflow", "Overflow", NamedTextColor.BLUE),
    TRASH("trash", "Trash", "Утилизатор", NamedTextColor.RED);

    private final String token;
    private final String displayName;
    private final String russianName;
    private final NamedTextColor color;
    private final int pipeTier;
    private final List<String> aliases;

    MechanismBlockType(String token, String displayName, String russianName, NamedTextColor color) {
        this(token, displayName, russianName, color, 0);
    }

    MechanismBlockType(String token, String displayName, String russianName, NamedTextColor color, int pipeTier, String... aliases) {
        this.token = token;
        this.displayName = displayName;
        this.russianName = russianName;
        this.color = color;
        this.pipeTier = pipeTier;
        this.aliases = List.of(aliases);
    }

    public String token() {
        return token;
    }

    public String displayName() {
        return displayName;
    }

    public String russianName() {
        return russianName;
    }

    public NamedTextColor color() {
        return color;
    }

    public boolean isPipe() {
        return pipeTier > 0;
    }

    public int pipeTier() {
        return pipeTier;
    }

    public boolean supportsFilter() {
        return this == ROUTER || isDestination();
    }

    public boolean supportsIoSide() {
        return this == EXTRACTOR || this == INSERTER || this == OVERFLOW;
    }

    public boolean isDestination() {
        return this == INSERTER || this == OVERFLOW || this == TRASH;
    }

    public static Optional<MechanismBlockType> fromToken(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values())
            .filter(type -> type.token.equals(normalized) || type.aliases.contains(normalized))
            .findFirst();
    }
}
