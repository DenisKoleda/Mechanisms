package com.denis.mechanisms.logistics;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterServiceTest {
    private final FilterService filterService = new FilterService();

    @Test
    void emptyFilterAllowsAllItems() {
        FilterSettings settings = new FilterSettings(FilterMode.WHITELIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of());

        assertTrue(filterService.allows(TestStacks.stack(Material.IRON_INGOT), settings));
        assertTrue(filterService.allows(TestStacks.stack(Material.GOLD_INGOT), settings));
    }

    @Test
    void whitelistMaterialOnlyAllowsListedMaterials() {
        FilterSettings settings = new FilterSettings(FilterMode.WHITELIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of(TestStacks.stack(Material.IRON_INGOT)));

        assertTrue(filterService.allows(TestStacks.stack(Material.IRON_INGOT), settings));
        assertFalse(filterService.allows(TestStacks.stack(Material.GOLD_INGOT), settings));
    }

    @Test
    void blacklistMaterialOnlyRejectsListedMaterials() {
        FilterSettings settings = new FilterSettings(FilterMode.BLACKLIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of(TestStacks.stack(Material.IRON_INGOT)));

        assertFalse(filterService.allows(TestStacks.stack(Material.IRON_INGOT), settings));
        assertTrue(filterService.allows(TestStacks.stack(Material.GOLD_INGOT), settings));
    }

    @Test
    void exactMetaModeUsesItemSimilarity() {
        FilterSettings settings = new FilterSettings(FilterMode.WHITELIST, MatchMode.EXACT_META, RouteMode.PRIORITY_FIRST, 0, List.of(TestStacks.stack(Material.IRON_INGOT, 1, "custom-name-a")));

        assertTrue(filterService.allows(TestStacks.stack(Material.IRON_INGOT, 1, "custom-name-a"), settings));
        assertFalse(filterService.allows(TestStacks.stack(Material.IRON_INGOT, 1, "custom-name-b"), settings));
    }
}
