package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class NetworkGraph {
    private final Map<String, MechanismNetwork> networksById;
    private final Map<BlockKey, String> networkIdByNode;
    private final long revision;

    public NetworkGraph(Collection<MechanismNetwork> networks, long revision) {
        this.networksById = new HashMap<>();
        this.networkIdByNode = new HashMap<>();
        this.revision = revision;
        for (MechanismNetwork network : networks) {
            networksById.put(network.id(), network);
            for (BlockKey node : network.nodes()) {
                networkIdByNode.put(node, network.id());
            }
        }
    }

    public Optional<MechanismNetwork> networkFor(BlockKey key) {
        String id = networkIdByNode.get(key);
        return id == null ? Optional.empty() : Optional.ofNullable(networksById.get(id));
    }

    public Collection<MechanismNetwork> networks() {
        return networksById.values();
    }

    public int count() {
        return networksById.size();
    }

    public long revision() {
        return revision;
    }
}
