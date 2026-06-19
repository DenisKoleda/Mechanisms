package com.denis.mechanisms.ui;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.MechanismIoSide;
import com.denis.mechanisms.block.TrashMode;
import com.denis.mechanisms.block.UpgradeModules;
import com.denis.mechanisms.logistics.RouteMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Optional;

public final class MechanismSettingsGui {
    private static final int SIZE = 45;
    private static final int SIDE_SLOT = 10;
    private static final int REDSTONE_SLOT = 11;
    private static final int CHANNEL_SLOT = 12;
    private static final int ROUTE_SLOT = 13;
    private static final int TRASH_SLOT = 14;
    private static final int FILTER_SLOT = 15;
    private static final int NETWORK_SLOT = 16;
    private static final int SPEED_SLOT = 19;
    private static final int STACK_SLOT = 20;
    private static final int FILTER_LEVEL_SLOT = 21;
    private static final int PRIORITY_LEVEL_SLOT = 22;
    private static final int RANGE_SLOT = 23;
    private static final int SILENT_SLOT = 24;
    private static final int CLOSE_SLOT = 40;

    private final MechanismBlockRegistry registry;
    private final FilterGui filterGui;
    private final NetworkInspectorGui networkInspectorGui;

    public MechanismSettingsGui(MechanismBlockRegistry registry, FilterGui filterGui, NetworkInspectorGui networkInspectorGui) {
        this.registry = registry;
        this.filterGui = filterGui;
        this.networkInspectorGui = networkInspectorGui;
    }

    public void open(Player player, MechanismBlockData data) {
        SettingsHolder holder = new SettingsHolder(data.key());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, "Mechanisms: настройки");
        holder.setInventory(inventory);
        draw(inventory, data);
        player.openInventory(inventory);
        player.sendActionBar(Component.text("Настройки: " + data.type().russianName(), NamedTextColor.YELLOW));
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SettingsHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= SIZE) {
            return;
        }
        Optional<MechanismBlockData> maybeData = registry.getRegistered(holder.key());
        if (maybeData.isEmpty()) {
            player.closeInventory();
            return;
        }
        MechanismBlockData data = maybeData.get();
        switch (rawSlot) {
            case SIDE_SLOT -> {
                if (!data.type().supportsIoSide()) {
                    return;
                }
                registry.updateIoSide(data.key(), data.ioSide().next());
                redrawAndAction(event, player, data.key(), "Сторона: ");
            }
            case CHANNEL_SLOT -> {
                if (!data.type().isPipe()) {
                    return;
                }
                registry.updatePipeChannel(data.key(), data.pipeChannel().next());
                registry.getRegistered(data.key()).ifPresent(updated -> {
                    draw(event.getView().getTopInventory(), updated);
                    player.sendActionBar(Component.text("Канал трубы: " + updated.pipeChannel().russianName(), NamedTextColor.YELLOW));
                });
            }
            case ROUTE_SLOT -> {
                if (!data.type().supportsFilter()) {
                    return;
                }
                RouteMode next = data.filter().routeMode().next();
                registry.updateFilter(data.key(), data.filter().withRouteMode(next));
                registry.getRegistered(data.key()).ifPresent(updated -> {
                    draw(event.getView().getTopInventory(), updated);
                    player.sendActionBar(Component.text("Маршрут: " + updated.filter().routeMode().russianName(), NamedTextColor.YELLOW));
                });
            }
            case REDSTONE_SLOT -> {
                if (data.type() != MechanismBlockType.EXTRACTOR) {
                    return;
                }
                registry.updateRedstoneMode(data.key(), data.redstoneMode().next());
                registry.getRegistered(data.key()).ifPresent(updated -> {
                    draw(event.getView().getTopInventory(), updated);
                    player.sendActionBar(Component.text("Redstone: " + updated.redstoneMode().russianName(), NamedTextColor.YELLOW));
                });
            }
            case TRASH_SLOT -> {
                if (data.type() != MechanismBlockType.TRASH) {
                    return;
                }
                registry.updateTrashMode(data.key(), data.trashMode().next());
                registry.getRegistered(data.key()).ifPresent(updated -> {
                    draw(event.getView().getTopInventory(), updated);
                    player.sendActionBar(Component.text("Trash: " + updated.trashMode().russianName(), NamedTextColor.YELLOW));
                });
            }
            case FILTER_SLOT -> {
                if (data.type().supportsFilter()) {
                    filterGui.open(player, data);
                }
            }
            case NETWORK_SLOT -> networkInspectorGui.open(player, data);
            case CLOSE_SLOT -> player.closeInventory();
            default -> {
            }
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SettingsHolder)) {
            return;
        }
        if (event.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot < SIZE)) {
            event.setCancelled(true);
        }
    }

    private void redrawAndAction(InventoryClickEvent event, Player player, BlockKey key, String prefix) {
        registry.getRegistered(key).ifPresent(updated -> {
            draw(event.getView().getTopInventory(), updated);
            player.sendActionBar(Component.text(prefix + updated.ioSide().russianName(), NamedTextColor.YELLOW));
        });
    }

    private void draw(Inventory inventory, MechanismBlockData data) {
        inventory.clear();
        inventory.setItem(SIDE_SLOT, sideButton(data));
        inventory.setItem(REDSTONE_SLOT, redstoneButton(data));
        inventory.setItem(CHANNEL_SLOT, channelButton(data));
        inventory.setItem(ROUTE_SLOT, routeButton(data));
        inventory.setItem(TRASH_SLOT, trashButton(data));
        inventory.setItem(FILTER_SLOT, filterButton(data));
        inventory.setItem(NETWORK_SLOT, button(Material.SPYGLASS, "Инспектор сети", "Открыть список узлов, ошибок и perf", NamedTextColor.AQUA));
        inventory.setItem(SPEED_SLOT, upgradeButton(Material.SUGAR, "Speed", data.upgrades().speedLevel(), "ускоряет перенос источника/трубы"));
        inventory.setItem(STACK_SLOT, upgradeButton(Material.CHEST, "Stack", data.upgrades().stackLevel(), "увеличивает размер пачки"));
        inventory.setItem(FILTER_LEVEL_SLOT, upgradeButton(Material.PAPER, "Filter slots", data.upgrades().filterLevel(), "добавляет страницы фильтра"));
        inventory.setItem(PRIORITY_LEVEL_SLOT, upgradeButton(Material.GOLD_INGOT, "Priority", data.upgrades().priorityLevel(), "добавляет приоритет назначения"));
        inventory.setItem(RANGE_SLOT, upgradeButton(Material.ENDER_PEARL, "Range", data.upgrades().rangeLevel(), "увеличивает maxRouteLength"));
        inventory.setItem(SILENT_SLOT, upgradeButton(Material.AMETHYST_SHARD, "Silent", data.upgrades().silentLevel(), "отключает visual echo от источника"));
        inventory.setItem(CLOSE_SLOT, button(Material.BARRIER, "Закрыть", "Сохранить и закрыть", NamedTextColor.RED));
    }

    private ItemStack sideButton(MechanismBlockData data) {
        if (!data.type().supportsIoSide()) {
            return button(Material.GRAY_DYE, "Сторона недоступна", "Только extractor/inserter/overflow", NamedTextColor.DARK_GRAY);
        }
        Material material = data.ioSide() == MechanismIoSide.AUTO ? Material.COMPASS : Material.OBSERVER;
        return button(material, "Сторона: " + data.ioSide().russianName(), "Клик: переключить сторону контейнера", NamedTextColor.YELLOW);
    }

    private ItemStack channelButton(MechanismBlockData data) {
        if (!data.type().isPipe()) {
            return button(Material.GRAY_DYE, "Канал недоступен", "Только трубы", NamedTextColor.DARK_GRAY);
        }
        return button(data.pipeChannel().icon(), "Канал: " + data.pipeChannel().russianName(), "Трубы разных каналов не соединяются", NamedTextColor.YELLOW);
    }

    private ItemStack routeButton(MechanismBlockData data) {
        if (!data.type().supportsFilter()) {
            return button(Material.GRAY_DYE, "Маршрут недоступен", "Только router/destination блоки", NamedTextColor.DARK_GRAY);
        }
        return button(Material.REPEATER, "Маршрут: " + data.filter().routeMode().russianName(), "Клик: priority/nearest/round-robin/split", NamedTextColor.YELLOW);
    }

    private ItemStack redstoneButton(MechanismBlockData data) {
        if (data.type() != MechanismBlockType.EXTRACTOR) {
            return button(Material.GRAY_DYE, "Redstone недоступен", "Только extractor", NamedTextColor.DARK_GRAY);
        }
        return button(Material.REDSTONE_TORCH, "Redstone: " + data.redstoneMode().russianName(), "Клик: ignore/only powered/only unpowered", NamedTextColor.YELLOW);
    }

    private ItemStack trashButton(MechanismBlockData data) {
        if (data.type() != MechanismBlockType.TRASH) {
            return button(Material.GRAY_DYE, "Trash safety недоступен", "Только trash block", NamedTextColor.DARK_GRAY);
        }
        Material material = data.trashMode() == TrashMode.DISABLED ? Material.BARRIER : Material.LAVA_BUCKET;
        return button(material, "Trash: " + data.trashMode().russianName(), "disabled/filter only/accept all", NamedTextColor.YELLOW);
    }

    private ItemStack filterButton(MechanismBlockData data) {
        if (!data.type().supportsFilter()) {
            return button(Material.GRAY_DYE, "Фильтр недоступен", "У этого блока нет фильтра", NamedTextColor.DARK_GRAY);
        }
        return button(Material.HOPPER, "Фильтр", "Клик: открыть ghost filter GUI", NamedTextColor.YELLOW);
    }

    private ItemStack upgradeButton(Material material, String name, int level, String lore) {
        return button(material, name + ": " + level + "/3", lore, level > 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY);
    }

    public static UpgradeModules nextUpgrade(UpgradeModules current, Material material) {
        return switch (material) {
            case SUGAR -> current.withSpeedLevel(current.speedLevel() + 1);
            case CHEST -> current.withStackLevel(current.stackLevel() + 1);
            case PAPER -> current.withFilterLevel(current.filterLevel() + 1);
            case GOLD_INGOT -> current.withPriorityLevel(current.priorityLevel() + 1);
            case ENDER_PEARL -> current.withRangeLevel(current.rangeLevel() + 1);
            case AMETHYST_SHARD -> current.withSilentLevel(current.silentLevel() + 1);
            default -> current;
        };
    }

    public static boolean isUpgradeMaterial(Material material) {
        return material == Material.SUGAR
            || material == Material.CHEST
            || material == Material.PAPER
            || material == Material.GOLD_INGOT
            || material == Material.ENDER_PEARL
            || material == Material.AMETHYST_SHARD;
    }

    private ItemStack button(Material material, String name, String lore, NamedTextColor color) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    public static final class SettingsHolder implements InventoryHolder {
        private final BlockKey key;
        private Inventory inventory;

        private SettingsHolder(BlockKey key) {
            this.key = key;
        }

        private BlockKey key() {
            return key;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
