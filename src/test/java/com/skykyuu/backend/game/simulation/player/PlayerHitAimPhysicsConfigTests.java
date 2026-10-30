package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerHitAimPhysicsConfigTests {

    @Test
    void exposesFrozenMaximumForwardVelocityContribution() {
        assertEquals(2.0,
                PlayerHitAimPhysicsConfig.MAX_FORWARD_VELOCITY_CONTRIBUTION_METERS_PER_SECOND);
    }

    @Test
    void exposesInitialMaximumLateralVelocityContribution() {
        assertEquals(
                3.0,
                PlayerHitAimPhysicsConfig
                        .MAX_LATERAL_VELOCITY_CONTRIBUTION_METERS_PER_SECOND
        );
    }
}
