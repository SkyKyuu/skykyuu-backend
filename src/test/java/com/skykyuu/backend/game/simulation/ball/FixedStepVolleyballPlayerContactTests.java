package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.court.CourtResult;
import com.skykyuu.backend.game.court.CourtSide;
import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
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
    void oldOverloadsDelegateUsingEmptyIntentLists() {
        VolleyballState initialState = overlappingInitialState();
        FixedStepVolleyballSimulator noArgumentSimulator =
                new FixedStepVolleyballSimulator(initialState);
        FixedStepVolleyballSimulator explicitEmptySimulator =
                new FixedStepVolleyballSimulator(initialState);
        FixedStepVolleyballSimulator targetsSimulator =
                new FixedStepVolleyballSimulator(initialState);
        FixedStepVolleyballSimulator explicitTargetsSimulator =
                new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult noArgumentResult = noArgumentSimulator.advance(
                FIXED_STEP_SECONDS
        );
        BallSimulationAdvanceResult explicitEmptyResult = explicitEmptySimulator.advance(
                FIXED_STEP_SECONDS,
                List.of(),
                List.of()
        );
        BallSimulationAdvanceResult targetsResult = targetsSimulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer())
        );
        BallSimulationAdvanceResult explicitTargetsResult = explicitTargetsSimulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer()),
                List.of()
        );

        assertEquals(noArgumentResult, explicitEmptyResult);
        assertEquals(noArgumentSimulator.getState(), explicitEmptySimulator.getState());
        assertEquals(targetsResult, explicitTargetsResult);
        assertEquals(targetsSimulator.getState(), explicitTargetsSimulator.getState());
    }

    @Test
    void newPlayerContactWithoutHitDoesNotApplyResponse() {
        VolleyballState initialState = overlappingInitialState();
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer())
        );

        VolleyballState freeFlightState = VolleyballSimulationMath.stepFreeFlight(
                initialState,
                FIXED_STEP_SECONDS
        );
        assertEquals(1, result.events().size());
        PlayerBallContactEvent contactEvent = assertInstanceOf(
                PlayerBallContactEvent.class,
                result.events().getFirst()
        );
        assertEquals(freeFlightState.position(), contactEvent.ballPosition());
        assertEquals(freeFlightState.velocity(), contactEvent.ballVelocity());
        assertEquals(freeFlightState, simulator.getState());
    }

    @Test
    void emitsSingleContactWhileOverlapRemainsActiveAcrossSteps() {
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
    void emitsContactAgainAfterPlayerLeavesAndReentersOverlap() {
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
        assertInstanceOf(PlayerBallContactEvent.class, reentry.events().getFirst());
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
    void groundContactRemainsTerminalEvenWithPressedHit() {
        VolleyballState initialState = new VolleyballState(
                new BallVector3(0.0, VolleyballConfig.RADIUS_METERS + 0.01, 0.0),
                new BallVector3(0.0, -1.0, 0.0)
        );
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer()),
                List.of(pressedIntent("player-a"))
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
    void frontendPreviewWithoutHitLandsInOnSideB() {
        VolleyballState initialState = new VolleyballState(
                new BallVector3(0.0, 3.0, -3.0),
                new BallVector3(0.0, 4.5, 6.0)
        );
        PlayerBallContactTarget playerB = new PlayerBallContactTarget(
                "player-b",
                TeamSide.B,
                new BallVector3(0.0, 0.0, 4.5)
        );
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);
        List<BallSimulationEvent> events = new ArrayList<>();

        for (int step = 0; step < 180 && !simulator.hasGroundContactOccurred(); step++) {
            events.addAll(simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(playerB)
            ).events());
        }

        assertTrue(simulator.hasGroundContactOccurred());
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
        assertTrue(events.stream().noneMatch(PlayerBallContactResponseEvent.class::isInstance));
    }

    @Test
    void hitOnEntryKeepsPreResponseVerticalVelocity() {
        double gravity = VolleyballSimulationConfig.GRAVITY_METERS_PER_SECOND_SQUARED;
        VolleyballState initialState = new VolleyballState(
                new BallVector3(0.0, 1.0, 0.0),
                new BallVector3(0.0, -3.0 + gravity * FIXED_STEP_SECONDS, 0.0)
        );
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer()),
                List.of(pressedIntent("player-a"))
        );

        assertEquals(2, result.events().size());
        PlayerBallContactEvent contactEvent = assertInstanceOf(
                PlayerBallContactEvent.class,
                result.events().get(0)
        );
        PlayerBallContactResponseEvent responseEvent = assertInstanceOf(
                PlayerBallContactResponseEvent.class,
                result.events().get(1)
        );
        assertEquals(-3.0, contactEvent.ballVelocity().y(), 1.0e-12);
        assertEquals(contactEvent.ballVelocity(), responseEvent.incomingVelocity());
        assertEquals(6.3, simulator.getState().velocity().y());
    }

    @Test
    void multipleNewContactsEmitAllButRespondOnlyToFirstEligibleTarget() {
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
                List.of(firstTarget, secondTarget),
                List.of(
                        pressedIntent("first-player"),
                        pressedIntent("second-player")
                )
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

    private static PlayerHitIntent pressedIntent(String playerId) {
        return new PlayerHitIntent(playerId, true, true);
    }
}
