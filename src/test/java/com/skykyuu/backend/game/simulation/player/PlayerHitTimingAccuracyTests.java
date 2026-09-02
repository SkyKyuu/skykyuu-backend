package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerHitTimingAccuracyTests {

    @ParameterizedTest(name = "{0} has accuracy multiplier {1}")
    @MethodSource("accuracyCases")
    void mapsEveryGradeToAccuracyMultiplier(
            PlayerHitTimingGrade grade,
            double expectedMultiplier
    ) {
        assertEquals(
                expectedMultiplier,
                PlayerHitTimingAccuracy.getAccuracyMultiplier(grade)
        );
    }

    @Test
    void rejectsNullGrade() {
        assertThrows(
                NullPointerException.class,
                () -> PlayerHitTimingAccuracy.getAccuracyMultiplier(null)
        );
    }

    private static Stream<Arguments> accuracyCases() {
        return Stream.of(
                Arguments.of(PlayerHitTimingGrade.VERY_EARLY, 0.60),
                Arguments.of(PlayerHitTimingGrade.EARLY, 0.85),
                Arguments.of(PlayerHitTimingGrade.PERFECT, 1.00),
                Arguments.of(PlayerHitTimingGrade.LATE, 0.85),
                Arguments.of(PlayerHitTimingGrade.VERY_LATE, 0.60)
        );
    }
}
