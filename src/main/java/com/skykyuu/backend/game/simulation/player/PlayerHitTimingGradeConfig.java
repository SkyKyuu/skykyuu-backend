package com.skykyuu.backend.game.simulation.player;

public final class PlayerHitTimingGradeConfig {

    public static final long VERY_EARLY_MAX_OFFSET_STEPS = -4L;
    public static final long EARLY_MIN_OFFSET_STEPS = -3L;
    public static final long EARLY_MAX_OFFSET_STEPS = -1L;
    public static final long PERFECT_OFFSET_STEPS = 0L;
    public static final long LATE_MIN_OFFSET_STEPS = 1L;
    public static final long LATE_MAX_OFFSET_STEPS = 3L;
    public static final long VERY_LATE_MIN_OFFSET_STEPS = 4L;

    private PlayerHitTimingGradeConfig() {
    }
}
