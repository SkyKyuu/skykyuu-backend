package com.skykyuu.backend.game.simulation.action;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerActionTypeTests {

    @Test
    void preservesGenericHitAndAddsForearmPass() {
        assertArrayEquals(
                new PlayerActionType[]{PlayerActionType.GENERIC_HIT, PlayerActionType.FOREARM_PASS},
                PlayerActionType.values()
        );
        assertEquals("GENERIC_HIT", PlayerActionType.GENERIC_HIT.name());
        assertEquals("FOREARM_PASS", PlayerActionType.FOREARM_PASS.name());
    }
}
