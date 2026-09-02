package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerHitTimingAccuracyAimTests {

    @ParameterizedTest(name = "raw aim {0} with accuracy {1} becomes {2}")
    @MethodSource("effectiveAimCases")
    void scalesValidatedRawAimWithoutClampingOrRounding(
            double rawAim,
            double accuracyMultiplier,
            double expectedEffectiveAim
    ) {
        assertEquals(
                expectedEffectiveAim,
                PlayerHitTimingAccuracyAim.getEffectiveAimLateral(
                        rawAim,
                        accuracyMultiplier
                )
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY,
            0.0,
            -0.1,
            1.01
    })
    void rejectsInvalidAccuracyMultiplier(double accuracyMultiplier) {
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerHitTimingAccuracyAim.getEffectiveAimLateral(
                        1.0,
                        accuracyMultiplier
                )
        );
    }

    private static Stream<Arguments> effectiveAimCases() {
        return Stream.of(
                Arguments.of(1.0, 1.0, 1.0),
                Arguments.of(1.0, 0.85, 0.85),
                Arguments.of(1.0, 0.60, 0.60),
                Arguments.of(-1.0, 0.85, -0.85),
                Arguments.of(0.5, 0.85, 0.425),
                Arguments.of(0.0, 0.60, 0.0)
        );
    }
}
