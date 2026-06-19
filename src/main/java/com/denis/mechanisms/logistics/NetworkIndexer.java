package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.ChunkKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.config.MechanismsConfig;
import org.bukkit.block.BlockFace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public final class NetworkIndexer {
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
    private final ChunkLoadChecker chunkLoadChecker;
    private NetworkGraph graph = new NetworkGraph(List.of(), 0L);
    private long revision;

    public NetworkIndexer(MechanismBlockRegistry registry, MechanismsConfig config) {
        this(registry, config, NetworkIndexer::isBukkitChunkLoaded);
    }

    NetworkIndexer(MechanismBlockRegistry registry, MechanismsConfig config, ChunkLoadChecker chunkLoadChecker) {
        this.registry = registry;
        this.config = config;
        this.chunkLoadChecker = chunkLoadChecker;
    }

    public synchronized void rebuild() {
        List<MechanismBlockData> all = registry.all().stream()
            .filter(this::isUsableNode)
            .collect(Collectors.toCollection(ArrayList::new));
        Set<BlockKey> unvisited = all.stream().map(MechanismBlockData::key).collect(Collectors.toCollection(HashSet::new));
        List<MechanismNetwork> networks = new ArrayList<>();

        while (!unvisited.isEmpty()) {
            BlockKey start = unvisited.stream().min(BlockKey.ORDER).orElseThrow();
            Set<BlockKey> nodes = new HashSet<>();
            Queue<BlockKey> queue = new ArrayDeque<>();
            boolean tooLarge = false;
            queue.add(start);
            unvisited.remove(start);

            while (!queue.isEmpty()) {
                BlockKey current = queue.remove();
                nodes.add(current);
                if (nodes.size() > config.maxNetworkNodes()) {
                    tooLarge = true;
                }

                for (MechanismBlockData neighbor : neighbors(current)) {
                    BlockKey neighborKey = neighbor.key();
                    if (!unvisited.contains(neighborKey)) {
                        continue;
                    }
                    unvisited.remove(neighborKey);
                    queue.add(neighborKey);
                }
            }

            BlockKey min = nodes.stream().min(BlockKey.ORDER).orElse(start);
            String id = "net-" + min.worldId().toString().substring(0, 8) + "-" + min.x() + "-" + min.y() + "-" + min.z();
            networks.add(new MechanismNetwork(id, Set.copyOf(nodes), tooLarge));
        }

        networks.sort(Comparator.comparing(MechanismNetwork::id));
        graph = new NetworkGraph(networks, ++revision);
    }

    public synchronized NetworkGraph graph() {
        return graph;
    }

    public synchronized Optional<MechanismNetwork> networkFor(BlockKey key) {
        return graph.networkFor(key);
    }

    public List<MechanismBlockData> neighbors(BlockKey key) {
        List<MechanismBlockData> neighbors = new ArrayList<>();
        ChunkKey currentChunk = ChunkKey.from(key);
        Optional<MechanismBlockData> currentData = registry.getRegistered(key);
        if (currentData.isEmpty()) {
            return neighbors;
        }
        for (BlockFace face : FACES) {
            BlockKey neighborKey = key.relative(face);
            if (!config.allowCrossChunk() && !ChunkKey.from(neighborKey).equals(currentChunk)) {
                continue;
            }
            registry.getRegistered(neighborKey)
                .filter(this::isUsableNode)
                .filter(neighbor -> canConnect(currentData.get(), neighbor))
                .ifPresent(neighbors::add);
        }
        return neighbors;
    }

    private boolean canConnect(MechanismBlockData current, MechanismBlockData neighbor) {
        if (current.type().isPipe() && neighbor.type().isPipe()) {
            return current.pipeChannel() == neighbor.pipeChannel();
        }
        return true;
    }

    private boolean isUsableNode(MechanismBlockData data) {
        if (config.allowUnloadedChunks()) {
            return true;
        }
        return chunkLoadChecker.isLoaded(data);
    }

    private static boolean isBukkitChunkLoaded(MechanismBlockData data) {
        return data.world()
            .map(world -> world.isChunkLoaded(data.x() >> 4, data.z() >> 4))
            .orElse(false);
    }

    @FunctionalInterface
    interface ChunkLoadChecker {
        boolean isLoaded(MechanismBlockData data);
    }
}
