package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class LogisticsStats {
    private static final int MAX_LOGS = 200;

    private final Map<BlockKey, StatusEntry> lastStatus = new HashMap<>();
    private final Map<BlockKey, CounterEntry> counters = new HashMap<>();
    private final List<LogEntry> recentLogs = new ArrayList<>();
    private final long startedAtMillis = Instant.now().toEpochMilli();
    private long totalTransfers;
    private long totalItems;
    private long failedAttempts;
    private long cycleCount;
    private long totalCycleNanos;
    private long lastCycleNanos;
    private long maxCycleNanos;

    public synchronized void recordSuccess(BlockKey key, ItemStack item, int amount, String destination) {
        totalTransfers++;
        totalItems += amount;
        counters.computeIfAbsent(key, ignored -> new CounterEntry()).recordSuccess(item, amount);
        lastStatus.put(key, new StatusEntry(
            "ok",
            item.getType().name().toLowerCase() + " x" + amount + " -> " + destination,
            Instant.now().toEpochMilli()
        ));
        addLog(new LogEntry(key, "ok", item.getType().name().toLowerCase() + " x" + amount + " -> " + destination, Instant.now().toEpochMilli()));
    }

    public synchronized void recordError(BlockKey key, String code, String detail) {
        failedAttempts++;
        counters.computeIfAbsent(key, ignored -> new CounterEntry()).recordError(code);
        lastStatus.put(key, new StatusEntry(code, detail, Instant.now().toEpochMilli()));
        addLog(new LogEntry(key, code, detail, Instant.now().toEpochMilli()));
    }

    public synchronized void recordCycle(long nanos) {
        cycleCount++;
        totalCycleNanos += nanos;
        lastCycleNanos = nanos;
        maxCycleNanos = Math.max(maxCycleNanos, nanos);
    }

    public synchronized Optional<StatusEntry> status(BlockKey key) {
        return Optional.ofNullable(lastStatus.get(key));
    }

    public synchronized long totalTransfers() {
        return totalTransfers;
    }

    public synchronized long totalItems() {
        return totalItems;
    }

    public synchronized long failedAttempts() {
        return failedAttempts;
    }

    public synchronized long cycleCount() {
        return cycleCount;
    }

    public synchronized double averageCycleMillis() {
        if (cycleCount == 0) {
            return 0.0;
        }
        return (totalCycleNanos / 1_000_000.0) / cycleCount;
    }

    public synchronized double lastCycleMillis() {
        return lastCycleNanos / 1_000_000.0;
    }

    public synchronized double maxCycleMillis() {
        return maxCycleNanos / 1_000_000.0;
    }

    public synchronized double transfersPerSecond() {
        long elapsedMillis = Math.max(1, Instant.now().toEpochMilli() - startedAtMillis);
        return totalTransfers * 1000.0 / elapsedMillis;
    }

    public synchronized Optional<CounterSnapshot> counters(BlockKey key) {
        CounterEntry entry = counters.get(key);
        return entry == null ? Optional.empty() : Optional.of(entry.snapshot());
    }

    public synchronized List<LogEntry> recentLogs(int limit) {
        int bounded = Math.max(1, Math.min(limit, MAX_LOGS));
        int from = Math.max(0, recentLogs.size() - bounded);
        return List.copyOf(recentLogs.subList(from, recentLogs.size()));
    }

    public synchronized List<LogEntry> recentLogs(BlockKey key, int limit) {
        int bounded = Math.max(1, Math.min(limit, MAX_LOGS));
        List<LogEntry> result = new ArrayList<>();
        for (int i = recentLogs.size() - 1; i >= 0 && result.size() < bounded; i--) {
            LogEntry entry = recentLogs.get(i);
            if (entry.key().equals(key)) {
                result.add(entry);
            }
        }
        return List.copyOf(result.reversed());
    }

    public synchronized Map<BlockKey, StatusEntry> statusSnapshot() {
        return new LinkedHashMap<>(lastStatus);
    }

    private void addLog(LogEntry entry) {
        recentLogs.add(entry);
        if (recentLogs.size() > MAX_LOGS) {
            recentLogs.removeFirst();
        }
    }

    public record StatusEntry(String code, String detail, long atMillis) {
    }

    public record LogEntry(BlockKey key, String code, String detail, long atMillis) {
    }

    public record CounterSnapshot(long transfers, long items, long errors, Map<String, Long> itemCounts, Map<String, Long> errorCounts) {
    }

    private static final class CounterEntry {
        private long transfers;
        private long items;
        private long errors;
        private final Map<String, Long> itemCounts = new TreeMap<>();
        private final Map<String, Long> errorCounts = new TreeMap<>();

        private void recordSuccess(ItemStack item, int amount) {
            transfers++;
            items += amount;
            itemCounts.merge(item.getType().name().toLowerCase(), (long) amount, Long::sum);
        }

        private void recordError(String code) {
            errors++;
            errorCounts.merge(code, 1L, Long::sum);
        }

        private CounterSnapshot snapshot() {
            return new CounterSnapshot(transfers, items, errors, new LinkedHashMap<>(itemCounts), new LinkedHashMap<>(errorCounts));
        }
    }
}
