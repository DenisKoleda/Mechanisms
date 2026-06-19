package com.denis.mechanisms.command;

import com.denis.mechanisms.MechanismsPlugin;
import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.MechanismItemService;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.crafting.MechanismRecipeService;
import com.denis.mechanisms.logistics.LogisticsStats;
import com.denis.mechanisms.logistics.MechanismNetwork;
import com.denis.mechanisms.logistics.NetworkIndexer;
import com.denis.mechanisms.logistics.PendingTransferStore;
import com.denis.mechanisms.selftest.SelfTestService;
import com.denis.mechanisms.ui.MechanismMenu;
import com.denis.mechanisms.ui.NetworkInspectorGui;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public final class MechanismsCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("menu", "give", "recipes", "network", "doctor", "log", "perf", "wand", "wrench", "reload", "list", "stats", "debug", "selftest", "help");

    private final MechanismsPlugin plugin;
    private final MechanismsConfig config;
    private final MechanismItemService itemService;
    private final MechanismBlockRegistry registry;
    private final NetworkIndexer networkIndexer;
    private final LogisticsStats stats;
    private final PendingTransferStore pendingStore;
    private final SelfTestService selfTestService;
    private final MechanismMenu mechanismMenu;
    private final MechanismRecipeService recipeService;
    private final NetworkInspectorGui networkInspectorGui;

    public MechanismsCommand(
        MechanismsPlugin plugin,
        MechanismsConfig config,
        MechanismItemService itemService,
        MechanismBlockRegistry registry,
        NetworkIndexer networkIndexer,
        LogisticsStats stats,
        PendingTransferStore pendingStore,
        SelfTestService selfTestService,
        MechanismMenu mechanismMenu,
        MechanismRecipeService recipeService,
        NetworkInspectorGui networkInspectorGui
    ) {
        this.plugin = plugin;
        this.config = config;
        this.itemService = itemService;
        this.registry = registry;
        this.networkIndexer = networkIndexer;
        this.stats = stats;
        this.pendingStore = pendingStore;
        this.selfTestService = selfTestService;
        this.mechanismMenu = mechanismMenu;
        this.recipeService = recipeService;
        this.networkInspectorGui = networkInspectorGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "menu", "give" -> menu(sender);
            case "recipes" -> recipes(sender);
            case "network" -> network(sender);
            case "doctor" -> doctor(sender);
            case "log" -> log(sender, args);
            case "perf" -> perf(sender);
            case "wand" -> wand(sender);
            case "wrench" -> wrench(sender);
            case "reload" -> reload(sender);
            case "list" -> list(sender);
            case "stats" -> stats(sender);
            case "debug" -> debug(sender, args);
            case "selftest" -> selftest(sender, args);
            case "help" -> help(sender);
            default -> help(sender);
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug")) {
            return filter(List.of("on", "off"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("selftest")) {
            return filter(List.of("keep"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("log")) {
            return filter(List.of("last", "block"), args[1]);
        }
        return List.of();
    }

    private boolean menu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Меню доступно только игроку в игре.", NamedTextColor.RED));
            return true;
        }
        if (!has(player, "mechanisms.use")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        mechanismMenu.open(player);
        return true;
    }

    private boolean wand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Консоль не может получить жезл.", NamedTextColor.RED));
            return true;
        }
        if (!canReceiveMechanisms(player)) {
            sender.sendMessage(Component.text("Жезл можно получить только админам или игрокам в creative.", NamedTextColor.RED));
            return true;
        }
        giveItem(player, itemService.createWand());
        sender.sendMessage(config.message("wandGiven"));
        return true;
    }

    private boolean wrench(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Консоль не может получить ключ.", NamedTextColor.RED));
            return true;
        }
        if (!canReceiveMechanisms(player)) {
            sender.sendMessage(Component.text("Ключ можно получить только админам или игрокам в creative.", NamedTextColor.RED));
            return true;
        }
        giveItem(player, itemService.createWrench());
        sender.sendMessage(config.message("wrenchGiven"));
        return true;
    }

    private boolean recipes(CommandSender sender) {
        if (!has(sender, "mechanisms.use")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        sender.sendMessage(Component.text("Рецепты Mechanisms, профиль: " + config.craftingProfile().russianName(), NamedTextColor.GOLD));
        for (MechanismBlockType type : MechanismBlockType.values()) {
            MechanismRecipeService.RecipeInfo recipe = recipeService.recipeInfo(type);
            sender.sendMessage(Component.text(type.russianName() + " x" + recipe.resultAmount(), type.color()));
            sender.sendMessage(Component.text(String.join(" / ", recipe.shape()), NamedTextColor.WHITE));
            sender.sendMessage(Component.text(String.join(", ", recipe.legend()), NamedTextColor.GRAY));
        }
        MechanismRecipeService.ToolRecipeInfo wrench = recipeService.wrenchRecipeInfo();
        sender.sendMessage(Component.text(wrench.name() + " x" + wrench.resultAmount(), NamedTextColor.YELLOW));
        sender.sendMessage(Component.text(String.join(" / ", wrench.shape()), NamedTextColor.WHITE));
        sender.sendMessage(Component.text(String.join(", ", wrench.legend()), NamedTextColor.GRAY));
        return true;
    }

    private boolean network(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Network inspector доступен только игроку в игре.", NamedTextColor.RED));
            return true;
        }
        if (!has(player, "mechanisms.use")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        Block target = player.getTargetBlockExact(8);
        if (target == null) {
            sender.sendMessage(Component.text("Посмотри на блок Mechanisms в радиусе 8 блоков.", NamedTextColor.RED));
            return true;
        }
        registry.getAt(target).ifPresentOrElse(
            data -> networkInspectorGui.open(player, data),
            () -> sender.sendMessage(Component.text("Это не блок Mechanisms.", NamedTextColor.RED))
        );
        return true;
    }

    private boolean doctor(CommandSender sender) {
        if (!has(sender, "mechanisms.admin")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        long tooLarge = networkIndexer.graph().networks().stream().filter(MechanismNetwork::tooLarge).count();
        long unloaded = registry.all().stream().filter(this::isUnloaded).count();
        Map<String, Long> lastErrors = new TreeMap<>();
        stats.statusSnapshot().values().stream()
            .filter(status -> !"ok".equals(status.code()))
            .forEach(status -> lastErrors.merge(status.code(), 1L, Long::sum));

        sender.sendMessage(Component.text("Mechanisms doctor", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Блоки: " + registry.count() + ", сети: " + networkIndexer.graph().count() + ", pending: " + pendingStore.count(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Профиль рецептов: " + config.craftingProfile().russianName() + ", recipes registered: " + recipeService.allRegistered(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Unloaded records: " + unloaded + ", too-large networks: " + tooLarge, unloaded > 0 || tooLarge > 0 ? NamedTextColor.YELLOW : NamedTextColor.GREEN));
        sender.sendMessage(Component.text(String.format(Locale.ROOT, "Cycle avg/last/max: %.3f / %.3f / %.3f ms", stats.averageCycleMillis(), stats.lastCycleMillis(), stats.maxCycleMillis()), NamedTextColor.GRAY));
        if (lastErrors.isEmpty()) {
            sender.sendMessage(Component.text("Последних ошибок нет.", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("Последние ошибки:", NamedTextColor.YELLOW));
            lastErrors.forEach((code, count) -> sender.sendMessage(Component.text("- " + code + ": " + count, NamedTextColor.GRAY)));
        }
        return true;
    }

    private boolean log(CommandSender sender, String[] args) {
        if (!has(sender, "mechanisms.admin")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        String mode = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "last";
        int limit = args.length >= 3 ? parseLimit(args[2]) : 10;
        if ("block".equals(mode)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("/mech log block доступен только игроку, который смотрит на механизм.", NamedTextColor.RED));
                return true;
            }
            Block target = player.getTargetBlockExact(8);
            if (target == null) {
                sender.sendMessage(Component.text("Посмотри на блок Mechanisms в радиусе 8 блоков.", NamedTextColor.RED));
                return true;
            }
            return registry.getAt(target).map(data -> {
                printLogs(sender, "Логи блока " + data.key().shortString(), stats.recentLogs(data.key(), limit));
                return true;
            }).orElseGet(() -> {
                sender.sendMessage(Component.text("Это не блок Mechanisms.", NamedTextColor.RED));
                return true;
            });
        }
        printLogs(sender, "Последние логи Mechanisms", stats.recentLogs(limit));
        return true;
    }

    private boolean perf(CommandSender sender) {
        if (!has(sender, "mechanisms.admin")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        sender.sendMessage(Component.text("Performance Mechanisms", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Cycles: " + stats.cycleCount(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(String.format(Locale.ROOT, "Cycle ms avg/last/max: %.3f / %.3f / %.3f", stats.averageCycleMillis(), stats.lastCycleMillis(), stats.maxCycleMillis()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text(String.format(Locale.ROOT, "Transfers/sec: %.2f", stats.transfersPerSecond()), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Transfers: " + stats.totalTransfers() + ", items: " + stats.totalItems() + ", failures: " + stats.failedAttempts(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Networks: " + networkIndexer.graph().count() + ", pending: " + pendingStore.count(), NamedTextColor.GRAY));
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!has(sender, "mechanisms.reload")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        plugin.reloadMechanisms();
        sender.sendMessage(config.message("reloaded"));
        return true;
    }

    private boolean list(CommandSender sender) {
        if (!has(sender, "mechanisms.admin")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        sender.sendMessage(Component.text("Блоки Mechanisms: " + registry.count(), NamedTextColor.AQUA));
        for (Map.Entry<MechanismBlockType, Integer> entry : registry.countByType().entrySet()) {
            sender.sendMessage(Component.text("- " + entry.getKey().russianName() + " (" + entry.getKey().token() + "): " + entry.getValue(), NamedTextColor.GRAY));
        }
        sender.sendMessage(Component.text("Сети: " + networkIndexer.graph().count(), NamedTextColor.GRAY));
        for (Map.Entry<String, Integer> entry : registry.countByWorld().entrySet()) {
            sender.sendMessage(Component.text("- " + entry.getKey() + ": " + entry.getValue(), NamedTextColor.DARK_GRAY));
        }
        return true;
    }

    private boolean stats(CommandSender sender) {
        if (!has(sender, "mechanisms.admin")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        sender.sendMessage(Component.text("Статистика логистики Mechanisms", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Переносов: " + stats.totalTransfers(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Предметов перенесено: " + stats.totalItems(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Ошибок/пропусков: " + stats.failedAttempts(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("В pending recovery: " + pendingStore.count(), NamedTextColor.GRAY));
        sender.sendMessage(Component.text("Сети: " + networkIndexer.graph().count(), NamedTextColor.GRAY));
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        if (!has(sender, "mechanisms.debug")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        if (args.length < 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            sender.sendMessage(Component.text("Использование: /mech debug on|off", NamedTextColor.RED));
            return true;
        }
        boolean enabled = args[1].equalsIgnoreCase("on");
        config.setDebug(enabled);
        sender.sendMessage(config.message(enabled ? "debugOn" : "debugOff"));
        return true;
    }

    private boolean selftest(CommandSender sender, String[] args) {
        if (!has(sender, "mechanisms.admin")) {
            sender.sendMessage(config.message("noPermission"));
            return true;
        }
        boolean keep = args.length >= 2 && args[1].equalsIgnoreCase("keep");
        selfTestService.run(sender, keep);
        return true;
    }

    private boolean help(CommandSender sender) {
        sender.sendMessage(Component.text("Команды Mechanisms", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/mech menu - меню механизмов, выдача для admin/creative, рецепты для всех", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech give - алиас /mech menu", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech recipes - показать рецепты в чате", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech network - инспектор сети блока, на который смотришь", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech doctor - диагностика сетей, chunk warning и последних ошибок", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech log last [n] - последние transfer/error события", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech log block [n] - события блока, на который смотришь", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech perf - performance counters", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech wand - выдать жезл диагностики admin/creative", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech wrench - выдать ключ настроек admin/creative", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech reload - перезагрузить config/messages/data", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech list - список блоков и сетей", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech stats - статистика переносов", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech debug on|off", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/mech selftest [keep] - live-тест логистики", NamedTextColor.GRAY));
        return true;
    }

    private void printLogs(CommandSender sender, String title, List<LogisticsStats.LogEntry> entries) {
        sender.sendMessage(Component.text(title, NamedTextColor.GOLD));
        if (entries.isEmpty()) {
            sender.sendMessage(Component.text("Лог пуст.", NamedTextColor.GRAY));
            return;
        }
        entries.stream()
            .sorted(Comparator.comparingLong(LogisticsStats.LogEntry::atMillis))
            .forEach(entry -> sender.sendMessage(Component.text(
                "- " + age(entry.atMillis()) + " " + entry.key().shortString() + " [" + entry.code() + "] " + entry.detail(),
                "ok".equals(entry.code()) ? NamedTextColor.GREEN : NamedTextColor.RED
            )));
    }

    private boolean isUnloaded(MechanismBlockData data) {
        return data.world()
            .map(world -> !world.isChunkLoaded(data.x() >> 4, data.z() >> 4))
            .orElse(false);
    }

    private int parseLimit(String value) {
        try {
            return Math.max(1, Math.min(50, Integer.parseInt(value)));
        } catch (NumberFormatException ex) {
            return 10;
        }
    }

    private String age(long atMillis) {
        Duration duration = Duration.between(Instant.ofEpochMilli(atMillis), Instant.now());
        if (duration.toSeconds() < 60) {
            return duration.toSeconds() + "s";
        }
        return duration.toMinutes() + "m";
    }

    private void giveItem(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
    }

    private boolean has(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.hasPermission("mechanisms.admin");
    }

    private boolean canReceiveMechanisms(Player player) {
        return player.getGameMode() == GameMode.CREATIVE
            || player.hasPermission("mechanisms.give")
            || player.hasPermission("mechanisms.admin");
    }

    private List<String> filter(List<String> values, String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return values;
        }
        String normalized = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(normalized)).toList();
    }
}
