package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerHitTimingPowerConfigTests {

    @Test
    void exposesExactForwardPowerMultipliers() {
        assertAll(
                () -> assertEquals(0.75,
                        PlayerHitTimingPowerConfig.VERY_EARLY_FORWARD_MULTIPLIER),
                () -> assertEquals(0.90,
                        PlayerHitTimingPowerConfig.EARLY_FORWARD_MULTIPLIER),
                () -> assertEquals(1.00,
                        PlayerHitTimingPowerConfig.PERFECT_FORWARD_MULTIPLIER),
                () -> assertEquals(0.90,
                        PlayerHitTimingPowerConfig.LATE_FORWARD_MULTIPLIER),
                () -> assertEquals(0.75,
                        PlayerHitTimingPowerConfig.VERY_LATE_FORWARD_MULTIPLIER)
        );
    }
}
