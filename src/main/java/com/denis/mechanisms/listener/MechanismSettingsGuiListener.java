package com.denis.mechanisms.listener;

import com.denis.mechanisms.ui.MechanismSettingsGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class MechanismSettingsGuiListener implements Listener {
    private final MechanismSettingsGui settingsGui;

    public MechanismSettingsGuiListener(MechanismSettingsGui settingsGui) {
        this.settingsGui = settingsGui;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        settingsGui.handleClick(event);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        settingsGui.handleDrag(event);
    }
}
