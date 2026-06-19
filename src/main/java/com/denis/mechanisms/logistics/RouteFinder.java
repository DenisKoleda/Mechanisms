package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.config.MechanismsConfig;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RouteFinder {
    private final MechanismBlockRegistry registry;
    private final NetworkIndexer indexer;
    private final MechanismsConfig config;
    private final RouteSearch routeSearch;
    private final Map<RouteCacheKey, List<Route>> routeCache = new LinkedHashMap<>();

    public RouteFinder(
        MechanismBlockRegistry registry,
        NetworkIndexer indexer,
        MechanismsConfig config,
        FilterService filterService
    ) {
        this.registry = registry;
        this.indexer = indexer;
        this.config = config;
        this.routeSearch = new RouteSearch(filterService);
    }

    public synchronized void clearCache() {
        routeCache.clear();
    }

    public synchronized List<Route> findRoutes(MechanismBlockData source, ItemStack item) {
        NetworkGraph graph = indexer.graph();
        RouteCacheKey cacheKey = new RouteCacheKey(source.key(), itemSignature(item), registry.version(), graph.revision());
        List<Route> cached = routeCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        List<Route> routes = computeRoutes(graph, source, item);
        routeCache.clear();
        routeCache.put(cacheKey, routes);
        return routes;
    }

    public synchronized String missReason(MechanismBlockData source) {
        NetworkGraph graph = indexer.graph();
        MechanismNetwork network = graph.networkFor(source.key()).orElse(null);
        if (network == null) {
            return "no_network";
        }
        if (network.tooLarge()) {
            return "network_too_large";
        }
        if (!routeSearch.hasReachableInserter(network, source, config.routeLengthFor(source), indexer::neighbors)) {
            return "no_destination";
        }
        return "filtered";
    }

    private List<Route> computeRoutes(NetworkGraph graph, MechanismBlockData source, ItemStack item) {
        MechanismNetwork network = graph.networkFor(source.key()).orElse(null);
        return routeSearch.findRoutes(network, source, item, config.routeLengthFor(source), indexer::neighbors);
    }

    private String itemSignature(ItemStack item) {
        if (item == null) {
            return "air";
        }
        Map<String, Object> serialized = new HashMap<>(item.serialize());
        serialized.remove("amount");
        return serialized.toString();
    }

    private record RouteCacheKey(BlockKey source, String itemSignature, long registryVersion, long graphRevision) {
    }
}
