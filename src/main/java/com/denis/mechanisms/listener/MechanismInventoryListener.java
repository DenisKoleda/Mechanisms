package com.denis.mechanisms.listener;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;

public final class MechanismInventoryListener implements Listener {
    private final MechanismBlockRegistry registry;

    public MechanismInventoryListener(MechanismBlockRegistry registry) {
        this.registry = registry;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (isMechanismInventory(event.getSource()) || isMechanismInventory(event.getDestination())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryPickupItem(InventoryPickupItemEvent event) {
        if (isMechanismInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    private boolean isMechanismInventory(Inventory inventory) {
        Location location = inventory.getLocation();
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Block block = location.getBlock();
        return registry.getRegistered(BlockKey.from(block)).isPresent();
    }
}
