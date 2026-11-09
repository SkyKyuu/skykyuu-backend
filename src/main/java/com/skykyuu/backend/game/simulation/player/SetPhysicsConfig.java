package com.skykyuu.backend.game.simulation.player;

public final class SetPhysicsConfig {

    public static final double UPWARD_VELOCITY_METERS_PER_SECOND = 7.0;
    public static final double FORWARD_VELOCITY_METERS_PER_SECOND = 0.8;
    public static final double MAX_LATERAL_VELOCITY_CONTRIBUTION = 1.6;
    public static final double MAX_FORWARD_VELOCITY_CONTRIBUTION = 1.2;

    public static final double INCOMING_LATERAL_RETENTION = 0.25;

    private SetPhysicsConfig() {
    }
}
