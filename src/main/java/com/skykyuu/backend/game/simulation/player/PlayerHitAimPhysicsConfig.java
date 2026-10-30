package com.skykyuu.backend.game.simulation.player;

public final class PlayerHitAimPhysicsConfig {

    /**
     * Temporary initial tuning for the maximum lateral velocity contributed by aim.
     */
    public static final double MAX_LATERAL_VELOCITY_CONTRIBUTION_METERS_PER_SECOND = 3.0;

    /** Maximum accuracy-adjusted forward velocity contribution, matching F2.18. */
    public static final double MAX_FORWARD_VELOCITY_CONTRIBUTION_METERS_PER_SECOND = 2.0;

    private PlayerHitAimPhysicsConfig() {
    }
}
