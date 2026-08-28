package com.skykyuu.backend.game.simulation.ball;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VolleyballSimulationMathTests {

    private static final double TOLERANCE = 1.0e-12;
    private static final double CONTACT_TIME_EPSILON_SECONDS = 1.0e-12;

    @Test
    void advancesOneFixedStepUsingFreeFlightEquations() {
        VolleyballState initialState = knownInitialState();
        double deltaSeconds = VolleyballSimulationConfig.FIXED_STEP_SECONDS;

        VolleyballState nextState = VolleyballSimulationMath.stepFreeFlight(
                initialState,
                deltaSeconds
        );

        assertEquals(0.0, nextState.position().x(), TOLERANCE);
        assertEquals(
                3.0 + 3.0 * deltaSeconds
                        - 0.5 * 9.81 * deltaSeconds * deltaSeconds,
                nextState.position().y(),
                TOLERANCE
        );
        assertEquals(-3.0 + 6.0 * deltaSeconds,
                nextState.position().z(),
                TOLERANCE);
        assertEquals(3.0 - 9.81 * deltaSeconds,
                nextState.velocity().y(),
                TOLERANCE);
    }

    @Test
    void gravityOnlyChangesVerticalVelocity() {
        VolleyballState initialState = new VolleyballState(
                new BallVector3(1.0, 2.0, 3.0),
                new BallVector3(4.0, 5.0, 6.0)
        );

        VolleyballState nextState = VolleyballSimulationMath.stepFreeFlight(
                initialState,
                0.25
        );

        assertEquals(4.0, nextState.velocity().x(), TOLERANCE);
        assertEquals(5.0 - 9.81 * 0.25, nextState.velocity().y(), TOLERANCE);
        assertEquals(6.0, nextState.velocity().z(), TOLERANCE);
    }

    @Test
    void returnsNewStateWithoutChangingOriginalState() {
        VolleyballState initialState = knownInitialState();

        VolleyballState nextState = VolleyballSimulationMath.stepFreeFlight(
                initialState,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );

        assertNotSame(initialState, nextState);
        assertNotSame(initialState.position(), nextState.position());
        assertNotSame(initialState.velocity(), nextState.velocity());
        assertEquals(new BallVector3(0.0, 3.0, -3.0), initialState.position());
        assertEquals(new BallVector3(0.0, 3.0, 6.0), initialState.velocity());
    }

    @Test
    void sixtyPureStepsMatchOneSecondAnalyticalState() {
        VolleyballState state = knownInitialState();

        for (int step = 0; step < 60; step++) {
            state = VolleyballSimulationMath.stepFreeFlight(
                    state,
                    VolleyballSimulationConfig.FIXED_STEP_SECONDS
            );
        }

        assertKnownOneSecondState(state, 1.0e-10);
    }

    @Test
    void findsDescendingGroundContactInsideOneFixedStep() {
        VolleyballState state = new VolleyballState(
                new BallVector3(0.0, VolleyballConfig.RADIUS_METERS + 0.02, 0.0),
                new BallVector3(0.0, -2.0, 0.0)
        );

        OptionalDouble contactTime = VolleyballSimulationMath.findGroundContactTime(
                state,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );

        assertTrue(contactTime.isPresent());
        assertTrue(contactTime.getAsDouble() >= 0.0);
        assertTrue(
                contactTime.getAsDouble() <= VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );
        double gravity = VolleyballSimulationConfig.GRAVITY_METERS_PER_SECOND_SQUARED;
        double expectedContactTime = (
                -2.0 + Math.sqrt(4.0 + 2.0 * gravity * 0.02)
        ) / gravity;
        assertEquals(expectedContactTime, contactTime.getAsDouble(), TOLERANCE);
        VolleyballState impactState = VolleyballSimulationMath.stepFreeFlight(
                state,
                contactTime.getAsDouble()
        );
        assertEquals(VolleyballConfig.RADIUS_METERS, impactState.position().y(), TOLERANCE);
    }

    @Test
    void returnsNoContactWhenBallRemainsAboveGroundDuringStep() {
        VolleyballState state = new VolleyballState(
                new BallVector3(0.0, 3.0, 0.0),
                new BallVector3(0.0, 0.0, 0.0)
        );

        OptionalDouble contactTime = VolleyballSimulationMath.findGroundContactTime(
                state,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );

        assertTrue(contactTime.isEmpty());
    }

    @Test
    void rejectsInvalidMaximumDeltas() {
        VolleyballState state = new VolleyballState(
                new BallVector3(0.0, VolleyballConfig.RADIUS_METERS, 0.0),
                new BallVector3(0.0, -1.0, 0.0)
        );
        List<Double> invalidDeltas = List.of(
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                -1.0
        );

        for (double invalidDelta : invalidDeltas) {
            assertFalse(
                    VolleyballSimulationMath.findGroundContactTime(state, invalidDelta)
                            .isPresent()
            );
        }
    }

    @Test
    void acceptsAnExactDescendingContactAtZeroDelta() {
        VolleyballState state = new VolleyballState(
                new BallVector3(0.0, VolleyballConfig.RADIUS_METERS, 0.0),
                new BallVector3(0.0, -1.0, 0.0)
        );

        OptionalDouble contactTime = VolleyballSimulationMath.findGroundContactTime(state, 0.0);

        assertTrue(contactTime.isPresent());
        assertEquals(0.0, contactTime.getAsDouble(), 0.0);
    }

    @Test
    void normalizesSlightlyNegativeContactTimeWithinEpsilonToZero() {
        double candidateTime = -CONTACT_TIME_EPSILON_SECONDS / 2.0;
        VolleyballState state = stateWithDescendingGroundContactAt(candidateTime);

        OptionalDouble contactTime = VolleyballSimulationMath.findGroundContactTime(
                state,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );

        assertTrue(contactTime.isPresent());
        assertEquals(0.0, contactTime.getAsDouble(), 0.0);
    }

    @Test
    void normalizesContactTimeSlightlyAboveMaximumWithinEpsilonToMaximum() {
        double maximumDeltaSeconds = VolleyballSimulationConfig.FIXED_STEP_SECONDS;
        double candidateTime = maximumDeltaSeconds + CONTACT_TIME_EPSILON_SECONDS / 2.0;
        VolleyballState state = stateWithDescendingGroundContactAt(candidateTime);

        OptionalDouble contactTime = VolleyballSimulationMath.findGroundContactTime(
                state,
                maximumDeltaSeconds
        );

        assertTrue(contactTime.isPresent());
        assertEquals(maximumDeltaSeconds, contactTime.getAsDouble(), 0.0);
    }

    @Test
    void rejectsContactTimeAboveMaximumOutsideEpsilon() {
        double maximumDeltaSeconds = VolleyballSimulationConfig.FIXED_STEP_SECONDS;
        double candidateTime = maximumDeltaSeconds + 2.0 * CONTACT_TIME_EPSILON_SECONDS;
        VolleyballState state = stateWithDescendingGroundContactAt(candidateTime);

        OptionalDouble contactTime = VolleyballSimulationMath.findGroundContactTime(
                state,
                maximumDeltaSeconds
        );

        assertTrue(contactTime.isEmpty());
    }

    private static VolleyballState knownInitialState() {
        return new VolleyballState(
                new BallVector3(0.0, 3.0, -3.0),
                new BallVector3(0.0, 3.0, 6.0)
        );
    }

    private static VolleyballState stateWithDescendingGroundContactAt(
            double contactTimeSeconds
    ) {
        double initialVelocityY = -1.0;
        double gravity = VolleyballSimulationConfig.GRAVITY_METERS_PER_SECOND_SQUARED;
        double initialY = VolleyballConfig.RADIUS_METERS
                - initialVelocityY * contactTimeSeconds
                + 0.5 * gravity * contactTimeSeconds * contactTimeSeconds;
        return new VolleyballState(
                new BallVector3(0.0, initialY, 0.0),
                new BallVector3(0.0, initialVelocityY, 0.0)
        );
    }

    private static void assertKnownOneSecondState(VolleyballState state, double tolerance) {
        assertEquals(0.0, state.position().x(), tolerance);
        assertEquals(1.095, state.position().y(), tolerance);
        assertEquals(3.0, state.position().z(), tolerance);
        assertEquals(0.0, state.velocity().x(), tolerance);
        assertEquals(-6.81, state.velocity().y(), tolerance);
        assertEquals(6.0, state.velocity().z(), tolerance);
    }
}
