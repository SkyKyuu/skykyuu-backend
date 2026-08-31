package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerHitTimingGradeClassifierTests {

    @ParameterizedTest(name = "offset {0} is {1}")
    @MethodSource("classificationCases")
    void classifiesOfficialTimingTable(
            long offsetSteps,
            PlayerHitTimingGrade expectedGrade
    ) {
        assertEquals(expectedGrade, PlayerHitTimingGradeClassifier.classify(offsetSteps));
    }

    private static Stream<Arguments> classificationCases() {
        return Stream.of(
                Arguments.of(-100L, PlayerHitTimingGrade.VERY_EARLY),
                Arguments.of(-5L, PlayerHitTimingGrade.VERY_EARLY),
                Arguments.of(-4L, PlayerHitTimingGrade.VERY_EARLY),
                Arguments.of(-3L, PlayerHitTimingGrade.EARLY),
                Arguments.of(-2L, PlayerHitTimingGrade.EARLY),
                Arguments.of(-1L, PlayerHitTimingGrade.EARLY),
                Arguments.of(0L, PlayerHitTimingGrade.PERFECT),
                Arguments.of(1L, PlayerHitTimingGrade.LATE),
                Arguments.of(2L, PlayerHitTimingGrade.LATE),
                Arguments.of(3L, PlayerHitTimingGrade.LATE),
                Arguments.of(4L, PlayerHitTimingGrade.VERY_LATE),
                Arguments.of(5L, PlayerHitTimingGrade.VERY_LATE),
                Arguments.of(100L, PlayerHitTimingGrade.VERY_LATE)
        );
    }
}
