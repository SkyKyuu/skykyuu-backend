package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.court.CourtResult;
import com.skykyuu.backend.game.court.CourtSide;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStepVolleyballPlayerContactTests {

    private static final double FIXED_STEP_SECONDS =
            VolleyballSimulationConfig.FIXED_STEP_SECONDS;

    @Test
    void noTargetsOverloadPreservesExistingBehavior() {
        VolleyballState initialState = overlappingInitialState();
        FixedStepVolleyballSimulator noArgumentSimulator =
                new FixedStepVolleyballSimulator(initialState);
        FixedStepVolleyballSimulator emptyListSimulator =
                new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult noArgumentResult = noArgumentSimulator.advance(
                FIXED_STEP_SECONDS
        );
        BallSimulationAdvanceResult emptyListResult = emptyListSimulator.advance(
                FIXED_STEP_SECONDS,
                List.of()
        );

        assertEquals(noArgumentResult, emptyListResult);
        assertEquals(noArgumentSimulator.getState(), emptyListSimulator.getState());
    }

    @Test
    void playerContactDoesNotChangeBallTrajectory() {
        VolleyballState initialState = overlappingInitialState();
        FixedStepVolleyballSimulator referenceSimulator =
                new FixedStepVolleyballSimulator(initialState);
        FixedStepVolleyballSimulator contactSimulator =
                new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult referenceResult = referenceSimulator.advance(
                FIXED_STEP_SECONDS
        );
        BallSimulationAdvanceResult contactResult = contactSimulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer())
        );

        assertTrue(referenceResult.events().isEmpty());
        assertEquals(1, contactResult.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, contactResult.events().getFirst());
        assertEquals(referenceSimulator.getState(), contactSimulator.getState());
        assertEquals(
                VolleyballSimulationMath.stepFreeFlight(initialState, FIXED_STEP_SECONDS),
                contactSimulator.getState()
        );
    }

    @Test
    void emitsSingleEventWhileOverlapRemainsActiveAcrossSteps() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS * 3.0,
                List.of(overlappingPlayer())
        );

        assertEquals(3, result.executedSteps());
        assertEquals(1, result.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, result.events().getFirst());
    }

    @Test
    void emitsAgainAfterPlayerLeavesAndReentersOverlap() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );
        PlayerBallContactTarget overlappingPlayer = overlappingPlayer();
        PlayerBallContactTarget farPlayerWithSameId = new PlayerBallContactTarget(
                overlappingPlayer.playerId(),
                overlappingPlayer.teamSide(),
                new BallVector3(10.0, 0.0, 0.0)
        );

        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer)
        );
        BallSimulationAdvanceResult leave = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayerWithSameId)
        );
        BallSimulationAdvanceResult reentry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer)
        );

        assertEquals(1, entry.events().size());
        assertTrue(leave.events().isEmpty());
        assertEquals(1, reentry.events().size());
    }

    @Test
    void emitsOnlyForOverlappingPlayerAmongMultipleTargets() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );
        PlayerBallContactTarget overlappingPlayer = overlappingPlayer();
        PlayerBallContactTarget farPlayer = new PlayerBallContactTarget(
                "far-player",
                TeamSide.B,
                new BallVector3(10.0, 0.0, 0.0)
        );

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer, overlappingPlayer)
        );

        assertEquals(1, result.events().size());
        PlayerBallContactEvent event = assertInstanceOf(
                PlayerBallContactEvent.class,
                result.events().getFirst()
        );
        assertEquals(overlappingPlayer.playerId(), event.playerId());
    }

    @Test
    void resetClearsActivePlayerContactIds() {
        VolleyballState initialState = overlappingInitialState();
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);
        PlayerBallContactTarget player = overlappingPlayer();
        simulator.advance(FIXED_STEP_SECONDS, List.of(player));

        simulator.reset(initialState);
        BallSimulationAdvanceResult resultAfterReset = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player)
        );

        assertEquals(1, resultAfterReset.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, resultAfterReset.events().getFirst());
    }

    @Test
    void emitsPlayerContactBeforeGroundContactWithinImpactStep() {
        VolleyballState initialState = new VolleyballState(
                new BallVector3(0.0, VolleyballConfig.RADIUS_METERS + 0.01, 0.0),
                new BallVector3(0.0, -1.0, 0.0)
        );
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer())
        );

        assertEquals(2, result.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, result.events().get(0));
        BallGroundContactEvent groundEvent = assertInstanceOf(
                BallGroundContactEvent.class,
                result.events().get(1)
        );
        assertEquals(CourtResult.IN, groundEvent.courtResult());
        assertEquals(CourtSide.CENTER, groundEvent.courtSide());
        assertEquals(VolleyballConfig.RADIUS_METERS, groundEvent.position().y(), 0.0);
    }

    @Test
    void frontendPreviewEmitsPlayerContactBeforeInSideBGroundContact() {
        VolleyballState initialState = new VolleyballState(
                new BallVector3(0.0, 3.0, -3.0),
                new BallVector3(0.0, 4.5, 6.0)
        );
        PlayerBallContactTarget playerB = new PlayerBallContactTarget(
                "player-b",
                TeamSide.B,
                new BallVector3(0.0, 0.0, 4.5)
        );
        FixedStepVolleyballSimulator contactSimulator =
                new FixedStepVolleyballSimulator(initialState);
        FixedStepVolleyballSimulator referenceSimulator =
                new FixedStepVolleyballSimulator(initialState);
        List<BallSimulationEvent> events = new ArrayList<>();

        for (int step = 0; step < 180 && !contactSimulator.hasGroundContactOccurred(); step++) {
            BallSimulationAdvanceResult contactResult = contactSimulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(playerB)
            );
            referenceSimulator.advance(FIXED_STEP_SECONDS);
            events.addAll(contactResult.events());
            assertEquals(referenceSimulator.getState(), contactSimulator.getState());
        }

        assertTrue(contactSimulator.hasGroundContactOccurred());
        assertEquals(2, events.size());
        PlayerBallContactEvent playerEvent = assertInstanceOf(
                PlayerBallContactEvent.class,
                events.get(0)
        );
        BallGroundContactEvent groundEvent = assertInstanceOf(
                BallGroundContactEvent.class,
                events.get(1)
        );
        assertEquals("player-b", playerEvent.playerId());
        assertEquals(TeamSide.B, playerEvent.teamSide());
        assertEquals(playerB.position(), playerEvent.playerPosition());
        assertEquals(CourtResult.IN, groundEvent.courtResult());
        assertEquals(CourtSide.B, groundEvent.courtSide());
        assertEquals(referenceSimulator.getState(), contactSimulator.getState());
    }

    private static VolleyballState overlappingInitialState() {
        return new VolleyballState(
                new BallVector3(0.0, 1.0, 0.0),
                new BallVector3(0.0, 0.0, 0.0)
        );
    }

    private static PlayerBallContactTarget overlappingPlayer() {
        return new PlayerBallContactTarget(
                "player-a",
                TeamSide.A,
                new BallVector3(0.0, 0.0, 0.0)
        );
    }
}
