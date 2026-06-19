package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismIoSide;
import com.denis.mechanisms.block.MechanismBlockData;
import com.denis.mechanisms.block.MechanismBlockRegistry;
import com.denis.mechanisms.block.MechanismBlockType;
import com.denis.mechanisms.block.PipeChannel;
import com.denis.mechanisms.block.RedstoneMode;
import com.denis.mechanisms.block.TrashMode;
import com.denis.mechanisms.block.UpgradeModules;
import com.denis.mechanisms.config.MechanismsConfig;
import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NetworkIndexerTest {
    @Test
    void networkOverConfiguredNodeLimitIsMarkedTooLarge() {
        UUID worldId = UUID.randomUUID();
        FilterSettings filter = new FilterSettings(FilterMode.WHITELIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of());
        List<MechanismBlockData> nodes = List.of(
            block(worldId, 0, filter),
            block(worldId, 1, filter),
            block(worldId, 2, filter),
            block(worldId, 3, filter),
            block(worldId, 4, filter)
        );
        Map<BlockKey, MechanismBlockData> byKey = nodes.stream().collect(Collectors.toMap(MechanismBlockData::key, Function.identity()));

        MechanismBlockRegistry registry = mock(MechanismBlockRegistry.class);
        MechanismsConfig config = mock(MechanismsConfig.class);
        when(registry.all()).thenReturn(nodes);
        when(registry.getRegistered(any(BlockKey.class))).thenAnswer(invocation -> Optional.ofNullable(byKey.get(invocation.getArgument(0))));
        when(config.maxNetworkNodes()).thenReturn(3);
        when(config.allowCrossChunk()).thenReturn(true);
        when(config.allowUnloadedChunks()).thenReturn(true);

        NetworkIndexer indexer = new NetworkIndexer(registry, config);
        indexer.rebuild();

        assertEquals(1, indexer.graph().count());
        MechanismNetwork network = indexer.graph().networks().iterator().next();
        assertEquals(5, network.size());
        assertTrue(network.tooLarge());
    }

    @Test
    void unloadedNodesAreExcludedWhenConfigDisallowsThem() {
        UUID worldId = UUID.randomUUID();
        FilterSettings filter = new FilterSettings(FilterMode.WHITELIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of());
        MechanismBlockData loaded = block(worldId, 0, filter);
        MechanismBlockData unloaded = block(worldId, 1, filter);
        List<MechanismBlockData> nodes = List.of(loaded, unloaded);
        Map<BlockKey, MechanismBlockData> byKey = nodes.stream().collect(Collectors.toMap(MechanismBlockData::key, Function.identity()));

        MechanismBlockRegistry registry = mock(MechanismBlockRegistry.class);
        MechanismsConfig config = mock(MechanismsConfig.class);
        when(registry.all()).thenReturn(nodes);
        when(registry.getRegistered(any(BlockKey.class))).thenAnswer(invocation -> Optional.ofNullable(byKey.get(invocation.getArgument(0))));
        when(config.maxNetworkNodes()).thenReturn(10);
        when(config.allowCrossChunk()).thenReturn(true);
        when(config.allowUnloadedChunks()).thenReturn(false);

        NetworkIndexer indexer = new NetworkIndexer(registry, config, data -> !data.key().equals(unloaded.key()));
        indexer.rebuild();

        assertTrue(indexer.networkFor(loaded.key()).isPresent());
        assertFalse(indexer.networkFor(unloaded.key()).isPresent());
        assertTrue(indexer.neighbors(loaded.key()).isEmpty());
    }

    @Test
    void adjacentPipesWithDifferentChannelsDoNotConnect() {
        UUID worldId = UUID.randomUUID();
        FilterSettings filter = new FilterSettings(FilterMode.WHITELIST, MatchMode.MATERIAL_ONLY, RouteMode.PRIORITY_FIRST, 0, List.of());
        MechanismBlockData red = block(worldId, 0, filter, PipeChannel.RED);
        MechanismBlockData blue = block(worldId, 1, filter, PipeChannel.BLUE);
        List<MechanismBlockData> nodes = List.of(red, blue);
        Map<BlockKey, MechanismBlockData> byKey = nodes.stream().collect(Collectors.toMap(MechanismBlockData::key, Function.identity()));

        MechanismBlockRegistry registry = mock(MechanismBlockRegistry.class);
        MechanismsConfig config = mock(MechanismsConfig.class);
        when(registry.all()).thenReturn(nodes);
        when(registry.getRegistered(any(BlockKey.class))).thenAnswer(invocation -> Optional.ofNullable(byKey.get(invocation.getArgument(0))));
        when(config.maxNetworkNodes()).thenReturn(10);
        when(config.allowCrossChunk()).thenReturn(true);
        when(config.allowUnloadedChunks()).thenReturn(true);

        NetworkIndexer indexer = new NetworkIndexer(registry, config);
        indexer.rebuild();

        assertEquals(2, indexer.graph().count());
        assertTrue(indexer.neighbors(red.key()).isEmpty());
    }

    private MechanismBlockData block(UUID worldId, int x, FilterSettings filter) {
        return block(worldId, x, filter, PipeChannel.DEFAULT);
    }

    private MechanismBlockData block(UUID worldId, int x, FilterSettings filter, PipeChannel channel) {
        return new MechanismBlockData(UUID.randomUUID(), MechanismBlockType.PIPE, worldId, "world", x, 64, 0, Material.GLASS, filter, MechanismIoSide.AUTO, RedstoneMode.IGNORE, TrashMode.DISABLED, channel, UpgradeModules.EMPTY, 0L);
    }
}
