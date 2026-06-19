package com.denis.mechanisms.ui;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.TrashMode;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.logistics.FilterService;
import com.denis.mechanisms.logistics.LogisticsStats;
import com.denis.mechanisms.logistics.MechanismNetwork;
import com.denis.mechanisms.logistics.NetworkIndexer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public final class StatusRenderer {
    private static final BlockFace[] FACES = {
        BlockFace.NORTH,
        BlockFace.SOUTH,
        BlockFace.EAST,
        BlockFace.WEST,
        BlockFace.UP,
        BlockFace.DOWN
    };

    private final MechanismBlockRegistry registry;
    private final MechanismsConfig config;
    private final NetworkIndexer indexer;
    private final FilterService filterService;
    private final LogisticsStats stats;

    public StatusRenderer(
        MechanismBlockRegistry registry,
        MechanismsConfig config,
        NetworkIndexer indexer,
        FilterService filterService,
        LogisticsStats stats
    ) {
        this.registry = registry;
        this.config = config;
        this.indexer = indexer;
        this.filterService = filterService;
        this.stats = stats;
    }

    public void sendStatus(Player player, MechanismBlockData data) {
        Optional<MechanismNetwork> network = indexer.networkFor(data.key());
        player.sendMessage(Component.text("Mechanisms: " + data.type().russianName(), data.type().color()));
        player.sendMessage(Component.text("Позиция: " + data.worldName() + " " + data.key().shortString(), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Сеть: " + network.map(MechanismNetwork::id).orElse("нет"), NamedTextColor.GRAY));
        player.sendMessage(Component.text("Узлов в сети: " + network.map(MechanismNetwork::size).orElse(0), NamedTextColor.GRAY));
        int unloadedInWorld = unloadedMechanismsInWorld(data);
        if (unloadedInWorld > 0 && !config.allowUnloadedChunks()) {
            player.sendMessage(Component.text("Chunk warning: " + unloadedInWorld + " механизмов в этом мире сейчас в unloaded chunks", NamedTextColor.YELLOW));
        }
        if (network.map(MechanismNetwork::tooLarge).orElse(false)) {
            player.sendMessage(Component.text("Ошибка: сеть слишком большая", NamedTextColor.RED));
        }
        if (data.type().isPipe()) {
            player.sendMessage(Component.text("Скорость трубы: " + config.itemsPerTransferFor(data) + " предметов за перенос", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Канал трубы: " + data.pipeChannel().russianName(), NamedTextColor.GRAY));
        }
        if (data.type().supportsIoSide()) {
            player.sendMessage(Component.text("Сторона контейнера: " + data.ioSide().russianName(), NamedTextColor.GRAY));
        }
        if (data.type() == MechanismBlockType.EXTRACTOR) {
            player.sendMessage(Component.text("Redstone: " + data.redstoneMode().russianName(), NamedTextColor.GRAY));
        }
        if (data.type() == MechanismBlockType.TRASH) {
            player.sendMessage(Component.text("Trash safety: " + data.trashMode().russianName(), NamedTextColor.RED));
            if (data.trashMode() == TrashMode.FILTERED_ONLY && data.filter().itemCount() == 0) {
                player.sendMessage(Component.text("Trash не активен: нужен непустой фильтр или accept all.", NamedTextColor.RED));
            }
        }
        if (data.type() == MechanismBlockType.EXTRACTOR) {
            player.sendMessage(Component.text("Источник: " + adjacentContainer(data).orElse("нет"), NamedTextColor.GRAY));
        }
        if (data.type().isDestination() && data.type() != MechanismBlockType.TRASH) {
            player.sendMessage(Component.text("Назначение: " + adjacentContainer(data).orElse("нет"), NamedTextColor.GRAY));
        }
        if (data.type().supportsFilter()) {
            player.sendMessage(Component.text("Фильтр: " + filterService.summary(data.filter()), NamedTextColor.GRAY));
            player.sendMessage(Component.text("Маршрут: " + data.filter().routeMode().russianName(), NamedTextColor.GRAY));
        }
        if (!data.upgrades().equals(com.denis.mechanisms.block.UpgradeModules.EMPTY)) {
            player.sendMessage(Component.text(
                "Модули: speed=" + data.upgrades().speedLevel()
                    + ", stack=" + data.upgrades().stackLevel()
                    + ", filter=" + data.upgrades().filterLevel()
                    + ", priority=" + data.upgrades().priorityLevel()
                    + ", range=" + data.upgrades().rangeLevel()
                    + ", silent=" + data.upgrades().silentLevel(),
                NamedTextColor.DARK_AQUA
            ));
        }
        stats.status(data.key()).ifPresentOrElse(
            status -> player.sendMessage(Component.text("Последнее: [" + status.code() + "] " + status.detail() + " (" + age(status.atMillis()) + " назад)", status.code().equals("ok") ? NamedTextColor.GREEN : NamedTextColor.RED)),
            () -> player.sendMessage(Component.text("Последнее: попыток переноса еще не было", NamedTextColor.DARK_GRAY))
        );
        stats.counters(data.key()).ifPresent(counter ->
            player.sendMessage(Component.text("Счетчики: transfers=" + counter.transfers() + ", items=" + counter.items() + ", errors=" + counter.errors(), NamedTextColor.GRAY))
        );
        player.sendMessage(Component.text("Debug: " + (config.debug() ? "вкл" : "выкл"), NamedTextColor.DARK_GRAY));
    }

    private Optional<String> adjacentContainer(MechanismBlockData data) {
        Optional<Block> maybeBlock = data.block();
        if (maybeBlock.isEmpty()) {
            return Optional.empty();
        }
        Block block = maybeBlock.get();
        World world = block.getWorld();
        BlockFace[] faces = data.ioSide().face() == null ? FACES : new BlockFace[] { data.ioSide().face() };
        for (BlockFace face : faces) {
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
            if (state instanceof InventoryHolder) {
                return Optional.of(face.name().toLowerCase() + " " + neighbor.getType().name().toLowerCase());
            }
        }
        return Optional.empty();
    }

    private int unloadedMechanismsInWorld(MechanismBlockData origin) {
        return (int) registry.all().stream()
            .filter(data -> data.worldId().equals(origin.worldId()))
            .filter(data -> data.world().isPresent())
            .filter(data -> !data.world().get().isChunkLoaded(data.x() >> 4, data.z() >> 4))
            .count();
    }

    private String age(long atMillis) {
        Duration duration = Duration.between(Instant.ofEpochMilli(atMillis), Instant.now());
        if (duration.toSeconds() < 60) {
            return duration.toSeconds() + "s";
        }
        return duration.toMinutes() + "m";
    }
}
