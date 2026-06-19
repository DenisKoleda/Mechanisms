package com.denis.mechanisms.listener;

import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismItemService;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.logistics.LogisticsModule;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public final class MechanismPlaceListener implements Listener {
    private final MechanismItemService itemService;
    private final MechanismsConfig config;
    private final MechanismBlockRegistry registry;
    private final LogisticsModule logisticsModule;

    public MechanismPlaceListener(MechanismItemService itemService, MechanismsConfig config, MechanismBlockRegistry registry, LogisticsModule logisticsModule) {
        this.itemService = itemService;
        this.config = config;
        this.registry = registry;
        this.logisticsModule = logisticsModule;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        itemService.typeFromItem(event.getItemInHand()).ifPresent(type -> {
            if (!event.getPlayer().hasPermission("mechanisms.use") && !event.getPlayer().hasPermission("mechanisms.admin")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(config.message("noPermission"));
                return;
            }
            registry.add(event.getBlockPlaced(), type);
            logisticsModule.rebuildNetworks();
            event.getPlayer().sendMessage(Component.text("Механизм зарегистрирован: ").append(Component.text(type.russianName(), type.color())));
        });
    }
}
