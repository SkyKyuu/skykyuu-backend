package com.skykyuu.backend.game.simulation.player;

public final class PlayerHitTimingAccuracyConfig {

    /**
     * Temporary initial tuning for hit timing accuracy telemetry.
     */
    public static final double VERY_EARLY_ACCURACY_MULTIPLIER = 0.60;
    public static final double EARLY_ACCURACY_MULTIPLIER = 0.85;
    public static final double PERFECT_ACCURACY_MULTIPLIER = 1.00;
    public static final double LATE_ACCURACY_MULTIPLIER = 0.85;
    public static final double VERY_LATE_ACCURACY_MULTIPLIER = 0.60;

    private PlayerHitTimingAccuracyConfig() {
    }
}
