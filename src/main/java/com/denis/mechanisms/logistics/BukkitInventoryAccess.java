package com.denis.mechanisms.logistics;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class BukkitInventoryAccess implements InventoryAccess {
    private final Inventory inventory;

    public BukkitInventoryAccess(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public int size() {
        return inventory.getSize();
    }

    @Override
    public ItemStack getItem(int slot) {
        return inventory.getItem(slot);
    }

    @Override
    public void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    @Override
    public int maxStackSize() {
        return inventory.getMaxStackSize();
    }

    public Inventory inventory() {
        return inventory;
    }
}
