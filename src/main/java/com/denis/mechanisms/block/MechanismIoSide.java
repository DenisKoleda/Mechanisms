package com.denis.mechanisms.block;

import org.bukkit.block.BlockFace;

import java.util.Arrays;
import java.util.Locale;

public enum MechanismIoSide {
    AUTO("auto", "авто", null),
    NORTH("north", "север", BlockFace.NORTH),
    SOUTH("south", "юг", BlockFace.SOUTH),
    EAST("east", "восток", BlockFace.EAST),
    WEST("west", "запад", BlockFace.WEST),
    UP("up", "верх", BlockFace.UP),
    DOWN("down", "низ", BlockFace.DOWN);

    private final String token;
    private final String russianName;
    private final BlockFace face;

    MechanismIoSide(String token, String russianName, BlockFace face) {
        this.token = token;
        this.russianName = russianName;
        this.face = face;
    }

    public String token() {
        return token;
    }

    public String russianName() {
        return russianName;
    }

    public BlockFace face() {
        return face;
    }

    public MechanismIoSide next() {
        MechanismIoSide[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static MechanismIoSide parse(String value, MechanismIoSide fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return Arrays.stream(values())
            .filter(side -> side.token.equals(normalized) || side.name().toLowerCase(Locale.ROOT).equals(normalized))
            .findFirst()
            .orElse(fallback);
    }
}
