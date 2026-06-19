package com.denis.mechanisms.block;

import org.bukkit.Color;
import org.bukkit.Material;

import java.util.Locale;

public enum PipeChannel {
    DEFAULT("default", "обычный", Material.WHITE_DYE, Color.fromRGB(190, 205, 215)),
    RED("red", "красный", Material.RED_DYE, Color.fromRGB(235, 70, 70)),
    BLUE("blue", "синий", Material.BLUE_DYE, Color.fromRGB(85, 145, 255)),
    GREEN("green", "зелёный", Material.LIME_DYE, Color.fromRGB(80, 230, 125)),
    YELLOW("yellow", "жёлтый", Material.YELLOW_DYE, Color.fromRGB(255, 215, 75));

    private final String token;
    private final String russianName;
    private final Material icon;
    private final Color color;

    PipeChannel(String token, String russianName, Material icon, Color color) {
        this.token = token;
        this.russianName = russianName;
        this.icon = icon;
        this.color = color;
    }

    public String token() {
        return token;
    }

    public String russianName() {
        return russianName;
    }

    public Material icon() {
        return icon;
    }

    public Color color() {
        return color;
    }

    public PipeChannel next() {
        PipeChannel[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static PipeChannel parse(String value, PipeChannel fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        for (PipeChannel channel : values()) {
            if (channel.token.equals(normalized) || channel.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return channel;
            }
        }
        return fallback;
    }
}
