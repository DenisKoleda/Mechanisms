package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;

import java.util.Set;

public record MechanismNetwork(
    String id,
    Set<BlockKey> nodes,
    boolean tooLarge
) {
    public int size() {
        return nodes.size();
    }
}
