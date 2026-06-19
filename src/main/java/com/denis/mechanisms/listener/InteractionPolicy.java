package com.denis.mechanisms.listener;

public final class InteractionPolicy {
    private InteractionPolicy() {
    }

    public static Decision decide(boolean wand, boolean sneaking, boolean placeableBlockInHand, boolean supportsFilter) {
        if (wand) {
            return sneaking && supportsFilter ? Decision.OPEN_FILTER : Decision.SHOW_STATUS;
        }
        if (placeableBlockInHand) {
            return sneaking ? Decision.PASS_THROUGH : Decision.SHOW_HINT;
        }
        if (sneaking) {
            return supportsFilter ? Decision.OPEN_FILTER : Decision.SHOW_STATUS;
        }
        return Decision.SHOW_HINT;
    }

    public enum Decision {
        PASS_THROUGH,
        SHOW_HINT,
        SHOW_STATUS,
        OPEN_FILTER
    }
}
