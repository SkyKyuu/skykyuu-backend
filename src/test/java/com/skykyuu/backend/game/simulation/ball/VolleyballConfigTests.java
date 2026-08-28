package com.skykyuu.backend.game.simulation.ball;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VolleyballConfigTests {

    private static final double TOLERANCE = 1.0e-12;

    @Test
    void derivesDiameterFromRadius() {
        assertEquals(0.105, VolleyballConfig.RADIUS_METERS, TOLERANCE);
        assertEquals(
                VolleyballConfig.RADIUS_METERS * 2.0,
                VolleyballConfig.DIAMETER_METERS,
                TOLERANCE
        );
        assertEquals(0.210, VolleyballConfig.DIAMETER_METERS, TOLERANCE);
        assertEquals(0.270, VolleyballConfig.MASS_KG, TOLERANCE);
    }

    @Test
    void exposesFrontendSimulationConstants() {
        assertEquals(9.81,
                VolleyballSimulationConfig.GRAVITY_METERS_PER_SECOND_SQUARED,
                TOLERANCE);
        assertEquals(60, VolleyballSimulationConfig.FIXED_HZ);
        assertEquals(1.0 / 60.0,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS,
                TOLERANCE);
        assertEquals(0.1,
                VolleyballSimulationConfig.MAX_FRAME_DELTA_SECONDS,
                TOLERANCE);
        assertEquals(8, VolleyballSimulationConfig.MAX_SUB_STEPS);
    }
}
