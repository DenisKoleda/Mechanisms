package com.denis.mechanisms.logistics;

import org.bukkit.inventory.ItemStack;

public final class TransferTransaction {
    public Result execute(InventoryAccess source, int sourceSlot, InventoryAccess destination, ItemStack moving) {
        if (StackUtil.isEmpty(moving)) {
            return Result.failed("empty_stack", null);
        }
        if (!InventoryUtil.canFullyAccept(destination, moving)) {
            return Result.failed("destination_full", null);
        }
        if (!InventoryUtil.removeFromSlot(source, sourceSlot, moving)) {
            return Result.failed("source_changed", null);
        }

        int leftover = InventoryUtil.addStack(destination, moving.clone());
        if (leftover == 0) {
            return Result.success(moving.getAmount());
        }

        ItemStack leftoverStack = moving.clone();
        leftoverStack.setAmount(leftover);
        int recoveryLeft = InventoryUtil.addStack(source, leftoverStack);
        if (recoveryLeft == 0) {
            return Result.failed("destination_leftover_recovered", null);
        }

        ItemStack recovery = moving.clone();
        recovery.setAmount(recoveryLeft);
        return Result.failed("needs_recovery_storage", recovery);
    }

    public Result delete(InventoryAccess source, int sourceSlot, ItemStack moving) {
        if (StackUtil.isEmpty(moving)) {
            return Result.failed("empty_stack", null);
        }
        if (!InventoryUtil.removeFromSlot(source, sourceSlot, moving)) {
            return Result.failed("source_changed", null);
        }
        return Result.success(moving.getAmount());
    }

    public record Result(boolean success, int moved, String code, ItemStack recoveryStack) {
        public static Result success(int moved) {
            return new Result(true, moved, "ok", null);
        }

        public static Result failed(String code, ItemStack recoveryStack) {
            return new Result(false, 0, code, recoveryStack);
        }
    }
}
