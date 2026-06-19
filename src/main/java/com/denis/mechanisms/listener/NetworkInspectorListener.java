package com.denis.mechanisms.listener;

import com.denis.mechanisms.ui.NetworkInspectorGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class NetworkInspectorListener implements Listener {
    private final NetworkInspectorGui inspectorGui;

    public NetworkInspectorListener(NetworkInspectorGui inspectorGui) {
        this.inspectorGui = inspectorGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        inspectorGui.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        inspectorGui.handleDrag(event);
    }
}
