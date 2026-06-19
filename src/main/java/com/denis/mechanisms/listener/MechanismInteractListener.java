package com.denis.mechanisms.listener;

import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.MechanismItemService;
import com.denis.mechanisms.block.UpgradeModules;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.logistics.StackUtil;
import com.denis.mechanisms.ui.FilterGui;
import com.denis.mechanisms.ui.MechanismSettingsGui;
import com.denis.mechanisms.ui.StatusRenderer;
import com.denis.mechanisms.visual.VisualEffectService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class MechanismInteractListener implements Listener {
    private final MechanismItemService itemService;
    private final MechanismsConfig config;
    private final MechanismBlockRegistry registry;
    private final StatusRenderer statusRenderer;
    private final FilterGui filterGui;
    private final MechanismSettingsGui settingsGui;
    private final VisualEffectService visualEffectService;

    public MechanismInteractListener(
        MechanismItemService itemService,
        MechanismsConfig config,
        MechanismBlockRegistry registry,
        StatusRenderer statusRenderer,
        FilterGui filterGui,
        MechanismSettingsGui settingsGui,
        VisualEffectService visualEffectService
    ) {
        this.itemService = itemService;
        this.config = config;
        this.registry = registry;
        this.statusRenderer = statusRenderer;
        this.filterGui = filterGui;
        this.settingsGui = settingsGui;
        this.visualEffectService = visualEffectService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        boolean wand = itemService.isWand(event.getItem());
        boolean wrench = itemService.isWrench(event.getItem());
        registry.getAt(block).ifPresentOrElse(data -> handleMechanism(event, data), () -> {
            if (wand || wrench) {
                event.getPlayer().sendMessage(Component.text("Это не блок Mechanisms.", NamedTextColor.GRAY));
            }
        });
    }

    private void handleMechanism(PlayerInteractEvent event, MechanismBlockData data) {
        if (!event.getPlayer().hasPermission("mechanisms.use") && !event.getPlayer().hasPermission("mechanisms.admin")) {
            event.getPlayer().sendMessage(config.message("noPermission"));
            return;
        }

        boolean wand = itemService.isWand(event.getItem());
        boolean wrench = itemService.isWrench(event.getItem());
        if (wrench) {
            event.setCancelled(true);
            if (!event.getPlayer().isSneaking() && tryPipeUpgrade(event, data)) {
                return;
            }
            if (!event.getPlayer().isSneaking() && tryModuleUpgrade(event, data)) {
                return;
            }
            if (event.getPlayer().isSneaking()) {
                settingsGui.open(event.getPlayer(), data);
            } else {
                statusRenderer.sendStatus(event.getPlayer(), data);
                if (data.type().isPipe() || data.type().supportsFilter()) {
                    visualEffectService.showNetwork(data.key());
                }
            }
            return;
        }
        boolean placeableBlockInHand = isPlaceableBlock(event.getItem());
        InteractionPolicy.Decision decision = InteractionPolicy.decide(
            wand,
            event.getPlayer().isSneaking(),
            placeableBlockInHand,
            data.type().supportsFilter()
        );

        switch (decision) {
            case PASS_THROUGH -> {
                return;
            }
            case SHOW_HINT -> {
                event.setCancelled(true);
                event.getPlayer().sendActionBar(Component.text(
                    placeableBlockInHand
                        ? "Shift+ПКМ с блоком: поставить. Ключ: статус."
                        : "Ключ: статус. Shift+ПКМ пустой рукой: настройки.",
                    NamedTextColor.GRAY
                ));
            }
            case OPEN_FILTER -> {
                event.setCancelled(true);
                filterGui.open(event.getPlayer(), data);
            }
            case SHOW_STATUS -> {
                event.setCancelled(true);
                statusRenderer.sendStatus(event.getPlayer(), data);
                if (data.type().isPipe() || data.type().supportsFilter()) {
                    visualEffectService.showNetwork(data.key());
                }
            }
        }
    }

    private boolean isPlaceableBlock(ItemStack item) {
        return !StackUtil.isEmpty(item) && item.getType().isBlock();
    }

    private boolean tryPipeUpgrade(PlayerInteractEvent event, MechanismBlockData data) {
        if (!data.type().isPipe()) {
            return false;
        }
        ItemStack offhand = event.getPlayer().getInventory().getItemInOffHand();
        if (StackUtil.isEmpty(offhand)) {
            return false;
        }

        MechanismBlockType targetType = null;
        boolean consume = false;
        if (data.type() == MechanismBlockType.PIPE && offhand.getType() == Material.REDSTONE_BLOCK) {
            targetType = MechanismBlockType.PIPE_FAST;
            consume = true;
        } else if (data.type() == MechanismBlockType.PIPE_FAST && offhand.getType() == Material.DIAMOND) {
            targetType = MechanismBlockType.PIPE_EXPRESS;
            consume = true;
        } else if (data.type() == MechanismBlockType.PIPE_EXPRESS && offhand.getType() == Material.SHEARS) {
            targetType = MechanismBlockType.PIPE_FAST;
        } else if (data.type() == MechanismBlockType.PIPE_FAST && offhand.getType() == Material.SHEARS) {
            targetType = MechanismBlockType.PIPE;
        }
        if (targetType == null) {
            return false;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return false;
        }
        Material targetMaterial = config.materialFor(targetType);
        block.setType(targetMaterial, false);
        registry.updateTypeAndMaterial(data.key(), targetType, targetMaterial);
        if (consume) {
            consumeOffhand(event, offhand);
        }
        event.getPlayer().sendActionBar(Component.text("Труба изменена: " + targetType.russianName(), targetType.color()));
        return true;
    }

    private boolean tryModuleUpgrade(PlayerInteractEvent event, MechanismBlockData data) {
        ItemStack offhand = event.getPlayer().getInventory().getItemInOffHand();
        if (StackUtil.isEmpty(offhand) || !MechanismSettingsGui.isUpgradeMaterial(offhand.getType())) {
            return false;
        }
        if (!canApplyModule(data, offhand.getType())) {
            event.getPlayer().sendActionBar(Component.text("Этот модуль не подходит для " + data.type().russianName(), NamedTextColor.RED));
            return true;
        }

        UpgradeModules current = data.upgrades();
        UpgradeModules updated = MechanismSettingsGui.nextUpgrade(current, offhand.getType());
        if (updated.equals(current)) {
            event.getPlayer().sendActionBar(Component.text("Модуль уже максимального уровня", NamedTextColor.YELLOW));
            return true;
        }
        registry.updateUpgrades(data.key(), updated);
        consumeOffhand(event, offhand);
        event.getPlayer().sendActionBar(Component.text("Модуль применен: " + moduleName(offhand.getType()), NamedTextColor.GREEN));
        return true;
    }

    private boolean canApplyModule(MechanismBlockData data, Material material) {
        return switch (material) {
            case SUGAR, CHEST -> data.type().isPipe() || data.type() == MechanismBlockType.EXTRACTOR;
            case PAPER, GOLD_INGOT -> data.type().supportsFilter();
            case ENDER_PEARL -> data.type() == MechanismBlockType.EXTRACTOR;
            case AMETHYST_SHARD -> true;
            default -> false;
        };
    }

    private String moduleName(Material material) {
        return switch (material) {
            case SUGAR -> "Speed";
            case CHEST -> "Stack";
            case PAPER -> "Filter slots";
            case GOLD_INGOT -> "Priority";
            case ENDER_PEARL -> "Range";
            case AMETHYST_SHARD -> "Silent";
            default -> material.name().toLowerCase();
        };
    }

    private void consumeOffhand(PlayerInteractEvent event, ItemStack offhand) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {
            return;
        }
        if (offhand.getAmount() <= 1) {
            event.getPlayer().getInventory().setItemInOffHand(null);
        } else {
            offhand.setAmount(offhand.getAmount() - 1);
        }
    }
}
