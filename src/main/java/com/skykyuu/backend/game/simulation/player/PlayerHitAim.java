package com.skykyuu.backend.game.simulation.player;

public final class PlayerHitAim {

    private PlayerHitAim() {
    }

    /**
     * Validates a player-local lateral aim where -1 is the player's left and
     * +1 is the player's right. No world-axis conversion is performed.
     */
    public static double validateLateral(double value) {
        if (!Double.isFinite(value) || value < -1.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    "aimLateral must be finite and between -1.0 and 1.0: " + value
            );
        }
        return value;
    }
}
