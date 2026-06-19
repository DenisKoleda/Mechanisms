package com.denis.mechanisms;

import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismDropService;
import com.denis.mechanisms.block.MechanismItemService;
import com.denis.mechanisms.command.MechanismsCommand;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.crafting.MechanismRecipeService;
import com.denis.mechanisms.listener.FilterGuiListener;
import com.denis.mechanisms.listener.MechanismBreakListener;
import com.denis.mechanisms.listener.MechanismInteractListener;
import com.denis.mechanisms.listener.MechanismInventoryListener;
import com.denis.mechanisms.listener.MechanismLifecycleListener;
import com.denis.mechanisms.listener.MechanismMenuListener;
import com.denis.mechanisms.listener.MechanismPlaceListener;
import com.denis.mechanisms.listener.MechanismSettingsGuiListener;
import com.denis.mechanisms.listener.NetworkInspectorListener;
import com.denis.mechanisms.listener.RecipeDiscoveryListener;
import com.denis.mechanisms.logistics.FilterService;
import com.denis.mechanisms.logistics.LogisticsModule;
import com.denis.mechanisms.logistics.LogisticsStats;
import com.denis.mechanisms.logistics.NetworkIndexer;
import com.denis.mechanisms.logistics.PendingTransferStore;
import com.denis.mechanisms.logistics.RouteFinder;
import com.denis.mechanisms.selftest.SelfTestService;
import com.denis.mechanisms.ui.FilterGui;
import com.denis.mechanisms.ui.MechanismMenu;
import com.denis.mechanisms.ui.MechanismSettingsGui;
import com.denis.mechanisms.ui.NetworkInspectorGui;
import com.denis.mechanisms.ui.StatusRenderer;
import com.denis.mechanisms.visual.VisualEffectService;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MechanismsPlugin extends JavaPlugin {
    private MechanismsConfig mechanismsConfig;
    private MechanismBlockRegistry blockRegistry;
    private MechanismItemService itemService;
    private MechanismDropService dropService;
    private MechanismRecipeService recipeService;
    private FilterService filterService;
    private NetworkIndexer networkIndexer;
    private RouteFinder routeFinder;
    private PendingTransferStore pendingTransferStore;
    private LogisticsStats logisticsStats;
    private VisualEffectService visualEffectService;
    private LogisticsModule logisticsModule;
    private SelfTestService selfTestService;
    private FilterGui filterGui;
    private MechanismMenu mechanismMenu;
    private MechanismSettingsGui settingsGui;
    private NetworkInspectorGui networkInspectorGui;
    private StatusRenderer statusRenderer;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");
        saveResourceIfMissing("data.yml");
        saveResourceIfMissing("pending.yml");

        mechanismsConfig = new MechanismsConfig(this);
        blockRegistry = new MechanismBlockRegistry(this, mechanismsConfig);
        itemService = new MechanismItemService(this, mechanismsConfig);
        dropService = new MechanismDropService(itemService);
        recipeService = new MechanismRecipeService(this, mechanismsConfig, itemService);
        filterService = new FilterService();
        networkIndexer = new NetworkIndexer(blockRegistry, mechanismsConfig);
        routeFinder = new RouteFinder(blockRegistry, networkIndexer, mechanismsConfig, filterService);
        pendingTransferStore = new PendingTransferStore(this);
        logisticsStats = new LogisticsStats();
        visualEffectService = new VisualEffectService(this, mechanismsConfig, networkIndexer, blockRegistry);
        logisticsModule = new LogisticsModule(this, blockRegistry, mechanismsConfig, networkIndexer, routeFinder, pendingTransferStore, logisticsStats, visualEffectService);
        selfTestService = new SelfTestService(this, mechanismsConfig, blockRegistry, logisticsModule, logisticsStats, pendingTransferStore, recipeService);
        filterGui = new FilterGui(blockRegistry, mechanismsConfig, filterService);
        mechanismMenu = new MechanismMenu(itemService, recipeService, mechanismsConfig);
        networkInspectorGui = new NetworkInspectorGui(blockRegistry, mechanismsConfig, networkIndexer, logisticsStats);
        settingsGui = new MechanismSettingsGui(blockRegistry, filterGui, networkInspectorGui);
        statusRenderer = new StatusRenderer(blockRegistry, mechanismsConfig, networkIndexer, filterService, logisticsStats);

        blockRegistry.load();
        recipeService.reload();
        logisticsModule.enable();
        registerCommands();
        registerListeners();
        scheduleStartupSelfTest();

        getLogger().info("Mechanisms enabled with " + blockRegistry.count() + " mechanism blocks and " + networkIndexer.graph().count() + " networks.");
    }

    @Override
    public void onDisable() {
        if (logisticsModule != null) {
            logisticsModule.disable();
        }
        if (blockRegistry != null) {
            blockRegistry.save();
        }
        if (recipeService != null) {
            recipeService.unregister();
        }
    }

    public void reloadMechanisms() {
        logisticsModule.disable();
        mechanismsConfig.reload();
        recipeService.reload();
        blockRegistry.load();
        logisticsModule.enable();
    }

    private void registerCommands() {
        MechanismsCommand executor = new MechanismsCommand(this, mechanismsConfig, itemService, blockRegistry, networkIndexer, logisticsStats, pendingTransferStore, selfTestService, mechanismMenu, recipeService, networkInspectorGui);
        PluginCommand command = getCommand("mech");
        if (command == null) {
            throw new IllegalStateException("Command /mech is missing from plugin.yml");
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    private void registerListeners() {
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new MechanismPlaceListener(itemService, mechanismsConfig, blockRegistry, logisticsModule), this);
        pluginManager.registerEvents(new MechanismBreakListener(blockRegistry, dropService, mechanismsConfig, logisticsModule), this);
        pluginManager.registerEvents(new MechanismLifecycleListener(blockRegistry, dropService, logisticsModule), this);
        pluginManager.registerEvents(new MechanismInventoryListener(blockRegistry), this);
        pluginManager.registerEvents(new MechanismInteractListener(itemService, mechanismsConfig, blockRegistry, statusRenderer, filterGui, settingsGui, visualEffectService), this);
        pluginManager.registerEvents(new FilterGuiListener(filterGui), this);
        pluginManager.registerEvents(new MechanismMenuListener(mechanismMenu), this);
        pluginManager.registerEvents(new MechanismSettingsGuiListener(settingsGui), this);
        pluginManager.registerEvents(new NetworkInspectorListener(networkInspectorGui), this);
        pluginManager.registerEvents(new RecipeDiscoveryListener(recipeService), this);
    }

    private void scheduleStartupSelfTest() {
        if (!Boolean.getBoolean("mechanisms.selftestOnStartup")) {
            return;
        }
        getServer().getScheduler().runTaskLater(this, () -> {
            boolean keepArea = Boolean.getBoolean("mechanisms.selftestKeep");
            selfTestService.run(getServer().getConsoleSender(), keepArea);
            if (Boolean.getBoolean("mechanisms.stopAfterSelftest")) {
                getServer().shutdown();
            }
        }, 40L);
    }

    private void saveResourceIfMissing(String resourceName) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            throw new IllegalStateException("Could not create plugin data folder");
        }
        if (!getDataFolder().toPath().resolve(resourceName).toFile().exists()) {
            saveResource(resourceName, false);
        }
    }
}
