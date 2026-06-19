package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.module.MechanismModule;
import com.denis.mechanisms.visual.VisualEffectService;
import org.bukkit.plugin.java.JavaPlugin;

public final class LogisticsModule implements MechanismModule {
    private final MechanismBlockRegistry registry;
    private final NetworkIndexer networkIndexer;
    private final RouteFinder routeFinder;
    private final PendingTransferStore pendingStore;
    private final TransferEngine transferEngine;
    private final LogisticsScheduler scheduler;
    private final VisualEffectService visualEffectService;

    public LogisticsModule(
        JavaPlugin plugin,
        MechanismBlockRegistry registry,
        MechanismsConfig config,
        NetworkIndexer networkIndexer,
        RouteFinder routeFinder,
        PendingTransferStore pendingStore,
        LogisticsStats stats,
        VisualEffectService visualEffectService
    ) {
        this.registry = registry;
        this.networkIndexer = networkIndexer;
        this.routeFinder = routeFinder;
        this.pendingStore = pendingStore;
        this.visualEffectService = visualEffectService;
        this.transferEngine = new TransferEngine(plugin, registry, config, networkIndexer, routeFinder, pendingStore, stats, visualEffectService);
        this.scheduler = new LogisticsScheduler(plugin, config, transferEngine);
    }

    @Override
    public String name() {
        return "logistics";
    }

    @Override
    public void enable() {
        pendingStore.load();
        rebuildNetworks();
        scheduler.start();
    }

    @Override
    public void disable() {
        scheduler.stop();
        pendingStore.save();
        visualEffectService.shutdown();
    }

    public void rebuildNetworks() {
        registry.cleanupInvalid();
        networkIndexer.rebuild();
        routeFinder.clearCache();
    }

    public void runCycles(int cycles) {
        for (int i = 0; i < cycles; i++) {
            transferEngine.runCycle();
        }
    }

    public NetworkIndexer networkIndexer() {
        return networkIndexer;
    }
}
