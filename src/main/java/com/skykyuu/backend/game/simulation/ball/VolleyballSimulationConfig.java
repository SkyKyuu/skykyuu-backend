package com.skykyuu.backend.game.simulation.ball;

public final class VolleyballSimulationConfig {

    public static final double GRAVITY_METERS_PER_SECOND_SQUARED = 9.81;
    public static final int FIXED_HZ = 60;
    public static final double FIXED_STEP_SECONDS = 1.0 / FIXED_HZ;
    public static final double MAX_FRAME_DELTA_SECONDS = 0.1;
    public static final int MAX_SUB_STEPS = 8;

    private VolleyballSimulationConfig() {
    }
}
