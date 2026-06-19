package com.denis.mechanisms.selftest;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.MechanismIoSide;
import com.denis.mechanisms.block.RedstoneMode;
import com.denis.mechanisms.block.TrashMode;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.crafting.MechanismRecipeService;
import com.denis.mechanisms.logistics.FilterMode;
import com.denis.mechanisms.logistics.FilterSettings;
import com.denis.mechanisms.logistics.LogisticsStats;
import com.denis.mechanisms.logistics.LogisticsModule;
import com.denis.mechanisms.logistics.MatchMode;
import com.denis.mechanisms.logistics.PendingTransferStore;
import com.denis.mechanisms.logistics.RouteMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SelfTestService {
    private static final int SIZE_X = 36;
    private static final int SIZE_Y = 8;
    private static final int SIZE_Z = 28;

    private final JavaPlugin plugin;
    private final MechanismsConfig config;
    private final MechanismBlockRegistry registry;
    private final LogisticsModule logisticsModule;
    private final LogisticsStats stats;
    private final PendingTransferStore pendingStore;
    private final MechanismRecipeService recipeService;

    public SelfTestService(JavaPlugin plugin, MechanismsConfig config, MechanismBlockRegistry registry, LogisticsModule logisticsModule, LogisticsStats stats, PendingTransferStore pendingStore, MechanismRecipeService recipeService) {
        this.plugin = plugin;
        this.config = config;
        this.registry = registry;
        this.logisticsModule = logisticsModule;
        this.stats = stats;
        this.pendingStore = pendingStore;
        this.recipeService = recipeService;
    }

    public Result run(CommandSender sender, boolean keepArea) {
        World world = sender instanceof Player player ? player.getWorld() : Bukkit.getWorlds().getFirst();
        Location origin = sender instanceof Player player
            ? player.getLocation().toBlockLocation().add(8, 0, 0)
            : new Location(world, 320, Math.max(world.getSpawnLocation().getBlockY() + 12, 80), 320);
        int baseX = origin.getBlockX();
        int baseY = Math.max(world.getMinHeight() + 5, origin.getBlockY());
        int baseZ = origin.getBlockZ();

        List<Check> checks = new ArrayList<>();
        try {
            prepareArea(world, baseX, baseY, baseZ);
            checks.add(basicTransfer(world, baseX, baseY, baseZ));
            checks.add(destinationFull(world, baseX, baseY, baseZ + 5));
            checks.add(filterSplit(world, baseX, baseY, baseZ + 11));
            checks.add(extractorInternalInventory(world, baseX + 10, baseY, baseZ));
            checks.add(noSource(world, baseX + 10, baseY, baseZ + 5));
            checks.add(noDestination(world, baseX + 10, baseY, baseZ + 10));
            checks.add(filteredDestination(world, baseX + 10, baseY, baseZ + 15));
            checks.add(persistenceRoundTrip(world, baseX + 18, baseY, baseZ));
            checks.add(stalePhysicalBlockCleanup(world, baseX + 18, baseY, baseZ + 5));
            checks.add(exactMetaFilter(world, baseX + 18, baseY, baseZ + 10));
            checks.add(twoExtractorsSameNetwork(world, baseX + 18, baseY, baseZ + 15));
            checks.add(pendingRecoveryToExtractorInventory(world, baseX + 24, baseY, baseZ));
            checks.add(partialPendingRecoveryKeepsOnlyLeftover(world, baseX + 24, baseY, baseZ + 5));
            checks.add(brokenPipe(world, baseX, baseY, baseZ + 18));
            checks.add(pipeTierSpeed(world, baseX + 10, baseY, baseZ + 18));
            checks.add(configuredSide(world, baseX, baseY, baseZ + 23));
            checks.add(overflowWhenPrimaryFull(world, baseX + 10, baseY, baseZ + 23));
            checks.add(trashWhenPrimaryFull(world, baseX + 20, baseY, baseZ + 23));
            checks.add(disabledTrashDoesNotDelete(world, baseX + 28, baseY, baseZ + 23));
            checks.add(roundRobinRoutes(world, baseX + 28, baseY, baseZ + 2));
            checks.add(redstoneBlocksExtractor(world, baseX + 28, baseY, baseZ + 10));
            checks.add(recipesRegistered());
        } finally {
            if (!keepArea) {
                cleanupArea(world, baseX, baseY, baseZ);
            }
            logisticsModule.rebuildNetworks();
        }

        Result result = new Result(checks);
        sender.sendMessage(Component.text("Mechanisms selftest: " + (result.passed() ? "PASS" : "FAIL"), result.passed() ? NamedTextColor.GREEN : NamedTextColor.RED));
        for (Check check : checks) {
            sender.sendMessage(Component.text((check.passed() ? "PASS " : "FAIL ") + check.name() + " - " + check.detail(), check.passed() ? NamedTextColor.GREEN : NamedTextColor.RED));
        }
        return result;
    }

    private Check basicTransfer(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 24));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        Inventory destination = chest(world, x + 4, y, z);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(8);

        int sourceIron = count(source, Material.IRON_INGOT);
        int destinationIron = count(destination, Material.IRON_INGOT);
        boolean passed = sourceIron == 0 && destinationIron == 24;
        return new Check("basic chest->extractor->pipe->inserter->chest", passed, "source=" + sourceIron + ", dest=" + destinationIron);
    }

    private Check destinationFull(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 8));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        Inventory destination = chest(world, x + 4, y, z);
        fill(destination, Material.COBBLESTONE);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(4);

        int sourceIron = count(source, Material.IRON_INGOT);
        int destinationIron = count(destination, Material.IRON_INGOT);
        boolean passed = sourceIron == 8 && destinationIron == 0;
        return new Check("destination full keeps item in source", passed, "source=" + sourceIron + ", dest=" + destinationIron);
    }

    private Check extractorInternalInventory(World world, int x, int y, int z) {
        MechanismBlockData extractor = mechanism(world, x, y, z, MechanismBlockType.EXTRACTOR);
        Inventory extractorInventory = inventoryAt(world, extractor.x(), extractor.y(), extractor.z());
        extractorInventory.setItem(0, new ItemStack(Material.DIAMOND, 12));
        mechanism(world, x + 1, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 2, y, z, MechanismBlockType.INSERTER);
        Inventory destination = chest(world, x + 3, y, z);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(4);

        int extractorDiamonds = count(extractorInventory, Material.DIAMOND);
        int destinationDiamonds = count(destination, Material.DIAMOND);
        boolean passed = extractorDiamonds == 0 && destinationDiamonds == 12;
        return new Check("extractor internal hopper inventory drains", passed, "extractor=" + extractorDiamonds + ", dest=" + destinationDiamonds);
    }

    private Check noSource(World world, int x, int y, int z) {
        MechanismBlockData extractor = mechanism(world, x, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 1, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 2, y, z, MechanismBlockType.INSERTER);
        chest(world, x + 3, y, z);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(2);

        String code = statusCode(extractor.key());
        boolean passed = "no_source".equals(code);
        return new Check("no source reports no_source", passed, "status=" + code);
    }

    private Check noDestination(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 8));
        MechanismBlockData extractor = mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(2);

        int sourceIron = count(source, Material.IRON_INGOT);
        String code = statusCode(extractor.key());
        boolean passed = sourceIron == 8 && "no_destination".equals(code);
        return new Check("no destination leaves item in source", passed, "source=" + sourceIron + ", status=" + code);
    }

    private Check filteredDestination(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.GOLD_INGOT, 8));
        MechanismBlockData extractor = mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        MechanismBlockData inserter = mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        registry.updateFilter(inserter.key(), filter(Material.IRON_INGOT));
        Inventory destination = chest(world, x + 4, y, z);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(3);

        int sourceGold = count(source, Material.GOLD_INGOT);
        int destinationGold = count(destination, Material.GOLD_INGOT);
        String code = statusCode(extractor.key());
        boolean passed = sourceGold == 8 && destinationGold == 0 && "filtered".equals(code);
        return new Check("filtered destination leaves item in source", passed, "source=" + sourceGold + ", dest=" + destinationGold + ", status=" + code);
    }

    private Check persistenceRoundTrip(World world, int x, int y, int z) {
        MechanismBlockData router = mechanism(world, x, y, z, MechanismBlockType.ROUTER);
        registry.updateFilter(router.key(), filter(Material.DIAMOND).withPriority(25));

        registry.save();
        registry.load();
        logisticsModule.rebuildNetworks();

        Optional<MechanismBlockData> reloaded = registry.getRegistered(router.key());
        boolean sameType = reloaded.map(data -> data.type() == MechanismBlockType.ROUTER).orElse(false);
        boolean filterPreserved = reloaded
            .map(MechanismBlockData::filter)
            .filter(filter -> filter.priority() == 25)
            .filter(filter -> filter.items().size() == 1)
            .filter(filter -> filter.items().getFirst().getType() == Material.DIAMOND)
            .isPresent();
        boolean passed = sameType && filterPreserved;
        return new Check("registry save/load preserves mechanism and filter", passed, "present=" + reloaded.isPresent() + ", filter=" + filterPreserved);
    }

    private Check stalePhysicalBlockCleanup(World world, int x, int y, int z) {
        MechanismBlockData pipe = mechanism(world, x, y, z, MechanismBlockType.PIPE);
        BlockKey key = pipe.key();
        world.getBlockAt(x, y, z).setType(Material.AIR, false);

        logisticsModule.rebuildNetworks();

        boolean present = registry.getRegistered(key).isPresent();
        boolean passed = !present;
        return new Check("stale physical block cleanup removes registry entry", passed, "present=" + present);
    }

    private Check exactMetaFilter(World world, int x, int y, int z) {
        ItemStack accepted = named(Material.IRON_INGOT, 8, "accepted-meta");
        ItemStack rejected = named(Material.IRON_INGOT, 8, "rejected-meta");
        Inventory source = chest(world, x, y, z);
        source.setItem(0, accepted);
        source.setItem(1, rejected);
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        MechanismBlockData inserter = mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        registry.updateFilter(inserter.key(), new FilterSettings(FilterMode.WHITELIST, MatchMode.EXACT_META, RouteMode.PRIORITY_FIRST, 0, List.of(named(Material.IRON_INGOT, 1, "accepted-meta"))));
        Inventory destination = chest(world, x + 4, y, z);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(6);

        int sourceAccepted = countSimilar(source, named(Material.IRON_INGOT, 1, "accepted-meta"));
        int sourceRejected = countSimilar(source, named(Material.IRON_INGOT, 1, "rejected-meta"));
        int destinationAccepted = countSimilar(destination, named(Material.IRON_INGOT, 1, "accepted-meta"));
        int destinationRejected = countSimilar(destination, named(Material.IRON_INGOT, 1, "rejected-meta"));
        boolean passed = sourceAccepted == 0 && sourceRejected == 8 && destinationAccepted == 8 && destinationRejected == 0;
        return new Check("exact meta filter moves only matching custom item", passed, "sourceAccepted=" + sourceAccepted + ", sourceRejected=" + sourceRejected + ", destAccepted=" + destinationAccepted + ", destRejected=" + destinationRejected);
    }

    private Check twoExtractorsSameNetwork(World world, int x, int y, int z) {
        Inventory sourceA = chest(world, x, y, z);
        sourceA.setItem(0, new ItemStack(Material.IRON_INGOT, 8));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        Inventory destination = chest(world, x + 4, y, z);

        Inventory sourceB = chest(world, x + 2, y, z + 2);
        sourceB.setItem(0, new ItemStack(Material.GOLD_INGOT, 8));
        mechanism(world, x + 2, y, z + 1, MechanismBlockType.EXTRACTOR);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(8);

        int sourceIron = count(sourceA, Material.IRON_INGOT);
        int sourceGold = count(sourceB, Material.GOLD_INGOT);
        int destIron = count(destination, Material.IRON_INGOT);
        int destGold = count(destination, Material.GOLD_INGOT);
        boolean passed = sourceIron == 0 && sourceGold == 0 && destIron == 8 && destGold == 8;
        return new Check("two extractors same network drain without loop", passed, "sourceIron=" + sourceIron + ", sourceGold=" + sourceGold + ", destIron=" + destIron + ", destGold=" + destGold);
    }

    private Check pendingRecoveryToExtractorInventory(World world, int x, int y, int z) {
        MechanismBlockData extractor = mechanism(world, x, y, z, MechanismBlockType.EXTRACTOR);
        Inventory extractorInventory = inventoryAt(world, x, y, z);
        int pendingBefore = pendingStore.count();
        pendingStore.add(extractor.key(), new ItemStack(Material.EMERALD, 7), "selftest");
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(1);

        int emeralds = count(extractorInventory, Material.EMERALD);
        int pendingAfter = pendingStore.count();
        boolean passed = emeralds == 7 && pendingAfter == pendingBefore;
        return new Check("pending recovery restores into extractor inventory", passed, "extractor=" + emeralds + ", pendingBefore=" + pendingBefore + ", pendingAfter=" + pendingAfter);
    }

    private Check partialPendingRecoveryKeepsOnlyLeftover(World world, int x, int y, int z) {
        MechanismBlockData extractor = mechanism(world, x, y, z, MechanismBlockType.EXTRACTOR);
        Inventory extractorInventory = inventoryAt(world, x, y, z);
        extractorInventory.setItem(0, new ItemStack(Material.EMERALD, 60));
        for (int slot = 1; slot < extractorInventory.getSize(); slot++) {
            extractorInventory.setItem(slot, new ItemStack(Material.COBBLESTONE, 64));
        }
        int pendingBefore = pendingStore.count();
        pendingStore.add(extractor.key(), new ItemStack(Material.EMERALD, 8), "selftest-partial");
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(1);

        int emeralds = count(extractorInventory, Material.EMERALD);
        Optional<com.denis.mechanisms.logistics.PendingTransfer> remaining = pendingStore.all().stream()
            .filter(transfer -> transfer.preferredSource().equals(extractor.key()))
            .filter(transfer -> "selftest-partial".equals(transfer.reason()))
            .findFirst();
        int remainingAmount = remaining.map(transfer -> transfer.item().getAmount()).orElse(0);
        remaining.ifPresent(transfer -> pendingStore.remove(transfer.id()));
        int pendingAfterCleanup = pendingStore.count();
        boolean passed = emeralds == 64 && remainingAmount == 4 && pendingAfterCleanup == pendingBefore;
        return new Check("partial pending recovery keeps only leftover", passed, "extractor=" + emeralds + ", remaining=" + remainingAmount + ", pendingBefore=" + pendingBefore + ", pendingAfterCleanup=" + pendingAfterCleanup);
    }

    private Check filterSplit(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 16));
        source.setItem(1, new ItemStack(Material.GOLD_INGOT, 16));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.ROUTER);
        mechanism(world, x + 3, y, z - 1, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z + 1, MechanismBlockType.PIPE);
        MechanismBlockData ironInserter = mechanism(world, x + 4, y, z - 1, MechanismBlockType.INSERTER);
        MechanismBlockData goldInserter = mechanism(world, x + 4, y, z + 1, MechanismBlockType.INSERTER);
        registry.updateFilter(ironInserter.key(), filter(Material.IRON_INGOT));
        registry.updateFilter(goldInserter.key(), filter(Material.GOLD_INGOT));
        Inventory ironDestination = chest(world, x + 5, y, z - 1);
        Inventory goldDestination = chest(world, x + 5, y, z + 1);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(10);

        int sourceIron = count(source, Material.IRON_INGOT);
        int sourceGold = count(source, Material.GOLD_INGOT);
        int destIron = count(ironDestination, Material.IRON_INGOT);
        int destGold = count(goldDestination, Material.GOLD_INGOT);
        boolean passed = sourceIron == 0 && sourceGold == 0 && destIron == 16 && destGold == 16;
        return new Check("filtered iron/gold split", passed, "sourceIron=" + sourceIron + ", sourceGold=" + sourceGold + ", ironDest=" + destIron + ", goldDest=" + destGold);
    }

    private Check brokenPipe(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 8));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        Block pipe = world.getBlockAt(x + 2, y, z);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        Inventory destination = chest(world, x + 4, y, z);
        registry.remove(pipe);
        pipe.setType(Material.AIR, false);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(4);

        int sourceIron = count(source, Material.IRON_INGOT);
        int destIron = count(destination, Material.IRON_INGOT);
        boolean passed = sourceIron == 8 && destIron == 0;
        return new Check("broken pipe blocks route", passed, "source=" + sourceIron + ", dest=" + destIron);
    }

    private Check pipeTierSpeed(World world, int x, int y, int z) {
        Inventory baseSource = chest(world, x, y, z);
        baseSource.setItem(0, new ItemStack(Material.IRON_INGOT, 32));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        Inventory baseDestination = chest(world, x + 4, y, z);

        Inventory expressSource = chest(world, x, y, z + 2);
        expressSource.setItem(0, new ItemStack(Material.GOLD_INGOT, 32));
        mechanism(world, x + 1, y, z + 2, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z + 2, MechanismBlockType.PIPE_EXPRESS);
        mechanism(world, x + 3, y, z + 2, MechanismBlockType.INSERTER);
        Inventory expressDestination = chest(world, x + 4, y, z + 2);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(1);

        int baseMoved = count(baseDestination, Material.IRON_INGOT);
        int expressMoved = count(expressDestination, Material.GOLD_INGOT);
        boolean passed = baseMoved == config.itemsPerTransferFor(MechanismBlockType.PIPE)
            && expressMoved == config.itemsPerTransferFor(MechanismBlockType.PIPE_EXPRESS);
        return new Check("pipe tiers change transfer amount", passed, "base=" + baseMoved + ", express=" + expressMoved);
    }

    private Check configuredSide(World world, int x, int y, int z) {
        Inventory northSource = chest(world, x, y, z - 1);
        northSource.setItem(0, new ItemStack(Material.IRON_INGOT, 8));
        Inventory southSource = chest(world, x, y, z + 1);
        southSource.setItem(0, new ItemStack(Material.GOLD_INGOT, 8));
        MechanismBlockData extractor = mechanism(world, x, y, z, MechanismBlockType.EXTRACTOR);
        registry.updateIoSide(extractor.key(), MechanismIoSide.NORTH);
        mechanism(world, x + 1, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 2, y, z, MechanismBlockType.INSERTER);
        Inventory destination = chest(world, x + 3, y, z);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(2);

        int destIron = count(destination, Material.IRON_INGOT);
        int destGold = count(destination, Material.GOLD_INGOT);
        int southGold = count(southSource, Material.GOLD_INGOT);
        boolean passed = destIron == 8 && destGold == 0 && southGold == 8;
        return new Check("configured extractor side chooses one container", passed, "destIron=" + destIron + ", destGold=" + destGold + ", southGold=" + southGold);
    }

    private Check overflowWhenPrimaryFull(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 8));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        Inventory primary = chest(world, x + 4, y, z);
        fill(primary, Material.COBBLESTONE);
        mechanism(world, x + 2, y, z + 1, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z + 1, MechanismBlockType.OVERFLOW);
        Inventory overflow = chest(world, x + 4, y, z + 1);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(2);

        int sourceIron = count(source, Material.IRON_INGOT);
        int primaryIron = count(primary, Material.IRON_INGOT);
        int overflowIron = count(overflow, Material.IRON_INGOT);
        boolean passed = sourceIron == 0 && primaryIron == 0 && overflowIron == 8;
        return new Check("overflow receives when primary destination is full", passed, "source=" + sourceIron + ", primary=" + primaryIron + ", overflow=" + overflowIron);
    }

    private Check trashWhenPrimaryFull(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 8));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        Inventory primary = chest(world, x + 4, y, z);
        fill(primary, Material.COBBLESTONE);
        mechanism(world, x + 2, y, z + 1, MechanismBlockType.PIPE);
        MechanismBlockData trash = mechanism(world, x + 3, y, z + 1, MechanismBlockType.TRASH);
        registry.updateTrashMode(trash.key(), TrashMode.ACCEPT_ALL);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(2);

        int sourceIron = count(source, Material.IRON_INGOT);
        int primaryIron = count(primary, Material.IRON_INGOT);
        boolean passed = sourceIron == 0 && primaryIron == 0;
        return new Check("trash deletes only after primary destination is full", passed, "source=" + sourceIron + ", primary=" + primaryIron);
    }

    private Check disabledTrashDoesNotDelete(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 8));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.TRASH);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(2);

        int sourceIron = count(source, Material.IRON_INGOT);
        boolean passed = sourceIron == 8;
        return new Check("disabled trash does not delete items", passed, "source=" + sourceIron);
    }

    private Check roundRobinRoutes(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 16));
        mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        MechanismBlockData router = mechanism(world, x + 3, y, z, MechanismBlockType.ROUTER);
        registry.updateFilter(router.key(), config.defaultFilterSettings().withRouteMode(RouteMode.ROUND_ROBIN));
        mechanism(world, x + 3, y, z - 1, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z + 1, MechanismBlockType.PIPE);
        mechanism(world, x + 4, y, z - 1, MechanismBlockType.INSERTER);
        mechanism(world, x + 4, y, z + 1, MechanismBlockType.INSERTER);
        Inventory destinationA = chest(world, x + 5, y, z - 1);
        Inventory destinationB = chest(world, x + 5, y, z + 1);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(2);

        int a = count(destinationA, Material.IRON_INGOT);
        int b = count(destinationB, Material.IRON_INGOT);
        boolean passed = a == 8 && b == 8;
        return new Check("round-robin route mode alternates destinations", passed, "a=" + a + ", b=" + b);
    }

    private Check redstoneBlocksExtractor(World world, int x, int y, int z) {
        Inventory source = chest(world, x, y, z);
        source.setItem(0, new ItemStack(Material.IRON_INGOT, 8));
        MechanismBlockData extractor = mechanism(world, x + 1, y, z, MechanismBlockType.EXTRACTOR);
        registry.updateRedstoneMode(extractor.key(), RedstoneMode.REQUIRES_POWER);
        mechanism(world, x + 2, y, z, MechanismBlockType.PIPE);
        mechanism(world, x + 3, y, z, MechanismBlockType.INSERTER);
        Inventory destination = chest(world, x + 4, y, z);
        logisticsModule.rebuildNetworks();

        logisticsModule.runCycles(2);

        int sourceIron = count(source, Material.IRON_INGOT);
        int destinationIron = count(destination, Material.IRON_INGOT);
        String code = statusCode(extractor.key());
        boolean passed = sourceIron == 8 && destinationIron == 0 && "redstone_blocked".equals(code);
        return new Check("redstone mode can block extractor", passed, "source=" + sourceIron + ", dest=" + destinationIron + ", status=" + code);
    }

    private Check recipesRegistered() {
        boolean registered = recipeService.allRegistered();
        return new Check("crafting recipes registered", registered, "recipes=" + String.join(",", recipeService.recipeNames()));
    }

    private MechanismBlockData mechanism(World world, int x, int y, int z, MechanismBlockType type) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(config.materialFor(type), false);
        clearInventory(block);
        return registry.add(block, type);
    }

    private Inventory chest(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        block.setType(Material.CHEST, false);
        return inventoryAt(world, x, y, z);
    }

    private Inventory inventoryAt(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        BlockState state = block.getState();
        if (!(state instanceof InventoryHolder holder)) {
            throw new IllegalStateException("Expected inventory at " + x + "," + y + "," + z);
        }
        holder.getInventory().clear();
        return holder.getInventory();
    }

    private FilterSettings filter(Material material) {
        return new FilterSettings(FilterMode.WHITELIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of(new ItemStack(material, 1)));
    }

    private ItemStack named(Material material, int amount, String name) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        item.setItemMeta(meta);
        return item;
    }

    private String statusCode(BlockKey key) {
        return stats.status(key).map(LogisticsStats.StatusEntry::code).orElse("none");
    }

    private void prepareArea(World world, int baseX, int baseY, int baseZ) {
        loadAreaChunks(world, baseX, baseZ);
        cleanupArea(world, baseX, baseY, baseZ);
        for (int x = 0; x < SIZE_X; x++) {
            for (int z = 0; z < SIZE_Z; z++) {
                world.getBlockAt(baseX + x, baseY - 1, baseZ + z).setType(Material.SMOOTH_STONE, false);
            }
        }
    }

    private void loadAreaChunks(World world, int baseX, int baseZ) {
        int minChunkX = baseX >> 4;
        int maxChunkX = (baseX + SIZE_X - 1) >> 4;
        int minChunkZ = baseZ >> 4;
        int maxChunkZ = (baseZ + SIZE_Z - 1) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                world.getChunkAt(chunkX, chunkZ).load(true);
            }
        }
    }

    private void cleanupArea(World world, int baseX, int baseY, int baseZ) {
        for (MechanismBlockData data : registry.all()) {
            if (!data.worldId().equals(world.getUID())) {
                continue;
            }
            if (data.x() >= baseX && data.x() < baseX + SIZE_X
                && data.y() >= baseY - 1 && data.y() < baseY + SIZE_Y
                && data.z() >= baseZ && data.z() < baseZ + SIZE_Z) {
                data.block().ifPresent(block -> registry.remove(block));
            }
        }
        for (int x = 0; x < SIZE_X; x++) {
            for (int y = -1; y < SIZE_Y; y++) {
                for (int z = 0; z < SIZE_Z; z++) {
                    Block block = world.getBlockAt(baseX + x, baseY + y, baseZ + z);
                    clearInventory(block);
                    block.setType(Material.AIR, false);
                }
            }
        }
    }

    private void fill(Inventory inventory, Material material) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, new ItemStack(material, 64));
        }
    }

    private int count(Inventory inventory, Material material) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() == material) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private int countSimilar(Inventory inventory, ItemStack expected) {
        int total = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.isSimilar(expected)) {
                total += item.getAmount();
            }
        }
        return total;
    }

    private void clearInventory(Block block) {
        BlockState state = block.getState();
        if (state instanceof InventoryHolder holder) {
            holder.getInventory().clear();
        }
    }

    public record Check(String name, boolean passed, String detail) {
    }

    public record Result(List<Check> checks) {
        public boolean passed() {
            return checks.stream().allMatch(Check::passed);
        }
    }
}
