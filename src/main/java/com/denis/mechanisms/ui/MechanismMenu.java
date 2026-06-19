package com.denis.mechanisms.ui;

import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.MechanismItemService;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.crafting.MechanismRecipeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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

public final class MechanismMenu {
    private static final int SIZE = 45;
    private static final int WAND_SLOT = 39;
    private static final int WRENCH_SLOT = 40;
    private static final int MODULES_SLOT = 41;
    private static final Map<Integer, MechanismBlockType> TYPE_SLOTS = Map.of(
        10, MechanismBlockType.EXTRACTOR,
        11, MechanismBlockType.PIPE,
        12, MechanismBlockType.PIPE_FAST,
        13, MechanismBlockType.PIPE_EXPRESS,
        14, MechanismBlockType.ROUTER,
        15, MechanismBlockType.INSERTER,
        16, MechanismBlockType.OVERFLOW,
        17, MechanismBlockType.TRASH
    );

    private final MechanismItemService itemService;
    private final MechanismRecipeService recipeService;
    private final MechanismsConfig config;

    public MechanismMenu(MechanismItemService itemService, MechanismRecipeService recipeService, MechanismsConfig config) {
        this.itemService = itemService;
        this.recipeService = recipeService;
        this.config = config;
    }

    public void open(Player player) {
        boolean canTake = canTake(player);
        MechanismMenuHolder holder = new MechanismMenuHolder(canTake);
        Inventory inventory = Bukkit.createInventory(holder, SIZE, "Mechanisms: меню");
        holder.setInventory(inventory);

        inventory.setItem(4, infoItem(canTake));
        for (Map.Entry<Integer, MechanismBlockType> entry : TYPE_SLOTS.entrySet()) {
            MechanismBlockType type = entry.getValue();
            MechanismRecipeService.RecipeInfo recipe = recipeService.recipeInfo(type);
            inventory.setItem(entry.getKey(), menuItem(type, recipe, canTake));
            holder.put(entry.getKey(), new MenuEntry(type, null, recipe.resultAmount()));
        }

        inventory.setItem(WRENCH_SLOT, wrenchItem(canTake));
        holder.put(WRENCH_SLOT, new MenuEntry(null, "wrench", 1));
        inventory.setItem(MODULES_SLOT, moduleInfoItem());
        if (canTake) {
            inventory.setItem(WAND_SLOT, wandItem());
            holder.put(WAND_SLOT, new MenuEntry(null, "wand", 1));
        }

        player.openInventory(inventory);
        player.sendActionBar(Component.text(canTake ? "Клик по предмету: получить. Shift+клик: стак." : "Меню показывает рецепты. Выдача только для admin/creative.", NamedTextColor.YELLOW));
    }

    public void handleClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MechanismMenuHolder holder)) {
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
        MenuEntry entry = holder.entry(rawSlot);
        if (entry == null) {
            return;
        }
        if (!holder.canTake()) {
            player.sendActionBar(Component.text("Получение доступно только админам или игрокам в creative.", NamedTextColor.RED));
            return;
        }

        ItemStack item;
        if ("wand".equals(entry.tool())) {
            item = itemService.createWand();
        } else if ("wrench".equals(entry.tool())) {
            item = itemService.createWrench();
        } else {
            int amount = event.isShiftClick() ? 64 : entry.amount();
            item = itemService.createMechanismItem(entry.type(), amount);
        }
        giveItem(player, item);
        player.sendActionBar(Component.text("Выдано: " + item.getAmount() + " x " + displayName(entry), NamedTextColor.GREEN));
    }

    public void handleDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MechanismMenuHolder)) {
            return;
        }
        if (event.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot < SIZE)) {
            event.setCancelled(true);
        }
    }

    private boolean canTake(Player player) {
        return player.getGameMode() == GameMode.CREATIVE
            || player.hasPermission("mechanisms.admin")
            || player.hasPermission("mechanisms.give");
    }

    private ItemStack infoItem(boolean canTake) {
        List<Component> lore = new ArrayList<>();
        lore.add(line("Профиль рецептов: " + config.craftingProfile().russianName(), NamedTextColor.GRAY));
        lore.add(line(canTake ? "Выдача доступна" : "Выдача недоступна", canTake ? NamedTextColor.GREEN : NamedTextColor.RED));
        lore.add(line("Рецепты работают в survival через recipe book", NamedTextColor.DARK_GRAY));
        return item(Material.KNOWLEDGE_BOOK, "Mechanisms", NamedTextColor.GOLD, lore);
    }

    private ItemStack menuItem(MechanismBlockType type, MechanismRecipeService.RecipeInfo recipe, boolean canTake) {
        ItemStack item = itemService.createMechanismItem(type, 1);
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = new ArrayList<>();
        lore.add(line(canTake ? "Клик: получить " + recipe.resultAmount() : "Только просмотр: рецепт ниже", canTake ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        if (canTake) {
            lore.add(line("Shift+клик: получить стак", NamedTextColor.DARK_GREEN));
        }
        if (type.isPipe()) {
            lore.add(line("Скорость: " + config.itemsPerTransferFor(type) + " предметов за перенос", NamedTextColor.DARK_AQUA));
            lore.add(line("Каналы меняются ключом в настройках", NamedTextColor.DARK_AQUA));
        }
        lore.add(Component.empty());
        lore.add(line("Рецепт:", NamedTextColor.YELLOW));
        for (String row : recipe.shape()) {
            lore.add(line(row, NamedTextColor.WHITE));
        }
        for (String line : recipe.legend()) {
            lore.add(line(line, NamedTextColor.GRAY));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack wandItem() {
        ItemStack item = itemService.createWand();
        ItemMeta meta = item.getItemMeta();
        meta.lore(List.of(
            line("Админский инструмент диагностики", NamedTextColor.GRAY),
            line("Клик: получить 1", NamedTextColor.GREEN)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack wrenchItem(boolean canTake) {
        ItemStack item = itemService.createWrench();
        ItemMeta meta = item.getItemMeta();
        MechanismRecipeService.ToolRecipeInfo recipe = recipeService.wrenchRecipeInfo();
        List<Component> lore = new ArrayList<>();
        lore.add(line(canTake ? "Клик: получить 1" : "Только просмотр: рецепт ниже", canTake ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        lore.add(Component.empty());
        lore.add(line("Рецепт:", NamedTextColor.YELLOW));
        for (String row : recipe.shape()) {
            lore.add(line(row, NamedTextColor.WHITE));
        }
        for (String line : recipe.legend()) {
            lore.add(line(line, NamedTextColor.GRAY));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack moduleInfoItem() {
        List<Component> lore = List.of(
            line("Ключ в основной руке, модуль в оффхенде", NamedTextColor.GRAY),
            line("Sugar: speed, Chest: stack", NamedTextColor.DARK_AQUA),
            line("Paper: filter slots, Gold: priority", NamedTextColor.DARK_AQUA),
            line("Ender pearl: range, Amethyst: silent", NamedTextColor.DARK_AQUA)
        );
        return item(Material.AMETHYST_SHARD, "Модули апгрейда", NamedTextColor.LIGHT_PURPLE, lore);
    }

    private String displayName(MenuEntry entry) {
        if ("wand".equals(entry.tool())) {
            return "Жезл Mechanisms";
        }
        if ("wrench".equals(entry.tool())) {
            return "Ключ Mechanisms";
        }
        return entry.type().russianName();
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    private ItemStack item(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private record MenuEntry(MechanismBlockType type, String tool, int amount) {
    }

    public static final class MechanismMenuHolder implements InventoryHolder {
        private final boolean canTake;
        private final Map<Integer, MenuEntry> entries = new HashMap<>();
        private Inventory inventory;

        private MechanismMenuHolder(boolean canTake) {
            this.canTake = canTake;
        }

        private boolean canTake() {
            return canTake;
        }

        private void put(int slot, MenuEntry entry) {
            entries.put(slot, entry);
        }

        private MenuEntry entry(int slot) {
            return entries.get(slot);
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
