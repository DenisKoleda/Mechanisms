package com.denis.mechanisms.block;

import com.denis.mechanisms.logistics.FilterSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Optional;
import java.util.UUID;

public record MechanismBlockData(
    UUID id,
    MechanismBlockType type,
    UUID worldId,
    String worldName,
    int x,
    int y,
    int z,
    Material material,
    FilterSettings filter,
    MechanismIoSide ioSide,
    RedstoneMode redstoneMode,
    TrashMode trashMode,
    PipeChannel pipeChannel,
    UpgradeModules upgrades,
    long placedAt
) {
    public MechanismBlockData {
        ioSide = ioSide == null ? MechanismIoSide.AUTO : ioSide;
        redstoneMode = redstoneMode == null ? RedstoneMode.IGNORE : redstoneMode;
        trashMode = trashMode == null ? TrashMode.DISABLED : trashMode;
        pipeChannel = pipeChannel == null ? PipeChannel.DEFAULT : pipeChannel;
        upgrades = upgrades == null ? UpgradeModules.EMPTY : upgrades;
    }

    public BlockKey key() {
        return new BlockKey(worldId, x, y, z);
    }

    public ChunkKey chunkKey() {
        return ChunkKey.from(key());
    }

    public Optional<World> world() {
        World world = Bukkit.getWorld(worldId);
        if (world == null && worldName != null && !worldName.isBlank()) {
            world = Bukkit.getWorld(worldName);
        }
        return Optional.ofNullable(world);
    }

    public Optional<Block> block() {
        return world().map(world -> world.getBlockAt(x, y, z));
    }

    public boolean isStillPhysical() {
        return block().map(block -> block.getType() == material).orElse(false);
    }

    public MechanismBlockData withFilter(FilterSettings newFilter) {
        return new MechanismBlockData(id, type, worldId, worldName, x, y, z, material, newFilter, ioSide, redstoneMode, trashMode, pipeChannel, upgrades, placedAt);
    }

    public MechanismBlockData withIoSide(MechanismIoSide newIoSide) {
        return new MechanismBlockData(id, type, worldId, worldName, x, y, z, material, filter, newIoSide, redstoneMode, trashMode, pipeChannel, upgrades, placedAt);
    }

    public MechanismBlockData withRedstoneMode(RedstoneMode newRedstoneMode) {
        return new MechanismBlockData(id, type, worldId, worldName, x, y, z, material, filter, ioSide, newRedstoneMode, trashMode, pipeChannel, upgrades, placedAt);
    }

    public MechanismBlockData withTrashMode(TrashMode newTrashMode) {
        return new MechanismBlockData(id, type, worldId, worldName, x, y, z, material, filter, ioSide, redstoneMode, newTrashMode, pipeChannel, upgrades, placedAt);
    }

    public MechanismBlockData withPipeChannel(PipeChannel newPipeChannel) {
        return new MechanismBlockData(id, type, worldId, worldName, x, y, z, material, filter, ioSide, redstoneMode, trashMode, newPipeChannel, upgrades, placedAt);
    }

    public MechanismBlockData withUpgrades(UpgradeModules newUpgrades) {
        return new MechanismBlockData(id, type, worldId, worldName, x, y, z, material, filter, ioSide, redstoneMode, trashMode, pipeChannel, newUpgrades, placedAt);
    }

    public MechanismBlockData withTypeAndMaterial(MechanismBlockType newType, Material newMaterial) {
        return new MechanismBlockData(id, newType, worldId, worldName, x, y, z, newMaterial, filter, ioSide, redstoneMode, trashMode, pipeChannel, upgrades, placedAt);
    }
}
