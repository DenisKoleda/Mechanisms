package com.denis.mechanisms.listener;

import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismDropService;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.logistics.LogisticsModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class MechanismBreakListener implements Listener {
    private final MechanismBlockRegistry registry;
    private final MechanismDropService dropService;
    private final MechanismsConfig config;
    private final LogisticsModule logisticsModule;

    public MechanismBreakListener(MechanismBlockRegistry registry, MechanismDropService dropService, MechanismsConfig config, LogisticsModule logisticsModule) {
        this.registry = registry;
        this.dropService = dropService;
        this.config = config;
        this.logisticsModule = logisticsModule;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        registry.getAt(event.getBlock()).ifPresent(data -> {
            if (!event.getPlayer().hasPermission("mechanisms.use") && !event.getPlayer().hasPermission("mechanisms.admin")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(config.message("noPermission"));
                return;
            }
            dropService.dropStoredContents(event.getBlock());
            if (event.isDropItems() && dropService.shouldDropFor(event.getPlayer())) {
                event.setDropItems(false);
                dropService.dropMechanismItem(event.getBlock(), data.type());
            }
            registry.remove(event.getBlock());
            logisticsModule.rebuildNetworks();
            event.getPlayer().sendMessage(Component.text("Механизм снят: " + data.type().russianName(), NamedTextColor.YELLOW));
        });
    }
}
