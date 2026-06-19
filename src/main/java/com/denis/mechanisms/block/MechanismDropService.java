package com.denis.mechanisms.block;

import com.denis.mechanisms.logistics.StackUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class MechanismDropService {
    private final MechanismItemService itemService;

    public MechanismDropService(MechanismItemService itemService) {
        this.itemService = itemService;
    }

    public void dropStoredContents(Block block) {
        BlockState state = block.getState();
        if (!(state instanceof InventoryHolder holder)) {
            return;
        }
        Location location = block.getLocation().add(0.5, 0.5, 0.5);
        for (ItemStack item : holder.getInventory().getContents()) {
            if (StackUtil.isEmpty(item)) {
                continue;
            }
            block.getWorld().dropItemNaturally(location, item.clone());
        }
        holder.getInventory().clear();
    }

    public void dropMechanismItem(Block block, MechanismBlockType type) {
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), itemService.createMechanismItem(type, 1));
    }

    public boolean shouldDropFor(Player player) {
        return player.getGameMode() != GameMode.CREATIVE;
    }
}
