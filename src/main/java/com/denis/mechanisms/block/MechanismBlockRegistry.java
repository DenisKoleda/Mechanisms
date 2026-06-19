package com.denis.mechanisms.block;

import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.logistics.FilterSettings;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

public final class MechanismBlockRegistry {
    private final JavaPlugin plugin;
    private final MechanismsConfig config;
    private final File dataFile;
    private final Map<BlockKey, MechanismBlockData> blocksByKey = new HashMap<>();
    private final Map<ChunkKey, Set<BlockKey>> blocksByChunk = new HashMap<>();
    private long version;

    public MechanismBlockRegistry(JavaPlugin plugin, MechanismsConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public synchronized void load() {
        blocksByKey.clear();
        blocksByChunk.clear();
        if (!dataFile.exists()) {
            save();
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        for (Map<?, ?> row : yaml.getMapList("blocks")) {
            readBlock(row).ifPresent(this::putInMemory);
        }
        version++;
    }

    public synchronized void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for data.yml");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        List<Map<String, Object>> rows = blocksByKey.values().stream()
            .sorted(Comparator.comparing(MechanismBlockData::worldName)
                .thenComparingInt(MechanismBlockData::x)
                .thenComparingInt(MechanismBlockData::y)
                .thenComparingInt(MechanismBlockData::z))
            .map(this::toRow)
            .toList();
        yaml.set("blocks", rows);

        File tempFile = new File(dataFile.getParentFile(), dataFile.getName() + ".tmp");
        try {
            yaml.save(tempFile);
            try {
                Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile.toPath(), dataFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save Mechanisms data.yml: " + ex.getMessage());
        }
    }

    public synchronized MechanismBlockData add(Block block, MechanismBlockType type) {
        MechanismBlockData data = new MechanismBlockData(
            UUID.randomUUID(),
            type,
            block.getWorld().getUID(),
            block.getWorld().getName(),
            block.getX(),
            block.getY(),
            block.getZ(),
            block.getType(),
            type.supportsFilter() ? config.defaultFilterSettings() : config.defaultFilterSettings().withItems(List.of()),
            MechanismIoSide.AUTO,
            RedstoneMode.IGNORE,
            TrashMode.DISABLED,
            PipeChannel.DEFAULT,
            UpgradeModules.EMPTY,
            System.currentTimeMillis()
        );
        putInMemory(data);
        version++;
        save();
        return data;
    }

    public synchronized boolean remove(Block block) {
        MechanismBlockData removed = blocksByKey.remove(BlockKey.from(block));
        if (removed == null) {
            return false;
        }
        Set<BlockKey> keys = blocksByChunk.get(removed.chunkKey());
        if (keys != null) {
            keys.remove(removed.key());
            if (keys.isEmpty()) {
                blocksByChunk.remove(removed.chunkKey());
            }
        }
        version++;
        save();
        return true;
    }

    public synchronized Optional<MechanismBlockData> getAt(Block block) {
        MechanismBlockData data = blocksByKey.get(BlockKey.from(block));
        if (data == null || block.getType() != data.material()) {
            return Optional.empty();
        }
        return Optional.of(data);
    }

    public synchronized Optional<MechanismBlockData> getRegistered(BlockKey key) {
        return Optional.ofNullable(blocksByKey.get(key));
    }

    public synchronized boolean isMechanismBlock(Block block) {
        return getAt(block).isPresent();
    }

    public synchronized void updateFilter(BlockKey key, FilterSettings filter) {
        MechanismBlockData current = blocksByKey.get(key);
        if (current == null || !current.type().supportsFilter()) {
            return;
        }
        blocksByKey.put(key, current.withFilter(filter));
        version++;
        save();
    }

    public synchronized void updateIoSide(BlockKey key, MechanismIoSide side) {
        MechanismBlockData current = blocksByKey.get(key);
        if (current == null || !current.type().supportsIoSide()) {
            return;
        }
        blocksByKey.put(key, current.withIoSide(side));
        version++;
        save();
    }

    public synchronized void updateRedstoneMode(BlockKey key, RedstoneMode mode) {
        MechanismBlockData current = blocksByKey.get(key);
        if (current == null || current.type() != MechanismBlockType.EXTRACTOR) {
            return;
        }
        blocksByKey.put(key, current.withRedstoneMode(mode));
        version++;
        save();
    }

    public synchronized void updateTrashMode(BlockKey key, TrashMode mode) {
        MechanismBlockData current = blocksByKey.get(key);
        if (current == null || current.type() != MechanismBlockType.TRASH) {
            return;
        }
        blocksByKey.put(key, current.withTrashMode(mode));
        version++;
        save();
    }

    public synchronized void updatePipeChannel(BlockKey key, PipeChannel channel) {
        MechanismBlockData current = blocksByKey.get(key);
        if (current == null || !current.type().isPipe()) {
            return;
        }
        blocksByKey.put(key, current.withPipeChannel(channel));
        version++;
        save();
    }

    public synchronized void updateUpgrades(BlockKey key, UpgradeModules upgrades) {
        MechanismBlockData current = blocksByKey.get(key);
        if (current == null) {
            return;
        }
        blocksByKey.put(key, current.withUpgrades(upgrades));
        version++;
        save();
    }

    public synchronized boolean updateTypeAndMaterial(BlockKey key, MechanismBlockType type, Material material) {
        MechanismBlockData current = blocksByKey.get(key);
        if (current == null) {
            return false;
        }
        blocksByKey.put(key, current.withTypeAndMaterial(type, material));
        version++;
        save();
        return true;
    }

    public synchronized Collection<MechanismBlockData> all() {
        return new ArrayList<>(blocksByKey.values());
    }

    public synchronized List<MechanismBlockData> byType(MechanismBlockType type) {
        return blocksByKey.values().stream()
            .filter(data -> data.type() == type)
            .sorted(Comparator.comparing(MechanismBlockData::worldName)
                .thenComparingInt(MechanismBlockData::x)
                .thenComparingInt(MechanismBlockData::y)
                .thenComparingInt(MechanismBlockData::z))
            .toList();
    }

    public synchronized Set<BlockKey> keysInChunk(ChunkKey chunkKey) {
        return Set.copyOf(blocksByChunk.getOrDefault(chunkKey, Set.of()));
    }

    public synchronized int count() {
        return blocksByKey.size();
    }

    public synchronized Map<MechanismBlockType, Integer> countByType() {
        Map<MechanismBlockType, Integer> counts = new EnumMap<>(MechanismBlockType.class);
        for (MechanismBlockType type : MechanismBlockType.values()) {
            counts.put(type, 0);
        }
        for (MechanismBlockData data : blocksByKey.values()) {
            counts.merge(data.type(), 1, Integer::sum);
        }
        return counts;
    }

    public synchronized Map<String, Integer> countByWorld() {
        Map<String, Integer> counts = new TreeMap<>();
        for (MechanismBlockData data : blocksByKey.values()) {
            counts.merge(data.worldName(), 1, Integer::sum);
        }
        return counts;
    }

    public synchronized int cleanupInvalid() {
        List<MechanismBlockData> invalid = blocksByKey.values().stream()
            .filter(data -> data.world().isPresent())
            .filter(this::canCheckPhysicalBlock)
            .filter(data -> !data.isStillPhysical())
            .toList();
        for (MechanismBlockData data : invalid) {
            removeFromMemory(data);
        }
        if (!invalid.isEmpty()) {
            version++;
            save();
        }
        return invalid.size();
    }

    public synchronized long version() {
        return version;
    }

    private boolean canCheckPhysicalBlock(MechanismBlockData data) {
        if (config.allowUnloadedChunks()) {
            return true;
        }
        return data.world()
            .map(world -> world.isChunkLoaded(data.x() >> 4, data.z() >> 4))
            .orElse(false);
    }

    private void putInMemory(MechanismBlockData data) {
        blocksByKey.put(data.key(), data);
        blocksByChunk.computeIfAbsent(data.chunkKey(), ignored -> new HashSet<>()).add(data.key());
    }

    private void removeFromMemory(MechanismBlockData data) {
        blocksByKey.remove(data.key());
        Set<BlockKey> keys = blocksByChunk.get(data.chunkKey());
        if (keys != null) {
            keys.remove(data.key());
            if (keys.isEmpty()) {
                blocksByChunk.remove(data.chunkKey());
            }
        }
    }

    private Optional<MechanismBlockData> readBlock(Map<?, ?> row) {
        try {
            UUID id = uuidValue(row.get("id"), UUID.randomUUID());
            MechanismBlockType type = MechanismBlockType.fromToken(String.valueOf(row.get("type"))).orElse(null);
            if (type == null) {
                return Optional.empty();
            }
            UUID worldId = UUID.fromString(String.valueOf(row.get("worldId")));
            String worldName = stringValue(row.get("world"), "world");
            int x = intValue(row.get("x"), 0);
            int y = intValue(row.get("y"), 0);
            int z = intValue(row.get("z"), 0);
            Material material = Material.matchMaterial(stringValue(row.get("material"), config.materialFor(type).name()).toUpperCase(Locale.ROOT));
            if (material == null || !material.isBlock()) {
                material = config.materialFor(type);
            }
            FilterSettings filter = config.defaultFilterSettings();
            Object filterValue = row.get("filter");
            if (filterValue instanceof Map<?, ?> filterMap) {
                filter = FilterSettings.fromMap(filterMap, filter);
            }
            MechanismIoSide ioSide = MechanismIoSide.parse(stringValue(row.get("ioSide"), "auto"), MechanismIoSide.AUTO);
            RedstoneMode redstoneMode = RedstoneMode.parse(stringValue(row.get("redstoneMode"), "ignore"), RedstoneMode.IGNORE);
            TrashMode trashMode = TrashMode.parse(stringValue(row.get("trashMode"), "disabled"), TrashMode.DISABLED);
            PipeChannel pipeChannel = PipeChannel.parse(stringValue(row.get("pipeChannel"), "default"), PipeChannel.DEFAULT);
            UpgradeModules upgrades = UpgradeModules.EMPTY;
            Object upgradesValue = row.get("upgrades");
            if (upgradesValue instanceof Map<?, ?> upgradesMap) {
                upgrades = UpgradeModules.fromMap(upgradesMap);
            }
            long placedAt = longValue(row.get("placedAt"), System.currentTimeMillis());
            return Optional.of(new MechanismBlockData(id, type, worldId, worldName, x, y, z, material, filter, ioSide, redstoneMode, trashMode, pipeChannel, upgrades, placedAt));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Skipping invalid mechanism block record: " + row);
            return Optional.empty();
        }
    }

    private Map<String, Object> toRow(MechanismBlockData data) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", data.id().toString());
        row.put("type", data.type().token());
        row.put("world", data.worldName());
        row.put("worldId", data.worldId().toString());
        row.put("x", data.x());
        row.put("y", data.y());
        row.put("z", data.z());
        row.put("material", data.material().name());
        row.put("filter", data.filter().toMap());
        row.put("ioSide", data.ioSide().token());
        row.put("redstoneMode", data.redstoneMode().token());
        row.put("trashMode", data.trashMode().token());
        row.put("pipeChannel", data.pipeChannel().token());
        row.put("upgrades", data.upgrades().toMap());
        row.put("placedAt", data.placedAt());
        return row;
    }

    private UUID uuidValue(Object value, UUID fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private long longValue(Object value, long fallback) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
