package com.denis.mechanisms.ui;

import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.logistics.LogisticsStats;
import com.denis.mechanisms.logistics.MechanismNetwork;
import com.denis.mechanisms.logistics.NetworkIndexer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class NetworkInspectorGui {
    private static final int SIZE = 54;
    private static final int CLOSE_SLOT = 53;

    private final MechanismBlockRegistry registry;
    private final MechanismsConfig config;
    private final NetworkIndexer indexer;
    private final LogisticsStats stats;

    public NetworkInspectorGui(MechanismBlockRegistry registry, MechanismsConfig config, NetworkIndexer indexer, LogisticsStats stats) {
        this.registry = registry;
        this.config = config;
        this.indexer = indexer;
        this.stats = stats;
    }

    public void open(Player player, MechanismBlockData origin) {
        InspectorHolder holder = new InspectorHolder();
        Inventory inventory = Bukkit.createInventory(holder, SIZE, "Mechanisms: сеть");
        holder.setInventory(inventory);
        draw(inventory, origin);
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof InspectorHolder) {
            event.setCancelled(true);
            if (event.getRawSlot() == CLOSE_SLOT && event.getWhoClicked() instanceof Player player) {
                player.closeInventory();
            }
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof InspectorHolder
            && event.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot < SIZE)) {
            event.setCancelled(true);
        }
    }

    private void draw(Inventory inventory, MechanismBlockData origin) {
        Optional<MechanismNetwork> maybeNetwork = indexer.networkFor(origin.key());
        MechanismNetwork network = maybeNetwork.orElse(null);
        List<MechanismBlockData> nodes = network == null
            ? List.of(origin)
            : network.nodes().stream()
                .map(registry::getRegistered)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(MechanismBlockData::key))
                .toList();

        inventory.setItem(4, summary(origin, network, nodes));
        inventory.setItem(6, perfItem());
        inventory.setItem(8, warningItem(origin, network));

        int slot = 9;
        for (MechanismBlockData node : nodes) {
            if (slot >= CLOSE_SLOT) {
                break;
            }
            inventory.setItem(slot++, nodeItem(node));
        }
        inventory.setItem(CLOSE_SLOT, button(Material.BARRIER, "Закрыть", "Вернуться в игру", NamedTextColor.RED));
    }

    private ItemStack summary(MechanismBlockData origin, MechanismNetwork network, List<MechanismBlockData> nodes) {
        List<Component> lore = new ArrayList<>();
        lore.add(line("origin: " + origin.key().shortString()));
        lore.add(line("network: " + (network == null ? "нет" : network.id())));
        lore.add(line("nodes: " + nodes.size() + " / max " + config.maxNetworkNodes()));
        if (network != null && network.tooLarge()) {
            lore.add(Component.text("Сеть превышает лимит узлов", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        return item(Material.COMPASS, "Network inspector", NamedTextColor.GOLD, lore);
    }

    private ItemStack perfItem() {
        List<Component> lore = List.of(
            line("cycles: " + stats.cycleCount()),
            line(String.format(java.util.Locale.ROOT, "avg cycle: %.3f ms", stats.averageCycleMillis())),
            line(String.format(java.util.Locale.ROOT, "last cycle: %.3f ms", stats.lastCycleMillis())),
            line(String.format(java.util.Locale.ROOT, "transfers/sec: %.2f", stats.transfersPerSecond()))
        );
        return item(Material.CLOCK, "Performance", NamedTextColor.AQUA, lore);
    }

    private ItemStack warningItem(MechanismBlockData origin, MechanismNetwork network) {
        int unloaded = unloadedMechanismsInWorld(origin);
        List<Component> lore = new ArrayList<>();
        lore.add(line("unloaded records in world: " + unloaded));
        lore.add(line("allowUnloadedChunks: " + config.allowUnloadedChunks()));
        if (network == null) {
            lore.add(Component.text("Этот блок сейчас не входит ни в одну сеть", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }
        Material material = unloaded > 0 || network == null ? Material.YELLOW_DYE : Material.LIME_DYE;
        return item(material, "Chunk safety", unloaded > 0 ? NamedTextColor.YELLOW : NamedTextColor.GREEN, lore);
    }

    private ItemStack nodeItem(MechanismBlockData node) {
        List<Component> lore = new ArrayList<>();
        lore.add(line(node.key().shortString()));
        lore.add(line("side: " + node.ioSide().russianName()));
        if (node.type().isPipe()) {
            lore.add(line("channel: " + node.pipeChannel().russianName()));
            lore.add(line("speed: " + config.itemsPerTransferFor(node)));
        }
        if (node.type().supportsFilter()) {
            lore.add(line("route: " + node.filter().routeMode().russianName()));
            lore.add(line("filter items: " + node.filter().itemCount()));
            lore.add(line("priority: " + (node.filter().priority() + config.priorityBonus(node.upgrades()))));
        }
        if (!node.upgrades().equals(com.denis.mechanisms.block.UpgradeModules.EMPTY)) {
            lore.add(line("modules: S" + node.upgrades().speedLevel()
                + "/St" + node.upgrades().stackLevel()
                + "/F" + node.upgrades().filterLevel()
                + "/P" + node.upgrades().priorityLevel()
                + "/R" + node.upgrades().rangeLevel()
                + "/Si" + node.upgrades().silentLevel()));
        }
        stats.status(node.key()).ifPresent(status -> lore.add(line("last: " + status.code())));
        stats.counters(node.key()).ifPresent(counter -> lore.add(line("items: " + counter.items() + ", errors: " + counter.errors())));
        return item(node.material(), node.type().russianName(), node.type().color(), lore);
    }

    private ItemStack button(Material material, String name, String lore, NamedTextColor color) {
        return item(material, name, color, List.of(line(lore)));
    }

    private ItemStack item(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Component line(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    private int unloadedMechanismsInWorld(MechanismBlockData origin) {
        return (int) registry.all().stream()
            .filter(data -> data.worldId().equals(origin.worldId()))
            .filter(data -> data.world().isPresent())
            .filter(data -> {
                World world = data.world().get();
                return !world.isChunkLoaded(data.x() >> 4, data.z() >> 4);
            })
            .count();
    }

    public static final class InspectorHolder implements InventoryHolder {
        private Inventory inventory;

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
