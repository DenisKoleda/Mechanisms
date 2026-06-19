package com.denis.mechanisms.visual;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.config.MechanismsConfig;
import com.denis.mechanisms.logistics.NetworkIndexer;
import com.denis.mechanisms.logistics.Route;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class VisualEffectService {
    private final JavaPlugin plugin;
    private final MechanismsConfig config;
    private final NetworkIndexer indexer;
    private final MechanismBlockRegistry registry;
    private final List<BukkitTask> tasks = new ArrayList<>();

    public VisualEffectService(JavaPlugin plugin, MechanismsConfig config, NetworkIndexer indexer, MechanismBlockRegistry registry) {
        this.plugin = plugin;
        this.config = config;
        this.indexer = indexer;
        this.registry = registry;
    }

    public void shutdown() {
        tasks.forEach(BukkitTask::cancel);
        tasks.clear();
    }

    public void showNetwork(BlockKey origin) {
        if (!config.visualsEnabled() || !config.particles()) {
            return;
        }
        indexer.networkFor(origin).ifPresent(network -> {
            List<BlockKey> nodes = List.copyOf(network.nodes());
            BukkitTask task = new BukkitRunnable() {
                private int ticks;

                @Override
                public void run() {
                    ticks += 10;
                    for (BlockKey node : nodes) {
                        center(node).ifPresent(location -> spawnNodeParticle(location, node, 1));
                    }
                    if (ticks >= 80) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 10L);
            tasks.add(task);
        });
    }

    public void showTransferEcho(Route route, ItemStack stack) {
        if (!config.visualsEnabled()) {
            return;
        }
        if (route.source().upgrades().hasSilent()) {
            return;
        }
        if (config.particles()) {
            List<BlockKey> path = route.path();
            for (int i = 0; i < path.size(); i++) {
                BlockKey currentKey = path.get(i);
                center(currentKey).ifPresent(location -> spawnNodeParticle(location, currentKey, 2));
                if (i + 1 < path.size()) {
                    Optional<Location> from = center(currentKey);
                    Optional<Location> to = center(path.get(i + 1));
                    if (from.isPresent() && to.isPresent()) {
                        spawnLineParticle(from.get(), to.get(), currentKey, path.get(i + 1));
                    }
                }
            }
        }
        if (!config.itemDisplayEcho() || route.path().isEmpty()) {
            return;
        }

        Optional<Location> start = center(route.path().getFirst());
        if (start.isEmpty()) {
            return;
        }
        World world = start.get().getWorld();
        ItemStack visualStack = stack.clone();
        visualStack.setAmount(1);
        ItemDisplay display = world.spawn(start.get(), ItemDisplay.class, entity -> {
            entity.setItemStack(visualStack);
            entity.setBillboard(Display.Billboard.CENTER);
            float scale = (float) config.itemDisplayScale();
            entity.setTransformation(new Transformation(
                new Vector3f(0.0f, 0.0f, 0.0f),
                new AxisAngle4f(),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f()
            ));
        });

        List<Location> locations = route.path().stream().map(this::center).flatMap(Optional::stream).toList();
        BukkitTask task = new BukkitRunnable() {
            private int index;

            @Override
            public void run() {
                if (!display.isValid() || index >= locations.size()) {
                    display.remove();
                    cancel();
                    return;
                }
                display.teleport(locations.get(index));
                index++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        tasks.add(task);
    }

    private Optional<Location> center(BlockKey key) {
        World world = Bukkit.getWorld(key.worldId());
        if (world == null) {
            return Optional.empty();
        }
        return Optional.of(new Location(world, key.x() + 0.5, key.y() + 0.55, key.z() + 0.5));
    }

    private void spawnNodeParticle(Location location, BlockKey key, int count) {
        registry.getRegistered(key)
            .filter(data -> data.type().isPipe())
            .ifPresentOrElse(
                data -> location.getWorld().spawnParticle(
                    Particle.DUST,
                    location,
                    count,
                    0.08,
                    0.08,
                    0.08,
                    0.0,
                    new Particle.DustOptions(pipeColor(data), 0.75f)
                ),
                () -> location.getWorld().spawnParticle(Particle.END_ROD, location, count, 0.08, 0.08, 0.08, 0.0)
            );
    }

    private void spawnLineParticle(Location from, Location to, BlockKey fromKey, BlockKey toKey) {
        World world = from.getWorld();
        if (world == null) {
            return;
        }
        Color color = registry.getRegistered(fromKey)
            .filter(data -> data.type().isPipe())
            .or(() -> registry.getRegistered(toKey).filter(data -> data.type().isPipe()))
            .map(this::pipeColor)
            .orElse(Color.WHITE);
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();
        int steps = Math.max(2, (int) Math.ceil(from.distance(to) * 4.0));
        for (int step = 1; step < steps; step++) {
            double t = step / (double) steps;
            Location point = from.clone().add(dx * t, dy * t, dz * t);
            world.spawnParticle(Particle.DUST, point, 1, 0.015, 0.015, 0.015, 0.0, new Particle.DustOptions(color, 0.55f));
        }
    }

    private Color pipeColor(MechanismBlockData data) {
        Color tier = switch (data.type()) {
            case PIPE -> Color.fromRGB(219, 128, 55);
            case PIPE_FAST -> Color.fromRGB(70, 220, 120);
            case PIPE_EXPRESS -> Color.fromRGB(178, 92, 255);
            default -> Color.WHITE;
        };
        if (data.pipeChannel() == com.denis.mechanisms.block.PipeChannel.DEFAULT) {
            return tier;
        }
        Color channel = data.pipeChannel().color();
        return Color.fromRGB(
            (tier.getRed() * 2 + channel.getRed() * 3) / 5,
            (tier.getGreen() * 2 + channel.getGreen() * 3) / 5,
            (tier.getBlue() * 2 + channel.getBlue() * 3) / 5
        );
    }
}
