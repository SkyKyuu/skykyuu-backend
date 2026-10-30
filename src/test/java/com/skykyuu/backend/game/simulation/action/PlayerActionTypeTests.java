package com.skykyuu.backend.game.simulation.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerActionTypeTests {

    @Test
    void containsOnlyGenericHitMatchingF31() {
        assertArrayEquals(new PlayerActionType[]{PlayerActionType.GENERIC_HIT}, PlayerActionType.values());
        assertEquals("GENERIC_HIT", PlayerActionType.GENERIC_HIT.name());
    }
}
