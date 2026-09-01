package com.skykyuu.backend.game.simulation.player;

import java.util.Objects;

public final class PlayerHitTimingPower {

    private PlayerHitTimingPower() {
    }

    public static double getForwardMultiplier(PlayerHitTimingGrade grade) {
        Objects.requireNonNull(grade, "grade must not be null");
        return switch (grade) {
            case VERY_EARLY -> PlayerHitTimingPowerConfig.VERY_EARLY_FORWARD_MULTIPLIER;
            case EARLY -> PlayerHitTimingPowerConfig.EARLY_FORWARD_MULTIPLIER;
            case PERFECT -> PlayerHitTimingPowerConfig.PERFECT_FORWARD_MULTIPLIER;
            case LATE -> PlayerHitTimingPowerConfig.LATE_FORWARD_MULTIPLIER;
            case VERY_LATE -> PlayerHitTimingPowerConfig.VERY_LATE_FORWARD_MULTIPLIER;
        };
    }
}
