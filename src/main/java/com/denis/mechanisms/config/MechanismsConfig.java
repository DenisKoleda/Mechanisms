package com.denis.mechanisms.config;

import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.UpgradeModules;
import com.denis.mechanisms.logistics.FilterMode;
import com.denis.mechanisms.logistics.FilterSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class MechanismsConfig {
    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<MechanismBlockType, Material> materials = new EnumMap<>(MechanismBlockType.class);
    private final File messagesFile;

    private YamlConfiguration messages = new YamlConfiguration();
    private boolean debug;
    private boolean logisticsEnabled;
    private int tickInterval;
    private int itemsPerTransfer;
    private int fastPipeItemsPerTransfer;
    private int expressPipeItemsPerTransfer;
    private int maxTransfersPerTick;
    private int maxNetworkNodes;
    private int maxRouteLength;
    private boolean allowCrossChunk;
    private boolean allowUnloadedChunks;
    private FilterMode defaultFilterMode;
    private boolean exactMetaDefault;
    private boolean visualsEnabled;
    private boolean particles;
    private boolean itemDisplayEcho;
    private double itemDisplayScale;
    private boolean sounds;
    private boolean recipesEnabled;
    private CraftingProfile craftingProfile;
    private int upgradeSpeedBonusPerLevel;
    private int upgradeStackBonusPerLevel;
    private int upgradePriorityBonusPerLevel;
    private int upgradeRangeBonusPerLevel;
    private int maxFilterPages;

    public MechanismsConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        if (messagesFile.exists()) {
            messages = YamlConfiguration.loadConfiguration(messagesFile);
        }

        this.debug = plugin.getConfig().getBoolean("debug", false);
        this.logisticsEnabled = plugin.getConfig().getBoolean("logistics.enabled", true);
        this.tickInterval = boundedInt("logistics.tickInterval", 10, 1, 200);
        this.itemsPerTransfer = boundedInt("logistics.itemsPerTransfer", 8, 1, 64);
        this.fastPipeItemsPerTransfer = boundedInt("logistics.fastPipeItemsPerTransfer", 16, 1, 64);
        this.expressPipeItemsPerTransfer = boundedInt("logistics.expressPipeItemsPerTransfer", 32, 1, 64);
        this.maxTransfersPerTick = boundedInt("logistics.maxTransfersPerTick", 32, 1, 1024);
        this.maxNetworkNodes = boundedInt("logistics.maxNetworkNodes", 256, 4, 8192);
        this.maxRouteLength = boundedInt("logistics.maxRouteLength", 128, 1, 8192);
        this.allowCrossChunk = plugin.getConfig().getBoolean("logistics.allowCrossChunk", true);
        this.allowUnloadedChunks = plugin.getConfig().getBoolean("logistics.allowUnloadedChunks", false);
        this.defaultFilterMode = FilterMode.parse(plugin.getConfig().getString("logistics.defaultFilterMode"), FilterMode.WHITELIST);
        this.exactMetaDefault = plugin.getConfig().getBoolean("logistics.exactMetaDefault", false);
        this.visualsEnabled = plugin.getConfig().getBoolean("visuals.enabled", true);
        this.particles = plugin.getConfig().getBoolean("visuals.particles", true);
        this.itemDisplayEcho = plugin.getConfig().getBoolean("visuals.itemDisplayEcho", true);
        this.itemDisplayScale = boundedDouble("visuals.itemDisplayScale", 0.35, 0.05, 1.0);
        this.sounds = plugin.getConfig().getBoolean("visuals.sounds", false);
        this.recipesEnabled = plugin.getConfig().getBoolean("recipes.enabled", true);
        this.craftingProfile = CraftingProfile.parse(plugin.getConfig().getString("recipes.profile"), CraftingProfile.NORMAL);
        this.upgradeSpeedBonusPerLevel = boundedInt("upgrades.speedBonusPerLevel", 4, 0, 64);
        this.upgradeStackBonusPerLevel = boundedInt("upgrades.stackBonusPerLevel", 8, 0, 64);
        this.upgradePriorityBonusPerLevel = boundedInt("upgrades.priorityBonusPerLevel", 10, 0, 1000);
        this.upgradeRangeBonusPerLevel = boundedInt("upgrades.rangeBonusPerLevel", 32, 0, 8192);
        this.maxFilterPages = boundedInt("upgrades.maxFilterPages", 4, 1, 8);

        materials.clear();
        materials.put(MechanismBlockType.EXTRACTOR, material("materials.extractor", Material.HOPPER));
        materials.put(MechanismBlockType.PIPE, material("materials.pipe", fallbackMaterial("COPPER_GRATE", Material.GLASS)));
        materials.put(MechanismBlockType.PIPE_FAST, material("materials.pipeFast", fallbackMaterial("EXPOSED_COPPER_GRATE", Material.GLASS)));
        materials.put(MechanismBlockType.PIPE_EXPRESS, material("materials.pipeExpress", fallbackMaterial("OXIDIZED_COPPER_GRATE", fallbackMaterial("TINTED_GLASS", Material.GLASS))));
        materials.put(MechanismBlockType.ROUTER, material("materials.router", Material.TARGET));
        materials.put(MechanismBlockType.INSERTER, material("materials.inserter", Material.DROPPER));
        materials.put(MechanismBlockType.OVERFLOW, material("materials.overflow", Material.BARREL));
        materials.put(MechanismBlockType.TRASH, material("materials.trash", Material.CAULDRON));
    }

    public Component message(String key) {
        String prefix = messages.getString("prefix", plugin.getConfig().getString("messages.prefix", ""));
        String body = messages.getString(key, plugin.getConfig().getString("messages." + key, key));
        return miniMessage.deserialize(prefix + body);
    }

    public Component parse(String text) {
        return miniMessage.deserialize(text == null ? "" : text);
    }

    public FilterSettings defaultFilterSettings() {
        return FilterSettings.empty(defaultFilterMode, exactMetaDefault);
    }

    public Material materialFor(MechanismBlockType type) {
        return materials.get(type);
    }

    public boolean debug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        plugin.getConfig().set("debug", debug);
        plugin.saveConfig();
    }

    public boolean logisticsEnabled() {
        return logisticsEnabled;
    }

    public int tickInterval() {
        return tickInterval;
    }

    public int itemsPerTransfer() {
        return itemsPerTransfer;
    }

    public int itemsPerTransferFor(MechanismBlockType type) {
        return switch (type) {
            case PIPE_FAST -> fastPipeItemsPerTransfer;
            case PIPE_EXPRESS -> expressPipeItemsPerTransfer;
            default -> itemsPerTransfer;
        };
    }

    public int itemsPerTransferFor(MechanismBlockData data) {
        int amount = itemsPerTransferFor(data.type());
        amount += data.upgrades().speedLevel() * upgradeSpeedBonusPerLevel;
        amount += data.upgrades().stackLevel() * upgradeStackBonusPerLevel;
        return Math.max(1, Math.min(64, amount));
    }

    public int maxItemsPerTransfer() {
        return Math.max(itemsPerTransfer, Math.max(fastPipeItemsPerTransfer, expressPipeItemsPerTransfer))
            + 3 * (upgradeSpeedBonusPerLevel + upgradeStackBonusPerLevel);
    }

    public int maxTransfersPerTick() {
        return maxTransfersPerTick;
    }

    public int maxNetworkNodes() {
        return maxNetworkNodes;
    }

    public int maxRouteLength() {
        return maxRouteLength;
    }

    public int routeLengthFor(MechanismBlockData data) {
        return Math.max(1, Math.min(8192, maxRouteLength + data.upgrades().rangeLevel() * upgradeRangeBonusPerLevel));
    }

    public int priorityBonus(UpgradeModules upgrades) {
        return upgrades.priorityLevel() * upgradePriorityBonusPerLevel;
    }

    public int maxFilterPages(UpgradeModules upgrades) {
        return Math.max(1, Math.min(maxFilterPages, 1 + upgrades.filterLevel()));
    }

    public boolean allowCrossChunk() {
        return allowCrossChunk;
    }

    public boolean allowUnloadedChunks() {
        return allowUnloadedChunks;
    }

    public boolean visualsEnabled() {
        return visualsEnabled;
    }

    public boolean particles() {
        return particles;
    }

    public boolean itemDisplayEcho() {
        return itemDisplayEcho;
    }

    public double itemDisplayScale() {
        return itemDisplayScale;
    }

    public boolean sounds() {
        return sounds;
    }

    public boolean recipesEnabled() {
        return recipesEnabled;
    }

    public CraftingProfile craftingProfile() {
        return craftingProfile;
    }

    private Material material(String path, Material fallback) {
        String configured = plugin.getConfig().getString(path, fallback.name());
        Material material = Material.matchMaterial(configured == null ? "" : configured.trim().toUpperCase(Locale.ROOT));
        if (material == null || !material.isBlock()) {
            plugin.getLogger().warning("Invalid material at " + path + ": " + configured + ". Using " + fallback.name());
            return fallback;
        }
        return material;
    }

    private Material fallbackMaterial(String preferred, Material fallback) {
        Material material = Material.matchMaterial(preferred);
        return material == null ? fallback : material;
    }

    private int boundedInt(String path, int fallback, int min, int max) {
        int value = plugin.getConfig().getInt(path, fallback);
        return Math.max(min, Math.min(max, value));
    }

    private double boundedDouble(String path, double fallback, double min, double max) {
        double value = plugin.getConfig().getDouble(path, fallback);
        return Math.max(min, Math.min(max, value));
    }
}
