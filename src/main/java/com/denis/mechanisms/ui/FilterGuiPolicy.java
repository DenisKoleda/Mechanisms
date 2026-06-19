package com.denis.mechanisms.ui;

import java.util.Set;

public final class FilterGuiPolicy {
    private FilterGuiPolicy() {
    }

    public static ClickDecision decideClick(int rawSlot, boolean shiftClick, int topInventorySize) {
        if (rawSlot < 0) {
            return ClickDecision.ALLOW;
        }
        if (rawSlot < topInventorySize) {
            return ClickDecision.CANCEL;
        }
        return shiftClick ? ClickDecision.CANCEL : ClickDecision.ALLOW;
    }

    public static boolean shouldCancelDrag(Set<Integer> rawSlots, int topInventorySize) {
        return rawSlots.stream().anyMatch(slot -> slot >= 0 && slot < topInventorySize);
    }

    public enum ClickDecision {
        ALLOW,
        CANCEL
    }
}
