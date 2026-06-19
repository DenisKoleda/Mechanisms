package com.denis.mechanisms.logistics;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferTransactionTest {
    private final TransferTransaction transaction = new TransferTransaction();

    @Test
    void transferMovesItemsWithoutDuplication() {
        FakeInventory source = new FakeInventory(9);
        FakeInventory destination = new FakeInventory(9);
        source.setItem(0, stack(Material.IRON_INGOT, 20));
        ItemStack moving = stack(Material.IRON_INGOT, 8);

        TransferTransaction.Result result = transaction.execute(source, 0, destination, moving);

        assertTrue(result.success());
        assertEquals(12, source.getItem(0).getAmount());
        assertEquals(8, destination.getItem(0).getAmount());
    }

    @Test
    void fullDestinationDoesNotRemoveFromSource() {
        FakeInventory source = new FakeInventory(1);
        FakeInventory destination = new FakeInventory(1);
        source.setItem(0, stack(Material.IRON_INGOT, 8));
        destination.setItem(0, stack(Material.GOLD_INGOT, 64));

        TransferTransaction.Result result = transaction.execute(source, 0, destination, stack(Material.IRON_INGOT, 8));

        assertFalse(result.success());
        assertEquals("destination_full", result.code());
        assertNull(result.recoveryStack());
        assertEquals(8, source.getItem(0).getAmount());
        assertEquals(Material.GOLD_INGOT, destination.getItem(0).getType());
    }

    @Test
    void sourceChangedFailsBeforeMutatingDestination() {
        FakeInventory source = new FakeInventory(1);
        FakeInventory destination = new FakeInventory(1);
        source.setItem(0, stack(Material.GOLD_INGOT, 8));

        TransferTransaction.Result result = transaction.execute(source, 0, destination, stack(Material.IRON_INGOT, 8));

        assertFalse(result.success());
        assertEquals("source_changed", result.code());
        assertEquals(Material.GOLD_INGOT, source.getItem(0).getType());
        assertNull(destination.getItem(0));
    }

    @Test
    void addStackMergesBeforeUsingEmptySlot() {
        FakeInventory inventory = new FakeInventory(2);
        inventory.setItem(0, stack(Material.IRON_INGOT, 60));

        int leftover = InventoryUtil.addStack(inventory, stack(Material.IRON_INGOT, 8));

        assertEquals(0, leftover);
        assertEquals(64, inventory.getItem(0).getAmount());
        assertEquals(4, inventory.getItem(1).getAmount());
    }

    @Test
    void addStackReturnsLeftoverAfterPartialMerge() {
        FakeInventory inventory = new FakeInventory(1);
        inventory.setItem(0, stack(Material.IRON_INGOT, 60));

        int leftover = InventoryUtil.addStack(inventory, stack(Material.IRON_INGOT, 8));

        assertEquals(4, leftover);
        assertEquals(64, inventory.getItem(0).getAmount());
    }

    private ItemStack stack(Material material, int amount) {
        return TestStacks.stack(material, amount);
    }

    private static final class FakeInventory implements InventoryAccess {
        private final ItemStack[] contents;

        private FakeInventory(int size) {
            this.contents = new ItemStack[size];
        }

        @Override
        public int size() {
            return contents.length;
        }

        @Override
        public ItemStack getItem(int slot) {
            return contents[slot];
        }

        @Override
        public void setItem(int slot, ItemStack item) {
            contents[slot] = item;
        }

        @Override
        public int maxStackSize() {
            return 64;
        }
    }
}
