package com.denis.mechanisms.listener;

import com.denis.mechanisms.ui.FilterGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class FilterGuiListener implements Listener {
    private final FilterGui filterGui;

    public FilterGuiListener(FilterGui filterGui) {
        this.filterGui = filterGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        filterGui.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        filterGui.handleDrag(event);
    }
}
