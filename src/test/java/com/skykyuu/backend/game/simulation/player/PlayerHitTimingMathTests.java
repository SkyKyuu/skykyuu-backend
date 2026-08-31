package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.VolleyballSimulationConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerHitTimingMathTests {

    @Test
    void createsNegativeOffsetForAnEarlyHit() {
        PlayerHitTimingSample sample = PlayerHitTimingMath.create(
                10L,
                11L,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );

        assertEquals(-1L, sample.offsetSteps());
        assertEquals(-VolleyballSimulationConfig.FIXED_STEP_SECONDS,
                sample.offsetSeconds());
    }

    @Test
    void createsZeroOffsetForAHitOnTheContactEntryStep() {
        PlayerHitTimingSample sample = PlayerHitTimingMath.create(
                10L,
                10L,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );

        assertEquals(0L, sample.offsetSteps());
        assertEquals(0.0, sample.offsetSeconds());
    }

    @Test
    void createsPositiveOffsetForALateHit() {
        PlayerHitTimingSample sample = PlayerHitTimingMath.create(
                12L,
                10L,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );

        assertEquals(2L, sample.offsetSteps());
        assertEquals(2.0 * VolleyballSimulationConfig.FIXED_STEP_SECONDS,
                sample.offsetSeconds());
    }
}
