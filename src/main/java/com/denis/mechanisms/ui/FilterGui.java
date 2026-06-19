package com.denis.mechanisms.ui;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.logistics.FilterMode;
import com.denis.mechanisms.logistics.FilterService;
import com.denis.mechanisms.logistics.FilterSettings;
import com.denis.mechanisms.logistics.MatchMode;
import com.denis.mechanisms.logistics.RouteMode;
import com.denis.mechanisms.logistics.StackUtil;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class FilterGui {
    private static final int SIZE = 54;
    private static final int FILTER_SLOTS_PER_PAGE = 36;
    private static final int MODE_SLOT = 36;
    private static final int MATCH_SLOT = 37;
    private static final int ROUTE_MODE_SLOT = 38;
    private static final int CLEAR_SLOT = 39;
    private static final int COPY_SLOT = 40;
    private static final int PASTE_SLOT = 41;
    private static final int SEARCH_SLOT = 42;
    private static final int PREV_SLOT = 45;
    private static final int PRIORITY_MINUS_TEN_SLOT = 46;
    private static final int PRIORITY_MINUS_ONE_SLOT = 47;
    private static final int PRIORITY_PLUS_ONE_SLOT = 48;
    private static final int PRIORITY_PLUS_TEN_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int CLOSE_SLOT = 53;

    private final MechanismBlockRegistry registry;
    private final MechanismsConfig config;
    private final FilterService filterService;
    private final Map<UUID, FilterSettings> clipboard = new HashMap<>();

    public FilterGui(MechanismBlockRegistry registry, MechanismsConfig config, FilterService filterService) {
        this.registry = registry;
        this.config = config;
        this.filterService = filterService;
    }

    public void open(Player player, MechanismBlockData data) {
        if (!data.type().supportsFilter()) {
            return;
        }
        FilterGuiHolder holder = new FilterGuiHolder(data.key());
        Inventory inventory = Bukkit.createInventory(holder, SIZE, "Mechanisms: фильтр");
        holder.setInventory(inventory);
        draw(inventory, data, holder.page());
        player.openInventory(inventory);
        player.sendActionBar(Component.text(data.type().russianName() + ": " + filterService.summary(data.filter()), NamedTextColor.YELLOW));
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof FilterGuiHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int rawSlot = event.getRawSlot();
        FilterGuiPolicy.ClickDecision decision = FilterGuiPolicy.decideClick(rawSlot, event.isShiftClick(), SIZE);
        if (decision == FilterGuiPolicy.ClickDecision.ALLOW) {
            return;
        }
        event.setCancelled(true);
        if (rawSlot < 0 || rawSlot >= SIZE) {
            return;
        }

        Optional<MechanismBlockData> maybeData = registry.getRegistered(holder.key());
        if (maybeData.isEmpty() || !maybeData.get().type().supportsFilter()) {
            player.closeInventory();
            return;
        }

        MechanismBlockData data = maybeData.get();
        Inventory inventory = event.getView().getTopInventory();
        FilterSettings current = data.filter();
        int maxPages = config.maxFilterPages(data.upgrades());

        if (rawSlot < FILTER_SLOTS_PER_PAGE) {
            int globalSlot = holder.page() * FILTER_SLOTS_PER_PAGE + rawSlot;
            if (globalSlot >= maxPages * FILTER_SLOTS_PER_PAGE) {
                return;
            }
            ItemStack cursor = event.getCursor();
            if (StackUtil.isEmpty(cursor)) {
                inventory.setItem(rawSlot, null);
            } else {
                ItemStack ghost = cursor.clone();
                ghost.setAmount(1);
                inventory.setItem(rawSlot, ghost);
            }
            saveVisiblePage(holder, inventory, current, maxPages);
            sendAction(player, holder.key());
            return;
        }

        FilterSettings withVisibleItems = current.withItems(mergedItems(current.items(), inventory, holder.page(), maxPages));
        switch (rawSlot) {
            case MODE_SLOT -> updateAndRedraw(player, holder, inventory, withVisibleItems.withMode(nextMode(withVisibleItems.mode())));
            case MATCH_SLOT -> updateAndRedraw(player, holder, inventory, withVisibleItems.withMatchMode(nextMatchMode(withVisibleItems.matchMode())));
            case ROUTE_MODE_SLOT -> updateAndRedraw(player, holder, inventory, withVisibleItems.withRouteMode(withVisibleItems.routeMode().next()));
            case PRIORITY_MINUS_TEN_SLOT -> updateAndRedraw(player, holder, inventory, withVisibleItems.withPriority(withVisibleItems.priority() - 10));
            case PRIORITY_MINUS_ONE_SLOT -> updateAndRedraw(player, holder, inventory, withVisibleItems.withPriority(withVisibleItems.priority() - 1));
            case PRIORITY_PLUS_ONE_SLOT -> updateAndRedraw(player, holder, inventory, withVisibleItems.withPriority(withVisibleItems.priority() + 1));
            case PRIORITY_PLUS_TEN_SLOT -> updateAndRedraw(player, holder, inventory, withVisibleItems.withPriority(withVisibleItems.priority() + 10));
            case CLEAR_SLOT -> updateAndRedraw(player, holder, inventory, withVisibleItems.withItems(List.of()));
            case COPY_SLOT -> {
                clipboard.put(player.getUniqueId(), withVisibleItems);
                player.sendActionBar(Component.text("Фильтр скопирован: " + withVisibleItems.itemCount() + " предметов", NamedTextColor.GREEN));
            }
            case PASTE_SLOT -> {
                FilterSettings copied = clipboard.get(player.getUniqueId());
                if (copied == null) {
                    player.sendActionBar(Component.text("Буфер фильтра пуст", NamedTextColor.RED));
                    return;
                }
                holder.setPage(0);
                updateAndRedraw(player, holder, inventory, copied);
            }
            case SEARCH_SLOT -> searchByCursor(player, holder, inventory, withVisibleItems, maxPages, event.getCursor());
            case PREV_SLOT -> {
                holder.setPage(Math.max(0, holder.page() - 1));
                draw(inventory, data.withFilter(withVisibleItems), holder.page());
            }
            case NEXT_SLOT -> {
                holder.setPage(Math.min(maxPages - 1, holder.page() + 1));
                draw(inventory, data.withFilter(withVisibleItems), holder.page());
            }
            case CLOSE_SLOT -> player.closeInventory();
            default -> {
            }
        }
    }

    public void handleDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof FilterGuiHolder
            && FilterGuiPolicy.shouldCancelDrag(event.getRawSlots(), SIZE)) {
            event.setCancelled(true);
        }
    }

    private void saveVisiblePage(FilterGuiHolder holder, Inventory inventory, FilterSettings current, int maxPages) {
        registry.updateFilter(holder.key(), current.withItems(mergedItems(current.items(), inventory, holder.page(), maxPages)));
        registry.getRegistered(holder.key()).ifPresent(data -> draw(inventory, data, holder.page()));
    }

    private void updateAndRedraw(Player player, FilterGuiHolder holder, Inventory inventory, FilterSettings settings) {
        registry.updateFilter(holder.key(), settings);
        registry.getRegistered(holder.key()).ifPresent(updated -> draw(inventory, updated, holder.page()));
        sendAction(player, holder.key());
    }

    private void searchByCursor(Player player, FilterGuiHolder holder, Inventory inventory, FilterSettings settings, int maxPages, ItemStack cursor) {
        if (StackUtil.isEmpty(cursor)) {
            player.sendActionBar(Component.text("Возьми предмет на курсор и нажми поиск", NamedTextColor.YELLOW));
            return;
        }
        for (int i = 0; i < settings.items().size(); i++) {
            ItemStack item = settings.items().get(i);
            if (matches(item, cursor, settings.matchMode())) {
                holder.setPage(Math.min(maxPages - 1, i / FILTER_SLOTS_PER_PAGE));
                registry.getRegistered(holder.key()).ifPresent(updated -> draw(inventory, updated, holder.page()));
                player.sendActionBar(Component.text("Найдено на странице " + (holder.page() + 1), NamedTextColor.GREEN));
                return;
            }
        }
        player.sendActionBar(Component.text("Такого предмета в фильтре нет", NamedTextColor.RED));
    }

    private List<ItemStack> mergedItems(List<ItemStack> currentItems, Inventory inventory, int page, int maxPages) {
        int capacity = maxPages * FILTER_SLOTS_PER_PAGE;
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < Math.min(currentItems.size(), capacity); i++) {
            result.add(currentItems.get(i).clone());
        }
        while (result.size() < capacity) {
            result.add(null);
        }
        int offset = page * FILTER_SLOTS_PER_PAGE;
        for (int slot = 0; slot < FILTER_SLOTS_PER_PAGE; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (StackUtil.isEmpty(item)) {
                result.set(offset + slot, null);
            } else {
                ItemStack clone = item.clone();
                clone.setAmount(1);
                result.set(offset + slot, clone);
            }
        }
        return result.stream()
            .filter(item -> !StackUtil.isEmpty(item))
            .map(item -> {
                ItemStack clone = item.clone();
                clone.setAmount(1);
                return clone;
            })
            .toList();
    }

    private void draw(Inventory inventory, MechanismBlockData data, int page) {
        FilterSettings settings = data.filter();
        inventory.clear();
        int offset = page * FILTER_SLOTS_PER_PAGE;
        for (int slot = 0; slot < FILTER_SLOTS_PER_PAGE; slot++) {
            int index = offset + slot;
            inventory.setItem(slot, index < settings.items().size() ? settings.items().get(index).clone() : null);
        }

        int maxPages = config.maxFilterPages(data.upgrades());
        inventory.setItem(MODE_SLOT, button(settings.mode() == FilterMode.WHITELIST ? Material.LIME_DYE : Material.RED_DYE, "Режим: " + modeName(settings.mode()), "Переключить белый/черный список"));
        inventory.setItem(MATCH_SLOT, button(Material.COMPARATOR, "Сравнение: " + matchName(settings.matchMode()), "Материал или точное совпадение"));
        inventory.setItem(ROUTE_MODE_SLOT, button(Material.REPEATER, "Маршрут: " + settings.routeMode().russianName(), "priority/nearest/round-robin/split"));
        inventory.setItem(CLEAR_SLOT, button(Material.BUCKET, "Очистить", "Удалить все ghost-items из фильтра"));
        inventory.setItem(COPY_SLOT, button(Material.PAPER, "Копировать", "Скопировать режим, приоритет и предметы"));
        inventory.setItem(PASTE_SLOT, button(Material.MAP, "Вставить", "Вставить фильтр из буфера"));
        inventory.setItem(SEARCH_SLOT, button(Material.SPYGLASS, "Поиск", "Возьми предмет на курсор и нажми"));
        inventory.setItem(PREV_SLOT, button(Material.ARROW, "Страница назад", "Сейчас: " + (page + 1) + "/" + maxPages));
        inventory.setItem(PRIORITY_MINUS_TEN_SLOT, button(Material.REDSTONE, "Приоритет -10", "Сейчас: " + settings.priority()));
        inventory.setItem(PRIORITY_MINUS_ONE_SLOT, button(Material.REDSTONE_TORCH, "Приоритет -1", "Сейчас: " + settings.priority()));
        inventory.setItem(PRIORITY_PLUS_ONE_SLOT, button(Material.TORCH, "Приоритет +1", "Сейчас: " + settings.priority()));
        inventory.setItem(PRIORITY_PLUS_TEN_SLOT, button(Material.EMERALD, "Приоритет +10", "Сейчас: " + settings.priority()));
        inventory.setItem(NEXT_SLOT, button(Material.SPECTRAL_ARROW, "Страница вперед", "Сейчас: " + (page + 1) + "/" + maxPages));
        inventory.setItem(CLOSE_SLOT, button(Material.BARRIER, "Закрыть", "Сохранить и закрыть"));
    }

    private ItemStack button(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(Component.text(lore, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        item.setItemMeta(meta);
        return item;
    }

    private void sendAction(Player player, BlockKey key) {
        registry.getRegistered(key).ifPresent(data ->
            player.sendActionBar(Component.text(data.type().russianName() + ": " + filterService.summary(data.filter()), NamedTextColor.YELLOW))
        );
    }

    private FilterMode nextMode(FilterMode mode) {
        return mode == FilterMode.WHITELIST ? FilterMode.BLACKLIST : FilterMode.WHITELIST;
    }

    private MatchMode nextMatchMode(MatchMode mode) {
        return mode == MatchMode.MATERIAL_ONLY ? MatchMode.EXACT_META : MatchMode.MATERIAL_ONLY;
    }

    private boolean matches(ItemStack candidate, ItemStack filter, MatchMode mode) {
        if (StackUtil.isEmpty(candidate) || StackUtil.isEmpty(filter)) {
            return false;
        }
        return mode == MatchMode.MATERIAL_ONLY ? candidate.getType() == filter.getType() : candidate.isSimilar(filter);
    }

    private String modeName(FilterMode mode) {
        return mode == FilterMode.WHITELIST ? "белый список" : "черный список";
    }

    private String matchName(MatchMode mode) {
        return mode == MatchMode.MATERIAL_ONLY ? "только материал" : "точное совпадение";
    }

    public static final class FilterGuiHolder implements InventoryHolder {
        private final BlockKey key;
        private Inventory inventory;
        private int page;

        private FilterGuiHolder(BlockKey key) {
            this.key = key;
        }

        public BlockKey key() {
            return key;
        }

        private int page() {
            return page;
        }

        private void setPage(int page) {
            this.page = Math.max(0, page);
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
