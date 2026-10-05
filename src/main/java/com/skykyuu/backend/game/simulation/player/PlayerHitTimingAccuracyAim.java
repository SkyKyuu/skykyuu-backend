package com.skykyuu.backend.game.simulation.player;

public final class PlayerHitTimingAccuracyAim {

    private PlayerHitTimingAccuracyAim() {
    }

    public static double getEffectiveAimLateral(double aimLateral, double accuracyMultiplier) {
        return applyTimingAccuracy(PlayerHitAim.validateLateral(aimLateral), accuracyMultiplier);
    }

    public static double getEffectiveAimForward(double aimForward, double accuracyMultiplier) {
        return applyTimingAccuracy(PlayerHitAim.validateForward(aimForward), accuracyMultiplier);
    }

    private static double applyTimingAccuracy(double validatedAim, double accuracyMultiplier) {
        if (!Double.isFinite(accuracyMultiplier)
                || accuracyMultiplier <= 0.0 || accuracyMultiplier > 1.0) {
            throw new IllegalArgumentException(
                    "Hit timing accuracy multiplier must be finite, greater than 0, and at most 1: "
                            + accuracyMultiplier
            );
        }
        return validatedAim * accuracyMultiplier;
    }
}
