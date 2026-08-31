package com.skykyuu.backend.game.simulation.input;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerHitBufferConfigTests {

    @Test
    void exposesTheInitialFrontendParityWindow() {
        assertEquals(0.1, PlayerHitBufferConfig.DURATION_SECONDS);
    }
}
