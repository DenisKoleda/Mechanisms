package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismIoSide;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.PipeChannel;
import com.denis.mechanisms.block.RedstoneMode;
import com.denis.mechanisms.block.TrashMode;
import com.denis.mechanisms.block.UpgradeModules;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteSearchTest {
    private final UUID worldId = UUID.randomUUID();
    private final FilterSettings pass = new FilterSettings(FilterMode.WHITELIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of());
    private final RouteSearch routeSearch = new RouteSearch(new FilterService());

    @Test
    void routerFilterBlocksNonMatchingItems() {
        MechanismBlockData extractor = block(MechanismBlockType.EXTRACTOR, 0, pass);
        MechanismBlockData pipe = block(MechanismBlockType.PIPE, 1, pass);
        FilterSettings ironOnly = new FilterSettings(FilterMode.WHITELIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of(TestStacks.stack(Material.IRON_INGOT)));
        MechanismBlockData router = block(MechanismBlockType.ROUTER, 2, ironOnly);
        MechanismBlockData inserter = block(MechanismBlockType.INSERTER, 3, pass);

        TestGraph graph = new TestGraph(extractor, pipe, router, inserter);
        graph.connect(extractor, pipe);
        graph.connect(pipe, router);
        graph.connect(router, inserter);

        assertEquals(1, routeSearch.findRoutes(graph.network(), extractor, TestStacks.stack(Material.IRON_INGOT), 128, graph::neighbors).size());
        assertTrue(routeSearch.findRoutes(graph.network(), extractor, TestStacks.stack(Material.GOLD_INGOT), 128, graph::neighbors).isEmpty());
    }

    @Test
    void destinationPrioritySortsBeforePathLength() {
        MechanismBlockData extractor = block(MechanismBlockType.EXTRACTOR, 0, pass);
        MechanismBlockData pipe = block(MechanismBlockType.PIPE, 1, pass);
        MechanismBlockData lowPriority = block(MechanismBlockType.INSERTER, 2, pass.withPriority(0));
        MechanismBlockData detour = block(MechanismBlockType.PIPE, 3, pass);
        MechanismBlockData highPriority = block(MechanismBlockType.INSERTER, 4, pass.withPriority(10));

        TestGraph graph = new TestGraph(extractor, pipe, lowPriority, detour, highPriority);
        graph.connect(extractor, pipe);
        graph.connect(pipe, lowPriority);
        graph.connect(pipe, detour);
        graph.connect(detour, highPriority);

        List<Route> routes = routeSearch.findRoutes(graph.network(), extractor, TestStacks.stack(Material.IRON_INGOT), 128, graph::neighbors);

        assertEquals(highPriority.key(), routes.getFirst().destination().key());
        assertEquals(10, routes.getFirst().priority());
    }

    @Test
    void maxRouteLengthStopsSearch() {
        MechanismBlockData extractor = block(MechanismBlockType.EXTRACTOR, 0, pass);
        MechanismBlockData pipe = block(MechanismBlockType.PIPE, 1, pass);
        MechanismBlockData inserter = block(MechanismBlockType.INSERTER, 2, pass);

        TestGraph graph = new TestGraph(extractor, pipe, inserter);
        graph.connect(extractor, pipe);
        graph.connect(pipe, inserter);

        assertTrue(routeSearch.findRoutes(graph.network(), extractor, TestStacks.stack(Material.IRON_INGOT), 1, graph::neighbors).isEmpty());
    }

    @Test
    void reachableInserterDiagnosticIgnoresFilters() {
        MechanismBlockData extractor = block(MechanismBlockType.EXTRACTOR, 0, pass);
        FilterSettings ironOnly = new FilterSettings(FilterMode.WHITELIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of(TestStacks.stack(Material.IRON_INGOT)));
        MechanismBlockData router = block(MechanismBlockType.ROUTER, 1, ironOnly);
        MechanismBlockData inserter = block(MechanismBlockType.INSERTER, 2, ironOnly);

        TestGraph graph = new TestGraph(extractor, router, inserter);
        graph.connect(extractor, router);
        graph.connect(router, inserter);

        assertTrue(routeSearch.findRoutes(graph.network(), extractor, TestStacks.stack(Material.GOLD_INGOT), 128, graph::neighbors).isEmpty());
        assertTrue(routeSearch.hasReachableInserter(graph.network(), extractor, 128, graph::neighbors));
    }

    private MechanismBlockData block(MechanismBlockType type, int x, FilterSettings filter) {
        return new MechanismBlockData(UUID.randomUUID(), type, worldId, "world", x, 64, 0, Material.STONE, filter, MechanismIoSide.AUTO, RedstoneMode.IGNORE, TrashMode.DISABLED, PipeChannel.DEFAULT, UpgradeModules.EMPTY, 0L);
    }

    private static final class TestGraph {
        private final Map<BlockKey, MechanismBlockData> nodes = new HashMap<>();
        private final Map<BlockKey, List<BlockKey>> edges = new HashMap<>();

        private TestGraph(MechanismBlockData... data) {
            for (MechanismBlockData node : data) {
                nodes.put(node.key(), node);
                edges.put(node.key(), new java.util.ArrayList<>());
            }
        }

        private void connect(MechanismBlockData first, MechanismBlockData second) {
            edges.get(first.key()).add(second.key());
            edges.get(second.key()).add(first.key());
        }

        private List<MechanismBlockData> neighbors(BlockKey key) {
            return edges.getOrDefault(key, List.of()).stream().map(nodes::get).toList();
        }

        private MechanismNetwork network() {
            return new MechanismNetwork("test", Set.copyOf(nodes.keySet()), false);
        }
    }
}
