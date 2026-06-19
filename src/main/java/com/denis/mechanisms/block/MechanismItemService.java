package com.denis.mechanisms.block;

import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.logistics.StackUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MechanismItemService {
    private final MechanismsConfig config;
    private final NamespacedKey mechanismTypeKey;
    private final NamespacedKey wandKey;
    private final NamespacedKey wrenchKey;

    public MechanismItemService(JavaPlugin plugin, MechanismsConfig config) {
        this.config = config;
        this.mechanismTypeKey = new NamespacedKey(plugin, "mechanism_type");
        this.wandKey = new NamespacedKey(plugin, "mechanism_wand");
        this.wrenchKey = new NamespacedKey(plugin, "mechanism_wrench");
    }

    public ItemStack createMechanismItem(MechanismBlockType type, int amount) {
        ItemStack item = new ItemStack(config.materialFor(type), amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(type.russianName(), type.color()).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Серверный блок логистики", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (type.isPipe()) {
            lore.add(Component.text("Скорость: " + config.itemsPerTransferFor(type) + " предметов за перенос", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Каналы настраиваются ключом", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Поставь блок, чтобы зарегистрировать механизм", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Ключ: статус, Shift+ключ: настройки", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(mechanismTypeKey, PersistentDataType.STRING, type.token());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createWrench() {
        ItemStack item = new ItemStack(Material.IRON_AXE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Ключ Mechanisms", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("ПКМ по механизму: статус", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Shift+ПКМ: настройки, фильтры и каналы", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Оффхенд с модулем: применить апгрейд", NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(wrenchKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createWand() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Жезл Mechanisms", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
            Component.text("ПКМ по механизму: диагностика", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
            Component.text("Shift+ПКМ по router/inserter: фильтр", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public Optional<MechanismBlockType> typeFromItem(ItemStack item) {
        if (StackUtil.isEmpty(item) || !item.hasItemMeta()) {
            return Optional.empty();
        }
        String value = item.getItemMeta().getPersistentDataContainer().get(mechanismTypeKey, PersistentDataType.STRING);
        return MechanismBlockType.fromToken(value);
    }

    public boolean isWand(ItemStack item) {
        if (StackUtil.isEmpty(item) || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(wandKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }

    public boolean isWrench(ItemStack item) {
        if (StackUtil.isEmpty(item) || !item.hasItemMeta()) {
            return false;
        }
        Byte marker = item.getItemMeta().getPersistentDataContainer().get(wrenchKey, PersistentDataType.BYTE);
        return marker != null && marker == (byte) 1;
    }
}
