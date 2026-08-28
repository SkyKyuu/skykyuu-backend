package com.skykyuu.backend.game.simulation.ball;

import java.util.Objects;

public final class FixedStepVolleyballSimulator {

    private static final double ACCUMULATOR_EPSILON_SECONDS = 1.0e-12;

    private VolleyballState state;
    private double accumulatorSeconds;
    private long totalSimulationSteps;

    public FixedStepVolleyballSimulator(VolleyballState initialState) {
        state = Objects.requireNonNull(initialState, "initialState must not be null");
    }

    public int advance(double frameDeltaSeconds) {
        if (!Double.isFinite(frameDeltaSeconds) || frameDeltaSeconds <= 0.0) {
            return 0;
        }

        double cappedDeltaSeconds = Math.min(
                frameDeltaSeconds,
                VolleyballSimulationConfig.MAX_FRAME_DELTA_SECONDS
        );
        accumulatorSeconds += cappedDeltaSeconds;

        int stepsExecuted = 0;
        while (stepsExecuted < VolleyballSimulationConfig.MAX_SUB_STEPS
                && accumulatorSeconds + ACCUMULATOR_EPSILON_SECONDS
                >= VolleyballSimulationConfig.FIXED_STEP_SECONDS) {
            state = VolleyballSimulationMath.stepFreeFlight(
                    state,
                    VolleyballSimulationConfig.FIXED_STEP_SECONDS
            );
            accumulatorSeconds -= VolleyballSimulationConfig.FIXED_STEP_SECONDS;
            if (accumulatorSeconds < 0.0
                    && accumulatorSeconds > -ACCUMULATOR_EPSILON_SECONDS) {
                accumulatorSeconds = 0.0;
            }
            stepsExecuted++;
            totalSimulationSteps++;
        }

        return stepsExecuted;
    }

    public VolleyballState getState() {
        return state;
    }

    public double getAccumulatorSeconds() {
        return accumulatorSeconds;
    }

    public long getTotalSimulationSteps() {
        return totalSimulationSteps;
    }

    public void reset(VolleyballState newState) {
        state = Objects.requireNonNull(newState, "newState must not be null");
        accumulatorSeconds = 0.0;
        totalSimulationSteps = 0L;
    }
}
