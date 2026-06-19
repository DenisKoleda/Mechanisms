package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class RouteSearch {
    private final FilterService filterService;

    public RouteSearch(FilterService filterService) {
        this.filterService = filterService;
    }

    public List<Route> findRoutes(
        MechanismNetwork network,
        MechanismBlockData source,
        ItemStack item,
        int maxRouteLength,
        NeighborProvider neighborProvider
    ) {
        if (network == null || network.tooLarge()) {
            return List.of();
        }

        List<Route> routes = new ArrayList<>();
        Queue<PathNode> queue = new ArrayDeque<>();
        Set<BlockKey> visited = new HashSet<>();
        queue.add(new PathNode(source.key(), List.of(source.key())));
        visited.add(source.key());

        while (!queue.isEmpty()) {
            PathNode current = queue.remove();
            if (current.path().size() - 1 >= maxRouteLength) {
                continue;
            }

            for (MechanismBlockData neighbor : neighborProvider.neighbors(current.key())) {
                BlockKey neighborKey = neighbor.key();
                if (!network.nodes().contains(neighborKey) || visited.contains(neighborKey)) {
                    continue;
                }

                if (neighbor.type() == MechanismBlockType.ROUTER && !filterService.allows(item, neighbor.filter())) {
                    continue;
                }

                List<BlockKey> nextPath = new ArrayList<>(current.path());
                nextPath.add(neighborKey);
                visited.add(neighborKey);

                if (neighbor.type().isDestination() && filterService.allows(item, neighbor.filter())) {
                    routes.add(new Route(network.id(), source, neighbor, List.copyOf(nextPath), neighbor.filter().priority()));
                }

                queue.add(new PathNode(neighborKey, List.copyOf(nextPath)));
            }
        }

        routes.sort(Comparator
            .comparingInt(Route::priority).reversed()
            .thenComparingInt(Route::length)
            .thenComparing(route -> route.destination().key()));
        return List.copyOf(routes);
    }

    public boolean hasReachableInserter(
        MechanismNetwork network,
        MechanismBlockData source,
        int maxRouteLength,
        NeighborProvider neighborProvider
    ) {
        if (network == null || network.tooLarge()) {
            return false;
        }

        Queue<PathNode> queue = new ArrayDeque<>();
        Set<BlockKey> visited = new HashSet<>();
        queue.add(new PathNode(source.key(), List.of(source.key())));
        visited.add(source.key());

        while (!queue.isEmpty()) {
            PathNode current = queue.remove();
            if (current.path().size() - 1 >= maxRouteLength) {
                continue;
            }

            for (MechanismBlockData neighbor : neighborProvider.neighbors(current.key())) {
                BlockKey neighborKey = neighbor.key();
                if (!network.nodes().contains(neighborKey) || visited.contains(neighborKey)) {
                    continue;
                }

                if (neighbor.type().isDestination()) {
                    return true;
                }

                List<BlockKey> nextPath = new ArrayList<>(current.path());
                nextPath.add(neighborKey);
                visited.add(neighborKey);
                queue.add(new PathNode(neighborKey, List.copyOf(nextPath)));
            }
        }

        return false;
    }

    public interface NeighborProvider {
        List<MechanismBlockData> neighbors(BlockKey key);
    }

    private record PathNode(BlockKey key, List<BlockKey> path) {
    }
}
