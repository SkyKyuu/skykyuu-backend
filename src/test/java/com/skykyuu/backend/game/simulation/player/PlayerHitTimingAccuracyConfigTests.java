package com.skykyuu.backend.game.simulation.player;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHitTimingAccuracyConfigTests {

    @Test
    void exposesInitialTimingAccuracyMultipliers() {
        assertEquals(0.60,
                PlayerHitTimingAccuracyConfig.VERY_EARLY_ACCURACY_MULTIPLIER);
        assertEquals(0.85,
                PlayerHitTimingAccuracyConfig.EARLY_ACCURACY_MULTIPLIER);
        assertEquals(1.00,
                PlayerHitTimingAccuracyConfig.PERFECT_ACCURACY_MULTIPLIER);
        assertEquals(0.85,
                PlayerHitTimingAccuracyConfig.LATE_ACCURACY_MULTIPLIER);
        assertEquals(0.60,
                PlayerHitTimingAccuracyConfig.VERY_LATE_ACCURACY_MULTIPLIER);
    }

    @ParameterizedTest
    @MethodSource("configuredMultipliers")
    void everyAccuracyMultiplierIsFiniteAndWithinContractRange(double multiplier) {
        assertTrue(Double.isFinite(multiplier));
        assertTrue(multiplier > 0.0);
        assertTrue(multiplier <= 1.0);
    }

    private static Stream<Double> configuredMultipliers() {
        return Stream.of(
                PlayerHitTimingAccuracyConfig.VERY_EARLY_ACCURACY_MULTIPLIER,
                PlayerHitTimingAccuracyConfig.EARLY_ACCURACY_MULTIPLIER,
                PlayerHitTimingAccuracyConfig.PERFECT_ACCURACY_MULTIPLIER,
                PlayerHitTimingAccuracyConfig.LATE_ACCURACY_MULTIPLIER,
                PlayerHitTimingAccuracyConfig.VERY_LATE_ACCURACY_MULTIPLIER
        );
    }
}
