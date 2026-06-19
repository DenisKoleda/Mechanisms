package com.denis.mechanisms.listener;

import com.denis.mechanisms.ui.MechanismMenu;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MechanismMenuListener implements Listener {
    private final MechanismMenu menu;

    public MechanismMenuListener(MechanismMenu menu) {
        this.menu = menu;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        menu.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        menu.handleDrag(event);
    }
}
