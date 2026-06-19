package com.denis.mechanisms.logistics;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class StackUtil {
    private StackUtil() {
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getAmount() <= 0 || isAir(stack.getType());
    }

    public static boolean isAir(Material material) {
        if (material == null) {
            return true;
        }
        String name = material.name();
        return name.equals("AIR") || name.equals("CAVE_AIR") || name.equals("VOID_AIR");
    }
}
