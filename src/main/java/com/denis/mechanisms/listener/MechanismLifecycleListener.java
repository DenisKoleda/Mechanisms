package com.denis.mechanisms.listener;

import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismDropService;
import com.denis.mechanisms.logistics.LogisticsModule;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;
import java.util.Optional;

public final class MechanismLifecycleListener implements Listener {
    private final MechanismBlockRegistry registry;
    private final MechanismDropService dropService;
    private final LogisticsModule logisticsModule;

    public MechanismLifecycleListener(MechanismBlockRegistry registry, MechanismDropService dropService, LogisticsModule logisticsModule) {
        this.registry = registry;
        this.dropService = dropService;
        this.logisticsModule = logisticsModule;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (containsMechanism(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (containsMechanism(event.getBlocks())) {
            event.setCancelled(true);
        }
    }

    private void handleExplosion(List<Block> blocks) {
        boolean changed = false;
        for (Block block : List.copyOf(blocks)) {
            Optional<MechanismBlockData> data = registry.getAt(block);
            if (data.isEmpty()) {
                continue;
            }
            dropService.dropStoredContents(block);
            dropService.dropMechanismItem(block, data.get().type());
            registry.remove(block);
            block.setType(Material.AIR, false);
            blocks.remove(block);
            changed = true;
        }
        if (changed) {
            logisticsModule.rebuildNetworks();
        }
    }

    private boolean containsMechanism(List<Block> blocks) {
        return blocks.stream().anyMatch(block -> registry.getAt(block).isPresent());
    }
}
