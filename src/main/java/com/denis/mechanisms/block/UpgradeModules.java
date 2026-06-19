package com.denis.mechanisms.block;

import java.util.LinkedHashMap;
import java.util.Map;

public record UpgradeModules(
    int speedLevel,
    int stackLevel,
    int filterLevel,
    int priorityLevel,
    int rangeLevel,
    int silentLevel
) {
    public static final UpgradeModules EMPTY = new UpgradeModules(0, 0, 0, 0, 0, 0);

    public UpgradeModules {
        speedLevel = clamp(speedLevel);
        stackLevel = clamp(stackLevel);
        filterLevel = clamp(filterLevel);
        priorityLevel = clamp(priorityLevel);
        rangeLevel = clamp(rangeLevel);
        silentLevel = clamp(silentLevel);
    }

    public UpgradeModules withSpeedLevel(int level) {
        return new UpgradeModules(level, stackLevel, filterLevel, priorityLevel, rangeLevel, silentLevel);
    }

    public UpgradeModules withStackLevel(int level) {
        return new UpgradeModules(speedLevel, level, filterLevel, priorityLevel, rangeLevel, silentLevel);
    }

    public UpgradeModules withFilterLevel(int level) {
        return new UpgradeModules(speedLevel, stackLevel, level, priorityLevel, rangeLevel, silentLevel);
    }

    public UpgradeModules withPriorityLevel(int level) {
        return new UpgradeModules(speedLevel, stackLevel, filterLevel, level, rangeLevel, silentLevel);
    }

    public UpgradeModules withRangeLevel(int level) {
        return new UpgradeModules(speedLevel, stackLevel, filterLevel, priorityLevel, level, silentLevel);
    }

    public UpgradeModules withSilentLevel(int level) {
        return new UpgradeModules(speedLevel, stackLevel, filterLevel, priorityLevel, rangeLevel, level);
    }

    public boolean hasSilent() {
        return silentLevel > 0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("speed", speedLevel);
        row.put("stack", stackLevel);
        row.put("filter", filterLevel);
        row.put("priority", priorityLevel);
        row.put("range", rangeLevel);
        row.put("silent", silentLevel);
        return row;
    }

    public static UpgradeModules fromMap(Map<?, ?> row) {
        if (row == null) {
            return EMPTY;
        }
        return new UpgradeModules(
            intValue(row.get("speed")),
            intValue(row.get("stack")),
            intValue(row.get("filter")),
            intValue(row.get("priority")),
            intValue(row.get("range")),
            intValue(row.get("silent"))
        );
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(3, value));
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
