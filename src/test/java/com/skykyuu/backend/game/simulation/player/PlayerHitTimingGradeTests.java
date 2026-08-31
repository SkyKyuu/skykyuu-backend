package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class PlayerHitTimingGradeTests {

    @Test
    void exposesExactlyTheOfficialTimingGrades() {
        assertArrayEquals(
                new PlayerHitTimingGrade[]{
                        PlayerHitTimingGrade.VERY_EARLY,
                        PlayerHitTimingGrade.EARLY,
                        PlayerHitTimingGrade.PERFECT,
                        PlayerHitTimingGrade.LATE,
                        PlayerHitTimingGrade.VERY_LATE
                },
                PlayerHitTimingGrade.values()
        );
    }
}
