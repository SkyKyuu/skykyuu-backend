package com.skykyuu.backend.game.simulation.player;

public final class PlayerHitTimingAccuracyAim {

    private PlayerHitTimingAccuracyAim() {
    }

    public static double getEffectiveAimLateral(
            double hitAimLateral,
            double hitTimingAccuracyMultiplier
    ) {
        double validatedAim = PlayerHitAim.validateLateral(hitAimLateral);
        if (!Double.isFinite(hitTimingAccuracyMultiplier)
                || hitTimingAccuracyMultiplier <= 0.0
                || hitTimingAccuracyMultiplier > 1.0) {
            throw new IllegalArgumentException(
                    "hitTimingAccuracyMultiplier must be finite, greater than 0.0, "
                            + "and at most 1.0: " + hitTimingAccuracyMultiplier
            );
        }
        return validatedAim * hitTimingAccuracyMultiplier;
    }
}
