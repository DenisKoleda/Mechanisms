package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public record PendingTransfer(
    UUID id,
    long createdAt,
    int attempts,
    BlockKey preferredSource,
    ItemStack item,
    String reason
) {
    public PendingTransfer withAttempts(int newAttempts) {
        return new PendingTransfer(id, createdAt, newAttempts, preferredSource, item.clone(), reason);
    }

    public PendingTransfer withItemAndAttempts(ItemStack newItem, int newAttempts) {
        return new PendingTransfer(id, createdAt, newAttempts, preferredSource, newItem.clone(), reason);
    }
}
