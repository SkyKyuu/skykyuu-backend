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
    void newPlayerContactAppliesResponseAfterIncomingEvent() {
        VolleyballState initialState = overlappingInitialState();
        FixedStepVolleyballSimulator contactSimulator =
                new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult contactResult = contactSimulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer())
        );

        VolleyballState incomingState = VolleyballSimulationMath.stepFreeFlight(
                initialState,
                FIXED_STEP_SECONDS
        );
        assertEquals(2, contactResult.events().size());
        PlayerBallContactEvent contactEvent = assertInstanceOf(
                PlayerBallContactEvent.class,
                contactResult.events().get(0)
        );
        PlayerBallContactResponseEvent responseEvent = assertInstanceOf(
                PlayerBallContactResponseEvent.class,
                contactResult.events().get(1)
        );
        assertEquals(incomingState.position(), contactEvent.ballPosition());
        assertEquals(incomingState.velocity(), contactEvent.ballVelocity());
        assertEquals(contactEvent.ballVelocity(), responseEvent.incomingVelocity());
        assertEquals(incomingState.position(), contactSimulator.getState().position());
        assertEquals(new BallVector3(0.0, 6.3, 5.0),
                contactSimulator.getState().velocity());
        assertEquals(contactSimulator.getState().velocity(), responseEvent.outgoingVelocity());
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
        assertEquals(2, result.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, result.events().get(0));
        assertInstanceOf(PlayerBallContactResponseEvent.class, result.events().get(1));
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

        assertEquals(2, entry.events().size());
        assertTrue(leave.events().isEmpty());
        assertEquals(2, reentry.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, reentry.events().get(0));
        assertInstanceOf(PlayerBallContactResponseEvent.class, reentry.events().get(1));
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

        assertEquals(2, result.events().size());
        PlayerBallContactEvent event = assertInstanceOf(
                PlayerBallContactEvent.class,
                result.events().get(0)
        );
        assertEquals(overlappingPlayer.playerId(), event.playerId());
        assertInstanceOf(PlayerBallContactResponseEvent.class, result.events().get(1));
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

        assertEquals(2, resultAfterReset.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, resultAfterReset.events().get(0));
        assertInstanceOf(PlayerBallContactResponseEvent.class,
                resultAfterReset.events().get(1));
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
        assertTrue(result.events().stream().noneMatch(
                PlayerBallContactResponseEvent.class::isInstance
        ));
    }

    @Test
    void frontendPreviewRespondsTowardSideAAndLandsIn() {
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
        List<BallSimulationEvent> events = new ArrayList<>();
        boolean crossedCenterBeforeGround = false;

        for (int step = 0; step < 180 && !contactSimulator.hasGroundContactOccurred(); step++) {
            BallSimulationAdvanceResult contactResult = contactSimulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(playerB)
            );
            events.addAll(contactResult.events());
            if (!contactSimulator.hasGroundContactOccurred()
                    && contactSimulator.getState().position().z() <= 0.0) {
                crossedCenterBeforeGround = true;
            }
        }

        assertTrue(contactSimulator.hasGroundContactOccurred());
        assertTrue(crossedCenterBeforeGround);
        assertEquals(3, events.size());
        PlayerBallContactEvent playerEvent = assertInstanceOf(
                PlayerBallContactEvent.class,
                events.get(0)
        );
        PlayerBallContactResponseEvent responseEvent = assertInstanceOf(
                PlayerBallContactResponseEvent.class,
                events.get(1)
        );
        BallGroundContactEvent groundEvent = assertInstanceOf(
                BallGroundContactEvent.class,
                events.get(2)
        );
        assertEquals("player-b", playerEvent.playerId());
        assertEquals(TeamSide.B, playerEvent.teamSide());
        assertEquals(playerB.position(), playerEvent.playerPosition());
        assertEquals(TeamSide.B, responseEvent.teamSide());
        assertEquals(6.3, responseEvent.outgoingVelocity().y());
        assertEquals(-5.0, responseEvent.outgoingVelocity().z());
        assertEquals(CourtResult.IN, groundEvent.courtResult());
        assertEquals(CourtSide.A, groundEvent.courtSide());
    }

    @Test
    void incomingContactEventKeepsPreResponseVerticalVelocity() {
        double gravity = VolleyballSimulationConfig.GRAVITY_METERS_PER_SECOND_SQUARED;
        VolleyballState initialState = new VolleyballState(
                new BallVector3(0.0, 1.0, 0.0),
                new BallVector3(0.0, -3.0 + gravity * FIXED_STEP_SECONDS, 0.0)
        );
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer())
        );

        PlayerBallContactEvent contactEvent = assertInstanceOf(
                PlayerBallContactEvent.class,
                result.events().get(0)
        );
        assertEquals(-3.0, contactEvent.ballVelocity().y(), 1.0e-12);
        assertEquals(6.3, simulator.getState().velocity().y());
    }

    @Test
    void multipleNewContactsEmitAllButRespondOnlyToFirstTarget() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );
        PlayerBallContactTarget firstTarget = new PlayerBallContactTarget(
                "first-player",
                TeamSide.B,
                new BallVector3(0.0, 0.0, 0.0)
        );
        PlayerBallContactTarget secondTarget = new PlayerBallContactTarget(
                "second-player",
                TeamSide.A,
                new BallVector3(0.0, 0.0, 0.0)
        );

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(firstTarget, secondTarget)
        );

        assertEquals(3, result.events().size());
        PlayerBallContactEvent firstContact = assertInstanceOf(
                PlayerBallContactEvent.class,
                result.events().get(0)
        );
        PlayerBallContactEvent secondContact = assertInstanceOf(
                PlayerBallContactEvent.class,
                result.events().get(1)
        );
        PlayerBallContactResponseEvent response = assertInstanceOf(
                PlayerBallContactResponseEvent.class,
                result.events().get(2)
        );
        assertEquals("first-player", firstContact.playerId());
        assertEquals("second-player", secondContact.playerId());
        assertEquals("first-player", response.playerId());
        assertEquals(TeamSide.B, response.teamSide());
        assertEquals(-5.0, response.outgoingVelocity().z());
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
