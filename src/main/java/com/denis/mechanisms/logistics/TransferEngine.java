package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.RedstoneMode;
import com.denis.mechanisms.block.TrashMode;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.visual.VisualEffectService;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TransferEngine {
    private static final BlockFace[] FACES = {
        BlockFace.NORTH,
        BlockFace.SOUTH,
        BlockFace.EAST,
        BlockFace.WEST,
        BlockFace.UP,
        BlockFace.DOWN
    };

    private final JavaPlugin plugin;
    private final MechanismBlockRegistry registry;
    private final MechanismsConfig config;
    private final NetworkIndexer indexer;
    private final RouteFinder routeFinder;
    private final PendingTransferStore pendingStore;
    private final LogisticsStats stats;
    private final TransferTransaction transaction = new TransferTransaction();
    private final VisualEffectService visualEffectService;
    private final Map<String, Integer> routeCursors = new HashMap<>();
    private int extractorCursor;

    public TransferEngine(
        JavaPlugin plugin,
        MechanismBlockRegistry registry,
        MechanismsConfig config,
        NetworkIndexer indexer,
        RouteFinder routeFinder,
        PendingTransferStore pendingStore,
        LogisticsStats stats,
        VisualEffectService visualEffectService
    ) {
        this.plugin = plugin;
        this.registry = registry;
        this.config = config;
        this.indexer = indexer;
        this.routeFinder = routeFinder;
        this.pendingStore = pendingStore;
        this.stats = stats;
        this.visualEffectService = visualEffectService;
    }

    public void runCycle() {
        long started = System.nanoTime();
        try {
            if (registry.cleanupInvalid() > 0) {
                indexer.rebuild();
                routeFinder.clearCache();
            }
            processPending(Math.max(1, config.maxTransfersPerTick() / 4));

            List<MechanismBlockData> extractors = registry.byType(MechanismBlockType.EXTRACTOR);
            if (extractors.isEmpty()) {
                return;
            }

            int attempts = Math.min(config.maxTransfersPerTick(), extractors.size());
            for (int i = 0; i < attempts; i++) {
                MechanismBlockData extractor = extractors.get(extractorCursor % extractors.size());
                extractorCursor = (extractorCursor + 1) % extractors.size();
                attemptExtractor(extractor);
            }
        } finally {
            stats.recordCycle(System.nanoTime() - started);
        }
    }

    public boolean attemptExtractor(MechanismBlockData extractor) {
        if (!redstoneAllows(extractor)) {
            stats.recordError(extractor.key(), "redstone_blocked", readableReason("redstone_blocked"));
            return false;
        }

        Optional<Inventory> sourceInventory = extractorInventoryWithItems(extractor).or(() -> adjacentInventory(extractor, true));
        if (sourceInventory.isEmpty()) {
            stats.recordError(extractor.key(), "no_source", readableReason("no_source"));
            return false;
        }

        BukkitInventoryAccess source = new BukkitInventoryAccess(sourceInventory.get());
        List<InventoryUtil.SelectedStack> candidates = InventoryUtil.selectMovableStacks(source, config.maxItemsPerTransfer());
        if (candidates.isEmpty()) {
            stats.recordError(extractor.key(), "source_empty", readableReason("source_empty"));
            return false;
        }

        String lastReason = "no_destination";
        for (InventoryUtil.SelectedStack selected : candidates) {
            ItemStack moving = selected.stack();
            List<Route> routes = routeFinder.findRoutes(extractor, moving);
            if (routes.isEmpty()) {
                lastReason = routeFinder.missReason(extractor);
                continue;
            }

            RoutePhaseResult normal = tryRoutePhase(extractor, source, selected.slot(), moving, routes, MechanismBlockType.INSERTER);
            if (normal.success()) {
                return true;
            }
            if (normal.terminalFailure()) {
                return false;
            }

            RoutePhaseResult overflow = tryRoutePhase(extractor, source, selected.slot(), moving, routes, MechanismBlockType.OVERFLOW);
            if (overflow.success()) {
                return true;
            }
            if (overflow.terminalFailure()) {
                return false;
            }

            RoutePhaseResult trash = tryRoutePhase(extractor, source, selected.slot(), moving, routes, MechanismBlockType.TRASH);
            if (trash.success()) {
                return true;
            }
            if (trash.terminalFailure()) {
                return false;
            }

            lastReason = combineReasons(normal, overflow, trash);
        }

        stats.recordError(extractor.key(), lastReason, readableReason(lastReason));
        return false;
    }

    private RoutePhaseResult tryRoutePhase(
        MechanismBlockData extractor,
        BukkitInventoryAccess source,
        int sourceSlot,
        ItemStack moving,
        List<Route> allRoutes,
        MechanismBlockType destinationType
    ) {
        List<Route> routes = allRoutes.stream()
            .filter(route -> route.destination().type() == destinationType)
            .toList();
        if (routes.isEmpty()) {
            return RoutePhaseResult.miss("no_destination", false, false);
        }

        boolean sawDestination = false;
        boolean sawFull = false;
        for (Route route : orderRoutes(extractor, moving, destinationType, routes)) {
            ItemStack routeMoving = withMaxAmount(moving, routeTransferAmount(route));
            if (destinationType == MechanismBlockType.TRASH) {
                if (!trashAllows(route.destination())) {
                    continue;
                }
                sawDestination = true;
                TransferTransaction.Result result = transaction.delete(source, sourceSlot, routeMoving);
                if (result.success()) {
                    stats.recordSuccess(extractor.key(), routeMoving, result.moved(), "trash:" + route.destination().key().shortString());
                    visualEffectService.showTransferEcho(route, routeMoving);
                    return RoutePhaseResult.ok();
                }
                stats.recordError(extractor.key(), result.code(), readableReason(result.code()));
                return RoutePhaseResult.terminal(result.code());
            }

            Optional<Inventory> destinationInventory = adjacentInventory(route.destination(), false);
            if (destinationInventory.isEmpty()) {
                continue;
            }
            sawDestination = true;
            BukkitInventoryAccess destination = new BukkitInventoryAccess(destinationInventory.get());
            if (!InventoryUtil.canFullyAccept(destination, routeMoving)) {
                sawFull = true;
                continue;
            }

            TransferTransaction.Result result = transaction.execute(source, sourceSlot, destination, routeMoving);
            if (result.success()) {
                stats.recordSuccess(extractor.key(), routeMoving, result.moved(), route.destination().key().shortString());
                visualEffectService.showTransferEcho(route, routeMoving);
                return RoutePhaseResult.ok();
            }

            if (result.recoveryStack() != null) {
                pendingStore.add(extractor.key(), result.recoveryStack(), result.code());
            }
            stats.recordError(extractor.key(), result.code(), readableReason(result.code()));
            return RoutePhaseResult.terminal(result.code());
        }

        return RoutePhaseResult.miss(sawDestination && sawFull ? "destination_full" : "no_destination", sawDestination, sawFull);
    }

    private List<Route> orderRoutes(MechanismBlockData extractor, ItemStack moving, MechanismBlockType destinationType, List<Route> routes) {
        if (routes.size() <= 1) {
            return routes;
        }
        RouteMode mode = effectiveRouteMode(routes.getFirst());
        List<Route> ordered = new ArrayList<>(routes);
        if (mode == RouteMode.NEAREST) {
            ordered.sort(Comparator
                .comparingInt(Route::length)
                .thenComparing(Comparator.comparingInt(this::effectivePriority).reversed())
                .thenComparing(route -> route.destination().key()));
            return List.copyOf(ordered);
        }

        ordered.sort(Comparator
            .comparingInt(this::effectivePriority).reversed()
            .thenComparingInt(Route::length)
            .thenComparing(route -> route.destination().key()));
        if (mode == RouteMode.ROUND_ROBIN || mode == RouteMode.SPLIT_EVENLY) {
            String key = extractor.key() + "|" + destinationType.token() + "|" + mode.token() + "|" + moving.getType().key();
            int cursor = routeCursors.merge(key, 1, Integer::sum) - 1;
            int offset = Math.floorMod(cursor, ordered.size());
            if (offset > 0) {
                List<Route> rotated = new ArrayList<>(ordered.size());
                rotated.addAll(ordered.subList(offset, ordered.size()));
                rotated.addAll(ordered.subList(0, offset));
                return List.copyOf(rotated);
            }
        }
        return List.copyOf(ordered);
    }

    private int effectivePriority(Route route) {
        return route.priority() + config.priorityBonus(route.destination().upgrades());
    }

    private RouteMode effectiveRouteMode(Route route) {
        for (BlockKey key : route.path()) {
            Optional<MechanismBlockData> data = registry.getRegistered(key);
            if (data.isPresent() && data.get().type() == MechanismBlockType.ROUTER) {
                return data.get().filter().routeMode();
            }
        }
        return route.destination().filter().routeMode();
    }

    private String combineReasons(RoutePhaseResult normal, RoutePhaseResult overflow, RoutePhaseResult trash) {
        if (normal.sawFull() || overflow.sawFull()) {
            return "destination_full";
        }
        if ("filtered".equals(normal.reason()) || "filtered".equals(overflow.reason()) || "filtered".equals(trash.reason())) {
            return "filtered";
        }
        return "no_destination";
    }

    private boolean redstoneAllows(MechanismBlockData extractor) {
        if (extractor.redstoneMode() == RedstoneMode.IGNORE) {
            return true;
        }
        Optional<Block> maybeBlock = extractor.block();
        if (maybeBlock.isEmpty()) {
            return false;
        }
        boolean powered = maybeBlock.get().isBlockPowered() || maybeBlock.get().isBlockIndirectlyPowered();
        return extractor.redstoneMode() == RedstoneMode.REQUIRES_POWER ? powered : !powered;
    }

    private boolean trashAllows(MechanismBlockData trash) {
        if (trash.trashMode() == TrashMode.DISABLED) {
            return false;
        }
        if (trash.trashMode() == TrashMode.ACCEPT_ALL) {
            return true;
        }
        return trash.filter().itemCount() > 0;
    }

    private int routeTransferAmount(Route route) {
        int amount = Integer.MAX_VALUE;
        boolean hasPipe = false;
        for (BlockKey key : route.path()) {
            Optional<MechanismBlockData> data = registry.getRegistered(key);
            if (data.isPresent() && data.get().type().isPipe()) {
                hasPipe = true;
                amount = Math.min(amount, config.itemsPerTransferFor(data.get()));
            }
        }
        return hasPipe ? amount : config.itemsPerTransferFor(route.source());
    }

    private ItemStack withMaxAmount(ItemStack stack, int maxAmount) {
        ItemStack copy = stack.clone();
        copy.setAmount(Math.min(copy.getAmount(), Math.max(1, maxAmount)));
        return copy;
    }

    public void processPending(int budget) {
        int processed = 0;
        for (PendingTransfer transfer : pendingStore.all()) {
            if (processed >= budget) {
                return;
            }
            processed++;
            Optional<MechanismBlockData> sourceBlock = registry.getRegistered(transfer.preferredSource());
            if (sourceBlock.isEmpty()) {
                pendingStore.updateAttempts(transfer.id(), transfer.attempts() + 1);
                continue;
            }
            Optional<Inventory> sourceInventory = recoveryInventory(sourceBlock.get());
            if (sourceInventory.isEmpty()) {
                pendingStore.updateAttempts(transfer.id(), transfer.attempts() + 1);
                continue;
            }
            BukkitInventoryAccess source = new BukkitInventoryAccess(sourceInventory.get());
            ItemStack recovering = transfer.item().clone();
            int originalAmount = recovering.getAmount();
            int leftover = InventoryUtil.addStack(source, recovering);
            if (leftover == 0) {
                pendingStore.remove(transfer.id());
            } else if (leftover < originalAmount) {
                ItemStack remaining = transfer.item().clone();
                remaining.setAmount(leftover);
                pendingStore.updateItemAndAttempts(transfer.id(), remaining, transfer.attempts() + 1);
            } else {
                pendingStore.updateAttempts(transfer.id(), transfer.attempts() + 1);
            }
        }
    }

    private Optional<Inventory> recoveryInventory(MechanismBlockData sourceBlock) {
        return mechanismInventoryWithSpace(sourceBlock).or(() -> adjacentInventory(sourceBlock, true));
    }

    public Optional<Inventory> adjacentInventory(MechanismBlockData mechanism, boolean sourceLookup) {
        Optional<World> maybeWorld = mechanism.world();
        if (maybeWorld.isEmpty()) {
            return Optional.empty();
        }
        World world = maybeWorld.get();
        if (!config.allowUnloadedChunks() && !world.isChunkLoaded(mechanism.x() >> 4, mechanism.z() >> 4)) {
            return Optional.empty();
        }
        Block block = world.getBlockAt(mechanism.x(), mechanism.y(), mechanism.z());
        for (BlockFace face : facesFor(mechanism)) {
            int x = block.getX() + face.getModX();
            int y = block.getY() + face.getModY();
            int z = block.getZ() + face.getModZ();
            if (!config.allowUnloadedChunks() && !world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            Block neighbor = world.getBlockAt(x, y, z);
            if (registry.getRegistered(BlockKey.from(neighbor)).isPresent()) {
                continue;
            }
            BlockState state = neighbor.getState();
            if (state instanceof InventoryHolder holder) {
                if (sourceLookup && mechanism.type() == MechanismBlockType.EXTRACTOR && holder instanceof Hopper) {
                    continue;
                }
                return Optional.of(holder.getInventory());
            }
        }
        return Optional.empty();
    }

    private BlockFace[] facesFor(MechanismBlockData mechanism) {
        if (mechanism.ioSide().face() == null) {
            return FACES;
        }
        return new BlockFace[] { mechanism.ioSide().face() };
    }

    private Optional<Inventory> extractorInventoryWithItems(MechanismBlockData extractor) {
        if (extractor.type() != MechanismBlockType.EXTRACTOR) {
            return Optional.empty();
        }
        Optional<Block> maybeBlock = extractor.block();
        if (maybeBlock.isEmpty()) {
            return Optional.empty();
        }
        BlockState state = maybeBlock.get().getState();
        if (!(state instanceof InventoryHolder holder)) {
            return Optional.empty();
        }
        BukkitInventoryAccess access = new BukkitInventoryAccess(holder.getInventory());
        return InventoryUtil.selectFirstMovable(access, 1).isPresent() ? Optional.of(holder.getInventory()) : Optional.empty();
    }

    private Optional<Inventory> mechanismInventoryWithSpace(MechanismBlockData mechanism) {
        if (mechanism.type() != MechanismBlockType.EXTRACTOR) {
            return Optional.empty();
        }
        Optional<Block> maybeBlock = mechanism.block();
        if (maybeBlock.isEmpty()) {
            return Optional.empty();
        }
        BlockState state = maybeBlock.get().getState();
        if (!(state instanceof InventoryHolder holder)) {
            return Optional.empty();
        }
        return Optional.of(holder.getInventory());
    }

    private record RoutePhaseResult(boolean success, boolean terminalFailure, String reason, boolean sawDestination, boolean sawFull) {
        private static RoutePhaseResult ok() {
            return new RoutePhaseResult(true, false, "ok", true, false);
        }

        private static RoutePhaseResult terminal(String reason) {
            return new RoutePhaseResult(false, true, reason, true, false);
        }

        private static RoutePhaseResult miss(String reason, boolean sawDestination, boolean sawFull) {
            return new RoutePhaseResult(false, false, reason, sawDestination, sawFull);
        }
    }

    private String readableReason(String code) {
        return switch (code) {
            case "no_source" -> "Нет соседнего контейнера-источника";
            case "source_empty" -> "Источник пуст";
            case "no_network" -> "Экстрактор не подключен к сети";
            case "network_too_large" -> "Сеть превышает maxNetworkNodes";
            case "no_destination" -> "Нет подходящего назначения";
            case "destination_full" -> "Контейнер назначения заполнен";
            case "filtered" -> "Фильтр не нашел подходящее назначение";
            case "source_changed" -> "Источник изменился перед commit";
            case "needs_recovery_storage" -> "Остаток сохранен в pending.yml";
            case "destination_leftover_recovered" -> "Остаток возвращен в источник";
            case "redstone_blocked" -> "Экстрактор заблокирован режимом redstone";
            default -> code;
        };
    }
}
