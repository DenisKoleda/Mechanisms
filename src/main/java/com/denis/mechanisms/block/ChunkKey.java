package com.denis.mechanisms.block;

import org.bukkit.block.Block;

import java.util.UUID;

public record ChunkKey(UUID worldId, int chunkX, int chunkZ) {
    public static ChunkKey from(Block block) {
        return new ChunkKey(block.getWorld().getUID(), block.getX() >> 4, block.getZ() >> 4);
    }

    public static ChunkKey from(BlockKey key) {
        return new ChunkKey(key.worldId(), key.x() >> 4, key.z() >> 4);
    }
}
