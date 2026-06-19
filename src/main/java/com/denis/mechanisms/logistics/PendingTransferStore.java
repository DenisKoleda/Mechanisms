package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PendingTransferStore {
    private final JavaPlugin plugin;
    private final File pendingFile;
    private final List<PendingTransfer> pending = new ArrayList<>();

    public PendingTransferStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pendingFile = new File(plugin.getDataFolder(), "pending.yml");
    }

    public synchronized void load() {
        pending.clear();
        if (!pendingFile.exists()) {
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(pendingFile);
        for (Map<?, ?> row : yaml.getMapList("pending")) {
            read(row).ifPresent(pending::add);
        }
    }

    public synchronized void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder for pending.yml");
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("pending", pending.stream().map(this::toRow).toList());
        File tempFile = new File(pendingFile.getParentFile(), pendingFile.getName() + ".tmp");
        try {
            yaml.save(tempFile);
            try {
                Files.move(tempFile.toPath(), pendingFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(tempFile.toPath(), pendingFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save Mechanisms pending.yml: " + ex.getMessage());
        }
    }

    public synchronized void add(BlockKey preferredSource, ItemStack item, String reason) {
        if (StackUtil.isEmpty(item)) {
            return;
        }
        pending.add(new PendingTransfer(UUID.randomUUID(), System.currentTimeMillis(), 0, preferredSource, item.clone(), reason));
        save();
    }

    public synchronized List<PendingTransfer> all() {
        return new ArrayList<>(pending);
    }

    public synchronized void remove(UUID id) {
        pending.removeIf(entry -> entry.id().equals(id));
        save();
    }

    public synchronized void updateAttempts(UUID id, int attempts) {
        for (int i = 0; i < pending.size(); i++) {
            PendingTransfer current = pending.get(i);
            if (current.id().equals(id)) {
                pending.set(i, current.withAttempts(attempts));
                save();
                return;
            }
        }
    }

    public synchronized void updateItemAndAttempts(UUID id, ItemStack item, int attempts) {
        if (StackUtil.isEmpty(item)) {
            remove(id);
            return;
        }
        for (int i = 0; i < pending.size(); i++) {
            PendingTransfer current = pending.get(i);
            if (current.id().equals(id)) {
                pending.set(i, current.withItemAndAttempts(item, attempts));
                save();
                return;
            }
        }
    }

    public synchronized int count() {
        return pending.size();
    }

    private java.util.Optional<PendingTransfer> read(Map<?, ?> row) {
        try {
            UUID id = UUID.fromString(String.valueOf(row.get("id")));
            long createdAt = longValue(row.get("createdAt"), System.currentTimeMillis());
            int attempts = intValue(row.get("attempts"), 0);
            UUID worldId = UUID.fromString(String.valueOf(row.get("sourceWorldId")));
            int x = intValue(row.get("sourceX"), 0);
            int y = intValue(row.get("sourceY"), 0);
            int z = intValue(row.get("sourceZ"), 0);
            Object itemValue = row.get("item");
            if (!(itemValue instanceof ItemStack item) || StackUtil.isEmpty(item)) {
                return java.util.Optional.empty();
            }
            Object reasonValue = row.get("reason");
            String reason = reasonValue == null ? "unknown" : String.valueOf(reasonValue);
            return java.util.Optional.of(new PendingTransfer(id, createdAt, attempts, new BlockKey(worldId, x, y, z), item.clone(), reason));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Skipping invalid pending transfer record: " + row);
            return java.util.Optional.empty();
        }
    }

    private Map<String, Object> toRow(PendingTransfer transfer) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", transfer.id().toString());
        row.put("createdAt", transfer.createdAt());
        row.put("attempts", transfer.attempts());
        row.put("sourceWorldId", transfer.preferredSource().worldId().toString());
        row.put("sourceX", transfer.preferredSource().x());
        row.put("sourceY", transfer.preferredSource().y());
        row.put("sourceZ", transfer.preferredSource().z());
        row.put("item", transfer.item().clone());
        row.put("reason", transfer.reason());
        return row;
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
