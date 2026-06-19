package com.denis.mechanisms.block;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MechanismBlockTypeTest {
    @Test
    void pipeTokensAndAliasesResolveToTiers() {
        assertEquals(MechanismBlockType.PIPE, MechanismBlockType.fromToken("pipe").orElseThrow());
        assertEquals(MechanismBlockType.PIPE_FAST, MechanismBlockType.fromToken("pipe2").orElseThrow());
        assertEquals(MechanismBlockType.PIPE_FAST, MechanismBlockType.fromToken("fast-pipe").orElseThrow());
        assertEquals(MechanismBlockType.PIPE_EXPRESS, MechanismBlockType.fromToken("pipe3").orElseThrow());
        assertEquals(MechanismBlockType.PIPE_EXPRESS, MechanismBlockType.fromToken("express-pipe").orElseThrow());
    }

    @Test
    void onlyPipeTypesReportPipeTier() {
        assertTrue(MechanismBlockType.PIPE.isPipe());
        assertTrue(MechanismBlockType.PIPE_FAST.isPipe());
        assertTrue(MechanismBlockType.PIPE_EXPRESS.isPipe());
        assertFalse(MechanismBlockType.EXTRACTOR.isPipe());
        assertFalse(MechanismBlockType.ROUTER.isPipe());
        assertFalse(MechanismBlockType.INSERTER.isPipe());
    }
}
