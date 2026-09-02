package com.skykyuu.backend.game.simulation.player;

import java.util.Objects;

public final class PlayerHitTimingAccuracy {

    private PlayerHitTimingAccuracy() {
    }

    public static double getAccuracyMultiplier(PlayerHitTimingGrade grade) {
        Objects.requireNonNull(grade, "grade must not be null");
        return switch (grade) {
            case VERY_EARLY ->
                    PlayerHitTimingAccuracyConfig.VERY_EARLY_ACCURACY_MULTIPLIER;
            case EARLY -> PlayerHitTimingAccuracyConfig.EARLY_ACCURACY_MULTIPLIER;
            case PERFECT -> PlayerHitTimingAccuracyConfig.PERFECT_ACCURACY_MULTIPLIER;
            case LATE -> PlayerHitTimingAccuracyConfig.LATE_ACCURACY_MULTIPLIER;
            case VERY_LATE ->
                    PlayerHitTimingAccuracyConfig.VERY_LATE_ACCURACY_MULTIPLIER;
        };
    }
}
