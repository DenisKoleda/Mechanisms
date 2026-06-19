package com.denis.mechanisms.listener;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InteractionPolicyTest {
    @Test
    void wandShowsStatusAndSneakWandOpensFilterWhenSupported() {
        assertEquals(InteractionPolicy.Decision.SHOW_STATUS, InteractionPolicy.decide(true, false, false, true));
        assertEquals(InteractionPolicy.Decision.OPEN_FILTER, InteractionPolicy.decide(true, true, false, true));
        assertEquals(InteractionPolicy.Decision.SHOW_STATUS, InteractionPolicy.decide(true, true, false, false));
    }

    @Test
    void blockInHandOnlyPassesThroughWhenSneaking() {
        assertEquals(InteractionPolicy.Decision.SHOW_HINT, InteractionPolicy.decide(false, false, true, true));
        assertEquals(InteractionPolicy.Decision.PASS_THROUGH, InteractionPolicy.decide(false, true, true, true));
    }

    @Test
    void emptyHandUsesHintOrQuickInteraction() {
        assertEquals(InteractionPolicy.Decision.SHOW_HINT, InteractionPolicy.decide(false, false, false, true));
        assertEquals(InteractionPolicy.Decision.OPEN_FILTER, InteractionPolicy.decide(false, true, false, true));
        assertEquals(InteractionPolicy.Decision.SHOW_STATUS, InteractionPolicy.decide(false, true, false, false));
    }
}
