package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.court.CourtResult;
import com.skykyuu.backend.game.court.CourtSide;
import com.skykyuu.backend.game.simulation.input.PlayerHitInput;
import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.input.PlayerHitIntentTracker;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStepVolleyballHitGatingTests {

    private static final double FIXED_STEP_SECONDS =
            VolleyballSimulationConfig.FIXED_STEP_SECONDS;

    @Test
    void rejectsNullContactTargetsAndHitIntents() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );

        assertThrows(
                NullPointerException.class,
                () -> simulator.advance(FIXED_STEP_SECONDS, null)
        );
        assertThrows(
                NullPointerException.class,
                () -> simulator.advance(FIXED_STEP_SECONDS, List.of(), null)
        );
    }

    @Test
    void heldWithoutPressedDoesNotApplyResponse() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer()),
                List.of(new PlayerHitIntent("player-a", true, false))
        );

        assertEquals(1, result.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, result.events().getFirst());
        assertTrue(result.events().stream().noneMatch(
                PlayerBallContactResponseEvent.class::isInstance
        ));
    }

    @Test
    void pressedHitAfterEntryAppliesResponseWithoutNewContact() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );
        PlayerBallContactTarget player = overlappingPlayer();

        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of()
        );
        BallSimulationAdvanceResult hit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );

        assertEquals(1, entry.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, entry.events().getFirst());
        assertEquals(1, hit.events().size());
        assertInstanceOf(PlayerBallContactResponseEvent.class, hit.events().getFirst());
        assertTrue(hit.events().stream().noneMatch(PlayerBallContactEvent.class::isInstance));
    }

    @Test
    void lateResponseUsesCurrentBallSnapshot() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                new VolleyballState(
                        new BallVector3(0.0, 1.0, 0.0),
                        new BallVector3(1.0, 2.0, 0.5)
                )
        );
        PlayerBallContactTarget player = overlappingPlayer();

        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of()
        );
        PlayerBallContactEvent entryContact = assertInstanceOf(
                PlayerBallContactEvent.class,
                entry.events().getFirst()
        );
        VolleyballState expectedResponseSnapshot = VolleyballSimulationMath.stepFreeFlight(
                simulator.getState(),
                FIXED_STEP_SECONDS
        );

        BallSimulationAdvanceResult hit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );

        PlayerBallContactResponseEvent response = assertInstanceOf(
                PlayerBallContactResponseEvent.class,
                hit.events().getFirst()
        );
        assertEquals(expectedResponseSnapshot.position(), response.ballPosition());
        assertEquals(expectedResponseSnapshot.velocity(), response.incomingVelocity());
        assertNotEquals(entryContact.ballPosition(), response.ballPosition());
        assertNotEquals(entryContact.ballVelocity(), response.incomingVelocity());
    }

    @Test
    void responseIsConsumedUntilPlayerLeavesOverlap() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );
        PlayerBallContactTarget player = overlappingPlayer();

        BallSimulationAdvanceResult firstHit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult repeatedHit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );

        assertEquals(2, firstHit.events().size());
        assertTrue(repeatedHit.events().isEmpty());
    }

    @Test
    void leaveAndReentryPermitNewContactAndResponse() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );
        PlayerBallContactTarget player = overlappingPlayer();
        PlayerBallContactTarget farPlayer = new PlayerBallContactTarget(
                player.playerId(),
                player.teamSide(),
                new BallVector3(10.0, 0.0, 0.0)
        );

        BallSimulationAdvanceResult firstResponse = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult leave = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of()
        );
        BallSimulationAdvanceResult reentry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );

        assertEquals(2, firstResponse.events().size());
        assertTrue(leave.events().isEmpty());
        assertEquals(2, reentry.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, reentry.events().get(0));
        assertInstanceOf(PlayerBallContactResponseEvent.class, reentry.events().get(1));
    }

    @Test
    void pressedHitBeforeOverlapIsBufferedForTheNextStep() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );
        PlayerBallContactTarget player = overlappingPlayer();
        PlayerBallContactTarget farPlayer = new PlayerBallContactTarget(
                player.playerId(),
                player.teamSide(),
                new BallVector3(10.0, 0.0, 0.0)
        );

        BallSimulationAdvanceResult earlyHit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(new PlayerHitIntent("player-a", true, false))
        );

        assertTrue(earlyHit.events().isEmpty());
        assertEquals(2, entry.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, entry.events().get(0));
        assertInstanceOf(PlayerBallContactResponseEvent.class, entry.events().get(1));
    }

    @Test
    void intentForDifferentPlayerDoesNotApplyResponse() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer()),
                List.of(pressedIntent("player-b"))
        );

        assertEquals(1, result.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, result.events().getFirst());
    }

    @Test
    void frontendPreviewWithLateHitLandsInOnSideA() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                new VolleyballState(
                        new BallVector3(0.0, 3.0, -3.0),
                        new BallVector3(0.0, 4.5, 6.0)
                )
        );
        PlayerBallContactTarget playerB = new PlayerBallContactTarget(
                "player-b",
                TeamSide.B,
                new BallVector3(0.0, 0.0, 4.5)
        );
        List<BallSimulationEvent> events = new ArrayList<>();
        boolean contactOccurred = false;
        boolean responseOccurred = false;

        for (int step = 0; step < 240 && !simulator.hasGroundContactOccurred(); step++) {
            List<PlayerHitIntent> intents = contactOccurred && !responseOccurred
                    ? List.of(pressedIntent("player-b"))
                    : List.of();
            BallSimulationAdvanceResult result = simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(playerB),
                    intents
            );
            events.addAll(result.events());
            contactOccurred = contactOccurred || result.events().stream().anyMatch(
                    PlayerBallContactEvent.class::isInstance
            );
            responseOccurred = responseOccurred || result.events().stream().anyMatch(
                    PlayerBallContactResponseEvent.class::isInstance
            );
        }

        assertTrue(simulator.hasGroundContactOccurred());
        assertTrue(responseOccurred);
        assertEquals(3, events.size());
        assertInstanceOf(PlayerBallContactEvent.class, events.get(0));
        PlayerBallContactResponseEvent response = assertInstanceOf(
                PlayerBallContactResponseEvent.class,
                events.get(1)
        );
        BallGroundContactEvent ground = assertInstanceOf(
                BallGroundContactEvent.class,
                events.get(2)
        );
        assertEquals("player-b", response.playerId());
        assertEquals(0.90, response.hitTimingForwardMultiplier());
        assertEquals(6.3, response.outgoingVelocity().y());
        assertEquals(-4.5, response.outgoingVelocity().z());
        assertEquals(CourtResult.IN, ground.courtResult());
        assertEquals(CourtSide.A, ground.courtSide());
    }

    @Test
    void rawHitTransitionThroughTrackerTriggersServerResponse() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                overlappingInitialState()
        );
        PlayerBallContactTarget player = overlappingPlayer();
        PlayerHitIntentTracker tracker = new PlayerHitIntentTracker();

        PlayerHitIntent released = tracker.update(new PlayerHitInput("player-a", false));
        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(released)
        );

        PlayerHitIntent pressed = tracker.update(new PlayerHitInput("player-a", true));
        BallSimulationAdvanceResult hit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressed)
        );

        assertEquals(1, entry.events().size());
        assertTrue(pressed.hitPressed());
        assertEquals(1, hit.events().size());
        assertInstanceOf(PlayerBallContactResponseEvent.class, hit.events().getFirst());
    }

    @Test
    void resetClearsResponseConsumption() {
        VolleyballState initialState = overlappingInitialState();
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);
        PlayerBallContactTarget player = overlappingPlayer();

        BallSimulationAdvanceResult firstResponse = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );

        simulator.reset(initialState);
        BallSimulationAdvanceResult responseAfterReset = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );

        assertEquals(2, firstResponse.events().size());
        assertEquals(2, responseAfterReset.events().size());
        assertInstanceOf(PlayerBallContactResponseEvent.class,
                responseAfterReset.events().get(1));
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
