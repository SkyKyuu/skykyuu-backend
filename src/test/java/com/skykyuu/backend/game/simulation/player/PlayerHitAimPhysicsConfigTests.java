package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerHitAimPhysicsConfigTests {

    @Test
    void exposesInitialMaximumLateralVelocityContribution() {
        assertEquals(
                3.0,
                PlayerHitAimPhysicsConfig
                        .MAX_LATERAL_VELOCITY_CONTRIBUTION_METERS_PER_SECOND
        );
    }
}
