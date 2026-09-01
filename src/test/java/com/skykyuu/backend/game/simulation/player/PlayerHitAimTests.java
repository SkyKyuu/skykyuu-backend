package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerHitAimTests {

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, -0.5, 0.0, 0.375, 0.5, 1.0})
    void acceptsFinitePlayerLocalAimWithinInclusiveRange(double aimLateral) {
        assertEquals(aimLateral, PlayerHitAim.validateLateral(aimLateral));
    }

    @ParameterizedTest
    @ValueSource(doubles = {
            -1.01,
            1.01,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    void rejectsOutOfRangeOrNonFiniteAim(double aimLateral) {
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerHitAim.validateLateral(aimLateral)
        );
    }
}
