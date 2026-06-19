package com.denis.mechanisms.logistics;

import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public final class InventoryUtil {
    private InventoryUtil() {
    }

    public static Optional<SelectedStack> selectFirstMovable(InventoryAccess inventory, int maxAmount) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (StackUtil.isEmpty(current)) {
                continue;
            }
            ItemStack moving = current.clone();
            moving.setAmount(Math.min(Math.max(1, maxAmount), current.getAmount()));
            return Optional.of(new SelectedStack(slot, moving));
        }
        return Optional.empty();
    }

    public static java.util.List<SelectedStack> selectMovableStacks(InventoryAccess inventory, int maxAmount) {
        java.util.List<SelectedStack> stacks = new java.util.ArrayList<>();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack current = inventory.getItem(slot);
            if (StackUtil.isEmpty(current)) {
                continue;
            }
            ItemStack moving = current.clone();
            moving.setAmount(Math.min(Math.max(1, maxAmount), current.getAmount()));
            stacks.add(new SelectedStack(slot, moving));
        }
        return java.util.List.copyOf(stacks);
    }

    public static boolean canFullyAccept(InventoryAccess inventory, ItemStack stack) {
        if (StackUtil.isEmpty(stack)) {
            return true;
        }
        return spaceFor(inventory, stack) >= stack.getAmount();
    }

    public static int spaceFor(InventoryAccess inventory, ItemStack stack) {
        int remaining = stack.getAmount();
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack current = inventory.getItem(slot);
            int maxStack = maxStackSize(inventory, stack);
            if (StackUtil.isEmpty(current)) {
                remaining -= maxStack;
            } else if (current.isSimilar(stack) && current.getAmount() < maxStack) {
                remaining -= maxStack - current.getAmount();
            }
            if (remaining <= 0) {
                return stack.getAmount();
            }
        }
        return stack.getAmount() - Math.max(0, remaining);
    }

    public static boolean removeFromSlot(InventoryAccess inventory, int slot, ItemStack expected) {
        ItemStack current = inventory.getItem(slot);
        if (StackUtil.isEmpty(current) || !current.isSimilar(expected) || current.getAmount() < expected.getAmount()) {
            return false;
        }
        if (current.getAmount() == expected.getAmount()) {
            inventory.setItem(slot, null);
        } else {
            ItemStack updated = current.clone();
            updated.setAmount(current.getAmount() - expected.getAmount());
            inventory.setItem(slot, updated);
        }
        return true;
    }

    public static int addStack(InventoryAccess inventory, ItemStack stack) {
        if (StackUtil.isEmpty(stack)) {
            return 0;
        }
        int remaining = stack.getAmount();
        int maxStack = maxStackSize(inventory, stack);

        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (StackUtil.isEmpty(current) || !current.isSimilar(stack) || current.getAmount() >= maxStack) {
                continue;
            }
            int moved = Math.min(remaining, maxStack - current.getAmount());
            ItemStack updated = current.clone();
            updated.setAmount(current.getAmount() + moved);
            inventory.setItem(slot, updated);
            remaining -= moved;
        }

        for (int slot = 0; slot < inventory.size() && remaining > 0; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (!StackUtil.isEmpty(current)) {
                continue;
            }
            int moved = Math.min(remaining, maxStack);
            ItemStack placed = stack.clone();
            placed.setAmount(moved);
            inventory.setItem(slot, placed);
            remaining -= moved;
        }

        return remaining;
    }

    private static int maxStackSize(InventoryAccess inventory, ItemStack stack) {
        return Math.max(1, Math.min(inventory.maxStackSize(), stack.getMaxStackSize()));
    }

    public record SelectedStack(int slot, ItemStack stack) {
    }
}
