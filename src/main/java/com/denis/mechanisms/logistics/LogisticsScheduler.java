package com.denis.mechanisms.logistics;

import com.denis.mechanisms.config.MechanismsConfig;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.java.JavaPlugin;

public final class LogisticsScheduler {
    private final JavaPlugin plugin;
    private final MechanismsConfig config;
    private final TransferEngine transferEngine;
    private BukkitTask task;

    public LogisticsScheduler(JavaPlugin plugin, MechanismsConfig config, TransferEngine transferEngine) {
        this.plugin = plugin;
        this.config = config;
        this.transferEngine = transferEngine;
    }

    public void start() {
        stop();
        if (!config.logisticsEnabled()) {
            return;
        }
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, transferEngine::runCycle, config.tickInterval(), config.tickInterval());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
