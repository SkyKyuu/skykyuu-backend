package com.skykyuu.backend.game.simulation.player;

public final class PlayerHitTimingGradeClassifier {

    private PlayerHitTimingGradeClassifier() {
    }

    public static PlayerHitTimingGrade classify(long offsetSteps) {
        if (offsetSteps <= PlayerHitTimingGradeConfig.VERY_EARLY_MAX_OFFSET_STEPS) {
            return PlayerHitTimingGrade.VERY_EARLY;
        }
        if (offsetSteps >= PlayerHitTimingGradeConfig.EARLY_MIN_OFFSET_STEPS
                && offsetSteps <= PlayerHitTimingGradeConfig.EARLY_MAX_OFFSET_STEPS) {
            return PlayerHitTimingGrade.EARLY;
        }
        if (offsetSteps == PlayerHitTimingGradeConfig.PERFECT_OFFSET_STEPS) {
            return PlayerHitTimingGrade.PERFECT;
        }
        if (offsetSteps >= PlayerHitTimingGradeConfig.LATE_MIN_OFFSET_STEPS
                && offsetSteps <= PlayerHitTimingGradeConfig.LATE_MAX_OFFSET_STEPS) {
            return PlayerHitTimingGrade.LATE;
        }
        if (offsetSteps >= PlayerHitTimingGradeConfig.VERY_LATE_MIN_OFFSET_STEPS) {
            return PlayerHitTimingGrade.VERY_LATE;
        }
        throw new IllegalStateException("Unclassifiable hit timing offsetSteps: " + offsetSteps);
    }
}
