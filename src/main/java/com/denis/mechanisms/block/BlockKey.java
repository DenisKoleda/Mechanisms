package com.denis.mechanisms.block;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

public record BlockKey(UUID worldId, int x, int y, int z) implements Comparable<BlockKey> {
    public static final Comparator<BlockKey> ORDER = Comparator
        .comparing((BlockKey key) -> key.worldId().toString())
        .thenComparingInt(BlockKey::x)
        .thenComparingInt(BlockKey::y)
        .thenComparingInt(BlockKey::z);

    public static BlockKey from(Block block) {
        return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    public BlockKey relative(BlockFace face) {
        return new BlockKey(worldId, x + face.getModX(), y + face.getModY(), z + face.getModZ());
    }

    public Optional<Block> block() {
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(world.getBlockAt(x, y, z));
    }

    public String shortString() {
        return x + "," + y + "," + z;
    }

    @Override
    public int compareTo(BlockKey other) {
        return ORDER.compare(this, other);
    }
}
