package com.denis.mechanisms.logistics;

import org.bukkit.inventory.ItemStack;

public interface InventoryAccess {
    int size();

    ItemStack getItem(int slot);

    void setItem(int slot, ItemStack item);

    int maxStackSize();
}
