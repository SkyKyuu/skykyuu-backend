package com.skykyuu.backend.game.simulation.ball;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class VolleyballSimulationMathTests {

    private static final double TOLERANCE = 1.0e-12;

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

    private static VolleyballState knownInitialState() {
        return new VolleyballState(
                new BallVector3(0.0, 3.0, -3.0),
                new BallVector3(0.0, 3.0, 6.0)
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
