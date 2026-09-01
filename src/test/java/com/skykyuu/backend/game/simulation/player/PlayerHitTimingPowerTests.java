package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerHitTimingPowerTests {

    @ParameterizedTest
    @EnumSource(PlayerHitTimingGrade.class)
    void mapsEveryTimingGradeToItsForwardMultiplier(PlayerHitTimingGrade grade) {
        double expectedMultiplier = switch (grade) {
            case VERY_EARLY, VERY_LATE -> 0.75;
            case EARLY, LATE -> 0.90;
            case PERFECT -> 1.00;
        };

        assertEquals(expectedMultiplier,
                PlayerHitTimingPower.getForwardMultiplier(grade));
    }

    @Test
    void rejectsNullGrade() {
        assertThrows(
                NullPointerException.class,
                () -> PlayerHitTimingPower.getForwardMultiplier(null)
        );
    }
}
