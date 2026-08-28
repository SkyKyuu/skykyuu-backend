package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.court.CourtResult;
import com.skykyuu.backend.game.court.CourtSide;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStepVolleyballGroundContactTests {

    private static final double TOLERANCE = 1.0e-12;

    @Test
    void advancesOnlyToImpactEmitsOneEventAndFreezesUntilReset() {
        VolleyballState initialState = new VolleyballState(
                new BallVector3(1.0, VolleyballConfig.RADIUS_METERS + 0.01, -2.0),
                new BallVector3(2.0, -1.0, 3.0)
        );
        double contactTime = VolleyballSimulationMath.findGroundContactTime(
                initialState,
                VolleyballSimulationConfig.FIXED_STEP_SECONDS
        ).orElseThrow();
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult impactResult = simulator.advance(
                VolleyballSimulationConfig.FIXED_STEP_SECONDS * 2.0
        );

        assertEquals(1, impactResult.executedSteps());
        assertEquals(1, impactResult.events().size());
        assertEquals(1L, simulator.getTotalSimulationSteps());
        assertEquals(0.0, simulator.getAccumulatorSeconds(), 0.0);
        assertTrue(simulator.hasGroundContactOccurred());

        BallGroundContactEvent event = (BallGroundContactEvent) impactResult.events().getFirst();
        assertEquals(VolleyballConfig.RADIUS_METERS, event.position().y(), 0.0);
        assertEquals(1.0 + 2.0 * contactTime, event.position().x(), TOLERANCE);
        assertEquals(-2.0 + 3.0 * contactTime, event.position().z(), TOLERANCE);
        assertEquals(
                -1.0 - VolleyballSimulationConfig.GRAVITY_METERS_PER_SECOND_SQUARED
                        * contactTime,
                event.velocity().y(),
                TOLERANCE
        );
        assertEquals(event.position(), simulator.getState().position());
        assertEquals(event.velocity(), simulator.getState().velocity());

        VolleyballState impactState = simulator.getState();
        BallSimulationAdvanceResult resultAfterImpact = simulator.advance(1.0);

        assertEquals(0, resultAfterImpact.executedSteps());
        assertTrue(resultAfterImpact.events().isEmpty());
        assertSame(impactState, simulator.getState());
        assertEquals(1L, simulator.getTotalSimulationSteps());
    }

    @Test
    void resetClearsGroundContactAndSimulationProgress() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                new VolleyballState(
                        new BallVector3(0.0, VolleyballConfig.RADIUS_METERS, 0.0),
                        new BallVector3(0.0, -1.0, 0.0)
                )
        );
        simulator.advance(VolleyballSimulationConfig.FIXED_STEP_SECONDS);
        VolleyballState resetState = new VolleyballState(
                new BallVector3(0.0, 3.0, -3.0),
                new BallVector3(0.0, 3.0, 6.0)
        );

        simulator.reset(resetState);

        assertFalse(simulator.hasGroundContactOccurred());
        assertSame(resetState, simulator.getState());
        assertEquals(0L, simulator.getTotalSimulationSteps());
        assertEquals(0.0, simulator.getAccumulatorSeconds(), 0.0);
        assertEquals(
                1,
                simulator.advance(VolleyballSimulationConfig.FIXED_STEP_SECONDS)
                        .executedSteps()
        );
    }

    @Test
    void previewTrajectoryLandsInCourtOnSideBExactlyOnce() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                new VolleyballState(
                        new BallVector3(0.0, 3.0, -3.0),
                        new BallVector3(0.0, 3.0, 6.0)
                )
        );
        List<BallSimulationEvent> events = new ArrayList<>();

        for (int step = 0; step < 120 && events.isEmpty(); step++) {
            events.addAll(simulator.advance(
                    VolleyballSimulationConfig.FIXED_STEP_SECONDS
            ).events());
        }

        assertEquals(1, events.size());
        BallGroundContactEvent event = (BallGroundContactEvent) events.getFirst();
        assertEquals(CourtResult.IN, event.courtResult());
        assertEquals(CourtSide.B, event.courtSide());
        assertEquals(0.0, event.position().x(), TOLERANCE);
        assertEquals(VolleyballConfig.RADIUS_METERS, event.position().y(), 0.0);
        assertTrue(event.position().z() > 0.0);
        assertTrue(event.velocity().y() < 0.0);

        BallSimulationAdvanceResult laterResult = simulator.advance(1.0);
        assertEquals(0, laterResult.executedSteps());
        assertTrue(laterResult.events().isEmpty());
    }
}
