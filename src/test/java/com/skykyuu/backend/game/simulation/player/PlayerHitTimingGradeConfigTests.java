package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerHitTimingGradeConfigTests {

    @Test
    void definesOfficialTimingGradeBoundaries() {
        assertAll(
                () -> assertEquals(-4L,
                        PlayerHitTimingGradeConfig.VERY_EARLY_MAX_OFFSET_STEPS),
                () -> assertEquals(-3L,
                        PlayerHitTimingGradeConfig.EARLY_MIN_OFFSET_STEPS),
                () -> assertEquals(-1L,
                        PlayerHitTimingGradeConfig.EARLY_MAX_OFFSET_STEPS),
                () -> assertEquals(0L,
                        PlayerHitTimingGradeConfig.PERFECT_OFFSET_STEPS),
                () -> assertEquals(1L,
                        PlayerHitTimingGradeConfig.LATE_MIN_OFFSET_STEPS),
                () -> assertEquals(3L,
                        PlayerHitTimingGradeConfig.LATE_MAX_OFFSET_STEPS),
                () -> assertEquals(4L,
                        PlayerHitTimingGradeConfig.VERY_LATE_MIN_OFFSET_STEPS)
        );
    }
}
