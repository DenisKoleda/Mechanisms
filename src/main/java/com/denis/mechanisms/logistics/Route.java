package com.denis.mechanisms.logistics;

import com.denis.mechanisms.block.BlockKey;
import com.denis.mechanisms.block.MechanismBlockData;

import java.util.List;

public record Route(
    String networkId,
    MechanismBlockData source,
    MechanismBlockData destination,
    List<BlockKey> path,
    int priority
) {
    public int length() {
        return Math.max(0, path.size() - 1);
    }
}
