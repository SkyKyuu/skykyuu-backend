package com.skykyuu.backend.game.simulation.ball;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStepVolleyballSimulatorTests {

    private static final double TOLERANCE = 1.0e-10;

    @Test
    void executesOneFixedStep() {
        VolleyballState initialState = knownInitialState();
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult result = simulator.advance(
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
        );

        assertEquals(1, result.executedSteps());
        assertTrue(result.events().isEmpty());
        assertEquals(1L, simulator.getTotalSimulationSteps());
        assertEquals(0.0, simulator.getAccumulatorSeconds(), TOLERANCE);
        assertEquals(
                VolleyballSimulationMath.stepFreeFlight(
                        initialState,
                        VolleyballSimulationConfig.FIXED_STEP_SECONDS
                ),
                simulator.getState()
        );
    }

    @Test
    void simulatesOneSecondAtThirtyFramesPerSecond() {
        FixedStepVolleyballSimulator simulator = simulateOneSecondAtFramesPerSecond(30);

        assertEquals(60L, simulator.getTotalSimulationSteps());
        assertKnownOneSecondState(simulator.getState());
    }

    @Test
    void simulatesOneSecondAtSixtyFramesPerSecond() {
        FixedStepVolleyballSimulator simulator = simulateOneSecondAtFramesPerSecond(60);

        assertEquals(60L, simulator.getTotalSimulationSteps());
        assertKnownOneSecondState(simulator.getState());
    }

    @Test
    void simulatesOneSecondAtOneHundredTwentyFramesPerSecond() {
        FixedStepVolleyballSimulator simulator = simulateOneSecondAtFramesPerSecond(120);

        assertEquals(60L, simulator.getTotalSimulationSteps());
        assertKnownOneSecondState(simulator.getState());
    }

    @Test
    void producesEquivalentStateAcrossRenderFrameRates() {
        VolleyballState stateAt30Fps = simulateOneSecondAtFramesPerSecond(30).getState();
        VolleyballState stateAt60Fps = simulateOneSecondAtFramesPerSecond(60).getState();
        VolleyballState stateAt120Fps = simulateOneSecondAtFramesPerSecond(120).getState();

        assertStatesEqual(stateAt30Fps, stateAt60Fps);
        assertStatesEqual(stateAt60Fps, stateAt120Fps);
    }

    @Test
    void ignoresInvalidFrameDeltasWithoutChangingState() {
        VolleyballState initialState = knownInitialState();
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);
        List<Double> invalidDeltas = List.of(
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                -1.0,
                0.0
        );

        for (double invalidDelta : invalidDeltas) {
            BallSimulationAdvanceResult result = simulator.advance(invalidDelta);
            assertEquals(0, result.executedSteps());
            assertTrue(result.events().isEmpty());
        }

        assertSame(initialState, simulator.getState());
        assertEquals(0.0, simulator.getAccumulatorSeconds(), 0.0);
        assertEquals(0L, simulator.getTotalSimulationSteps());
    }

    @Test
    void capsLargeFrameDeltaAtOneTenthOfASecond() {
        FixedStepVolleyballSimulator cappedSimulator = new FixedStepVolleyballSimulator(
                knownInitialState()
        );
        FixedStepVolleyballSimulator referenceSimulator = new FixedStepVolleyballSimulator(
                knownInitialState()
        );

        int cappedSteps = cappedSimulator.advance(10.0).executedSteps();
        int referenceSteps = referenceSimulator.advance(
                VolleyballSimulationConfig.MAX_FRAME_DELTA_SECONDS
        ).executedSteps();

        assertEquals(referenceSteps, cappedSteps);
        assertEquals(6, cappedSteps);
        assertEquals(referenceSimulator.getState(), cappedSimulator.getState());
    }

    @Test
    void neverExecutesMoreThanMaximumSubStepsPerAdvance() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                knownInitialState()
        );

        int steps = simulator.advance(Double.MAX_VALUE).executedSteps();

        assertTrue(steps <= VolleyballSimulationConfig.MAX_SUB_STEPS);
        assertEquals(steps, simulator.getTotalSimulationSteps());
    }

    @Test
    void carriesPartialTimeInAccumulatorUntilAFullStepExists() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                knownInitialState()
        );
        double halfStep = VolleyballSimulationConfig.FIXED_STEP_SECONDS / 2.0;

        assertEquals(0, simulator.advance(halfStep).executedSteps());
        assertEquals(halfStep, simulator.getAccumulatorSeconds(), TOLERANCE);
        assertEquals(1, simulator.advance(halfStep).executedSteps());
        assertEquals(0.0, simulator.getAccumulatorSeconds(), TOLERANCE);
    }

    @Test
    void resetReplacesStateAndClearsCounters() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                knownInitialState()
        );
        simulator.advance(0.025);
        VolleyballState resetState = new VolleyballState(
                new BallVector3(1.0, 2.0, 3.0),
                new BallVector3(4.0, 5.0, 6.0)
        );

        simulator.reset(resetState);

        assertSame(resetState, simulator.getState());
        assertEquals(0.0, simulator.getAccumulatorSeconds(), 0.0);
        assertEquals(0L, simulator.getTotalSimulationSteps());
    }

    private static FixedStepVolleyballSimulator simulateOneSecondAtFramesPerSecond(
            int framesPerSecond
    ) {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                knownInitialState()
        );
        double frameDeltaSeconds = 1.0 / framesPerSecond;

        for (int frame = 0; frame < framesPerSecond; frame++) {
            simulator.advance(frameDeltaSeconds);
        }

        return simulator;
    }

    private static VolleyballState knownInitialState() {
        return new VolleyballState(
                new BallVector3(0.0, 3.0, -3.0),
                new BallVector3(0.0, 3.0, 6.0)
        );
    }

    private static void assertKnownOneSecondState(VolleyballState state) {
        assertEquals(0.0, state.position().x(), TOLERANCE);
        assertEquals(1.095, state.position().y(), TOLERANCE);
        assertEquals(3.0, state.position().z(), TOLERANCE);
        assertEquals(0.0, state.velocity().x(), TOLERANCE);
        assertEquals(-6.81, state.velocity().y(), TOLERANCE);
        assertEquals(6.0, state.velocity().z(), TOLERANCE);
    }

    private static void assertStatesEqual(VolleyballState expected, VolleyballState actual) {
        assertEquals(expected.position().x(), actual.position().x(), TOLERANCE);
        assertEquals(expected.position().y(), actual.position().y(), TOLERANCE);
        assertEquals(expected.position().z(), actual.position().z(), TOLERANCE);
        assertEquals(expected.velocity().x(), actual.velocity().x(), TOLERANCE);
        assertEquals(expected.velocity().y(), actual.velocity().y(), TOLERANCE);
        assertEquals(expected.velocity().z(), actual.velocity().z(), TOLERANCE);
    }
}
