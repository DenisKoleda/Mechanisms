package com.denis.mechanisms.logistics;

import org.bukkit.inventory.ItemStack;

public final class FilterService {
    public boolean allows(ItemStack stack, FilterSettings settings) {
        if (StackUtil.isEmpty(stack)) {
            return false;
        }
        if (settings == null || settings.items().isEmpty()) {
            return true;
        }

        boolean matched = settings.items().stream().anyMatch(filter -> matches(stack, filter, settings.matchMode()));
        return settings.mode() == FilterMode.WHITELIST ? matched : !matched;
    }

    public String summary(FilterSettings settings) {
        if (settings == null) {
            return "без фильтра";
        }
        return modeName(settings.mode())
            + ", "
            + settings.itemCount()
            + " предметов, "
            + matchName(settings.matchMode())
            + ", приоритет "
            + settings.priority()
            + ", маршрут "
            + settings.routeMode().russianName();
    }

    private String modeName(FilterMode mode) {
        return mode == FilterMode.WHITELIST ? "белый список" : "черный список";
    }

    private String matchName(MatchMode mode) {
        return mode == MatchMode.MATERIAL_ONLY ? "только материал" : "точное совпадение";
    }

    private boolean matches(ItemStack candidate, ItemStack filter, MatchMode mode) {
        if (StackUtil.isEmpty(filter)) {
            return false;
        }
        if (mode == MatchMode.MATERIAL_ONLY) {
            return candidate.getType() == filter.getType();
        }
        return candidate.isSimilar(filter);
    }
}
