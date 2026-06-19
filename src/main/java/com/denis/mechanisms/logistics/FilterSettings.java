package com.denis.mechanisms.logistics;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FilterSettings(
    FilterMode mode,
    MatchMode matchMode,
    RouteMode routeMode,
    int priority,
    List<ItemStack> items
) {
    public FilterSettings {
        mode = mode == null ? FilterMode.WHITELIST : mode;
        matchMode = matchMode == null ? MatchMode.MATERIAL_ONLY : matchMode;
        routeMode = routeMode == null ? RouteMode.PRIORITY_FIRST : routeMode;
        items = cloneItems(items);
    }

    public static FilterSettings empty(FilterMode mode, boolean exactMetaDefault) {
        return new FilterSettings(
            mode == null ? FilterMode.WHITELIST : mode,
            exactMetaDefault ? MatchMode.EXACT_META : MatchMode.MATERIAL_ONLY,
            RouteMode.PRIORITY_FIRST,
            0,
            List.of()
        );
    }

    public FilterSettings withMode(FilterMode newMode) {
        return new FilterSettings(newMode, matchMode, routeMode, priority, items);
    }

    public FilterSettings withMatchMode(MatchMode newMatchMode) {
        return new FilterSettings(mode, newMatchMode, routeMode, priority, items);
    }

    public FilterSettings withRouteMode(RouteMode newRouteMode) {
        return new FilterSettings(mode, matchMode, newRouteMode, priority, items);
    }

    public FilterSettings withPriority(int newPriority) {
        return new FilterSettings(mode, matchMode, routeMode, Math.max(-1000, Math.min(1000, newPriority)), items);
    }

    public FilterSettings withItems(List<ItemStack> newItems) {
        return new FilterSettings(mode, matchMode, routeMode, priority, newItems);
    }

    public int itemCount() {
        return items.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("mode", mode.name());
        row.put("matchMode", matchMode.name());
        row.put("routeMode", routeMode.name());
        row.put("priority", priority);
        row.put("items", cloneItems(items));
        return row;
    }

    public static FilterSettings fromMap(Map<?, ?> row, FilterSettings fallback) {
        if (row == null) {
            return fallback;
        }

        FilterMode mode = FilterMode.parse(String.valueOf(row.get("mode")), fallback.mode());
        MatchMode matchMode = MatchMode.parse(String.valueOf(row.get("matchMode")), fallback.matchMode());
        RouteMode routeMode = RouteMode.parse(String.valueOf(row.get("routeMode")), fallback.routeMode());
        int priority = intValue(row.get("priority"), fallback.priority());
        List<ItemStack> items = new ArrayList<>();
        Object itemRows = row.get("items");
        if (itemRows instanceof List<?> list) {
            for (Object value : list) {
                if (value instanceof ItemStack stack && !StackUtil.isEmpty(stack)) {
                    ItemStack clone = stack.clone();
                    clone.setAmount(1);
                    items.add(clone);
                }
            }
        }
        return new FilterSettings(mode, matchMode, routeMode, priority, items);
    }

    private static List<ItemStack> cloneItems(List<ItemStack> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack item : source) {
            if (StackUtil.isEmpty(item)) {
                continue;
            }
            ItemStack clone = item.clone();
            clone.setAmount(1);
            result.add(clone);
        }
        return List.copyOf(result);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
