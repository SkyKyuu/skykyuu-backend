package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.court.CourtResult;
import com.skykyuu.backend.game.court.CourtSide;
import com.skykyuu.backend.game.court.IndoorCourtClassifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

public final class FixedStepVolleyballSimulator {

    private static final double ACCUMULATOR_EPSILON_SECONDS = 1.0e-12;

    private VolleyballState state;
    private double accumulatorSeconds;
    private long totalSimulationSteps;
    private boolean groundContactOccurred;

    public FixedStepVolleyballSimulator(VolleyballState initialState) {
        state = Objects.requireNonNull(initialState, "initialState must not be null");
    }

    public BallSimulationAdvanceResult advance(double frameDeltaSeconds) {
        if (groundContactOccurred
                || !Double.isFinite(frameDeltaSeconds)
                || frameDeltaSeconds <= 0.0) {
            return BallSimulationAdvanceResult.empty();
        }

        double cappedDeltaSeconds = Math.min(
                frameDeltaSeconds,
                VolleyballSimulationConfig.MAX_FRAME_DELTA_SECONDS
        );
        accumulatorSeconds += cappedDeltaSeconds;

        int stepsExecuted = 0;
        List<BallSimulationEvent> events = new ArrayList<>(1);
        while (stepsExecuted < VolleyballSimulationConfig.MAX_SUB_STEPS
                && accumulatorSeconds + ACCUMULATOR_EPSILON_SECONDS
                >= VolleyballSimulationConfig.FIXED_STEP_SECONDS) {
            OptionalDouble groundContactTime = VolleyballSimulationMath.findGroundContactTime(
                    state,
                    VolleyballSimulationConfig.FIXED_STEP_SECONDS
            );

            if (groundContactTime.isPresent()) {
                state = stateAtGroundContact(groundContactTime.getAsDouble());
                events.add(toGroundContactEvent(state));
                groundContactOccurred = true;
                accumulatorSeconds = 0.0;
            } else {
                state = VolleyballSimulationMath.stepFreeFlight(
                        state,
                        VolleyballSimulationConfig.FIXED_STEP_SECONDS
                );
                accumulatorSeconds -= VolleyballSimulationConfig.FIXED_STEP_SECONDS;
                if (accumulatorSeconds < 0.0
                        && accumulatorSeconds > -ACCUMULATOR_EPSILON_SECONDS) {
                    accumulatorSeconds = 0.0;
                }
            }

            stepsExecuted++;
            totalSimulationSteps++;

            if (groundContactOccurred) {
                break;
            }
        }

        return new BallSimulationAdvanceResult(stepsExecuted, events);
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

    public boolean hasGroundContactOccurred() {
        return groundContactOccurred;
    }

    public void reset(VolleyballState newState) {
        state = Objects.requireNonNull(newState, "newState must not be null");
        accumulatorSeconds = 0.0;
        totalSimulationSteps = 0L;
        groundContactOccurred = false;
    }

    private VolleyballState stateAtGroundContact(double contactTimeSeconds) {
        VolleyballState impactState = VolleyballSimulationMath.stepFreeFlight(
                state,
                contactTimeSeconds
        );
        BallVector3 impactPosition = impactState.position();
        BallVector3 normalizedPosition = new BallVector3(
                impactPosition.x(),
                VolleyballConfig.RADIUS_METERS,
                impactPosition.z()
        );
        return new VolleyballState(normalizedPosition, impactState.velocity());
    }

    private static BallGroundContactEvent toGroundContactEvent(VolleyballState impactState) {
        BallVector3 position = impactState.position();
        CourtResult courtResult = IndoorCourtClassifier.classifyResult(
                position.x(),
                position.z()
        );
        CourtSide courtSide = IndoorCourtClassifier.classifySide(position.z());
        return new BallGroundContactEvent(
                position,
                impactState.velocity(),
                courtResult,
                courtSide
        );
    }
}
