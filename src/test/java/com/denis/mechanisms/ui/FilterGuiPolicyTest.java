package com.denis.mechanisms.ui;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterGuiPolicyTest {
    @Test
    void topInventoryClicksAreCancelled() {
        assertEquals(FilterGuiPolicy.ClickDecision.CANCEL, FilterGuiPolicy.decideClick(0, false, 54));
        assertEquals(FilterGuiPolicy.ClickDecision.CANCEL, FilterGuiPolicy.decideClick(53, true, 54));
    }

    @Test
    void bottomInventoryNormalClicksAreAllowedButShiftClicksAreCancelled() {
        assertEquals(FilterGuiPolicy.ClickDecision.ALLOW, FilterGuiPolicy.decideClick(54, false, 54));
        assertEquals(FilterGuiPolicy.ClickDecision.CANCEL, FilterGuiPolicy.decideClick(54, true, 54));
    }

    @Test
    void outsideClicksAreAllowed() {
        assertEquals(FilterGuiPolicy.ClickDecision.ALLOW, FilterGuiPolicy.decideClick(-999, false, 54));
    }

    @Test
    void dragsTouchingTopInventoryAreCancelled() {
        assertTrue(FilterGuiPolicy.shouldCancelDrag(Set.of(10, 60), 54));
        assertFalse(FilterGuiPolicy.shouldCancelDrag(Set.of(54, 55, 70), 54));
    }
}
