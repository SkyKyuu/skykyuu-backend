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
import static org.junit.jupiter.api.Assertions.fail;

class FixedStepVolleyballHitBufferTests {

    private static final double FIXED_STEP_SECONDS =
            VolleyballSimulationConfig.FIXED_STEP_SECONDS;

    @Test
    void directHitDuringOverlapEmitsContactThenResponse() {
        FixedStepVolleyballSimulator simulator = newSimulator();

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );

        assertContactAndResponse(result, "player-a");
    }

    @Test
    void oneStepEarlyHitRespondsOnEntry() {
        FixedStepVolleyballSimulator simulator = newSimulator();

        BallSimulationAdvanceResult earlyHit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertTrue(earlyHit.events().isEmpty());
        assertContactAndResponse(entry, "player-a");
    }

    @Test
    void hitRemainsEligibleAcrossSeveralStepsWithinWindow() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a"))
        );
        simulator.advance(FIXED_STEP_SECONDS, List.of(farPlayer), List.of());
        simulator.advance(FIXED_STEP_SECONDS, List.of(farPlayer), List.of());

        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertContactAndResponse(entry, "player-a");
    }

    @Test
    void bufferCanRespondAtStartOfSixthStep() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a"))
        );
        advanceWithoutHit(simulator, farPlayer, 4);

        BallSimulationAdvanceResult sixthStep = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertContactAndResponse(sixthStep, "player-a");
    }

    @Test
    void bufferHasExpiredBeforeSeventhStep() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a"))
        );
        advanceWithoutHit(simulator, farPlayer, 5);

        BallSimulationAdvanceResult seventhStep = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertContactWithoutResponse(seventhStep, "player-a");
    }

    @Test
    void zeroStepAdvanceArmsWithoutDecayingBuffer() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);
        double halfStep = FIXED_STEP_SECONDS / 2.0;

        BallSimulationAdvanceResult arm = simulator.advance(
                halfStep,
                List.of(farPlayer),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult firstStep = simulator.advance(
                halfStep,
                List.of(farPlayer),
                List.of()
        );
        advanceWithoutHit(simulator, farPlayer, 4);
        BallSimulationAdvanceResult sixthStep = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertEquals(0, arm.executedSteps());
        assertEquals(1, firstStep.executedSteps());
        assertContactAndResponse(sixthStep, "player-a");
    }

    @Test
    void zeroAndInvalidDeltasPreserveNewlyArmedHits() {
        for (double delta : new double[]{0.0, Double.NaN}) {
            FixedStepVolleyballSimulator simulator = newSimulator();

            BallSimulationAdvanceResult arm = simulator.advance(
                    delta,
                    List.of(farPlayer("player-a", TeamSide.A)),
                    List.of(pressedIntent("player-a"))
            );
            BallSimulationAdvanceResult response = simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(overlappingPlayer("player-a", TeamSide.A)),
                    List.of()
            );

            assertEquals(0, arm.executedSteps());
            assertEquals(0.0, simulator.getAccumulatorSeconds(), 1.0e-12);
            assertContactAndResponse(response, "player-a");
        }
    }

    @Test
    void heldIntentDoesNotRearmExpiredBuffer() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a"))
        );
        for (int step = 2; step <= 6; step++) {
            simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(farPlayer),
                    List.of(heldIntent("player-a"))
            );
        }

        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of(heldIntent("player-a"))
        );

        assertContactWithoutResponse(entry, "player-a");
    }

    @Test
    void responseConsumesBufferBeforeLeaveAndReentry() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget overlappingPlayer =
                overlappingPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult response = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        );
        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of()
        );
        BallSimulationAdvanceResult reentry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        );

        assertContactAndResponse(response, "player-a");
        assertContactWithoutResponse(reentry, "player-a");
    }

    @Test
    void secondHitDuringRespondedOverlapIsDiscarded() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget overlappingPlayer =
                overlappingPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult secondHit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a"))
        );
        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of()
        );
        BallSimulationAdvanceResult reentry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        );

        assertTrue(secondHit.events().isEmpty());
        assertContactWithoutResponse(reentry, "player-a");
    }

    @Test
    void secondHitDuringRespondedOverlapIsDiscardedEvenWithZeroSteps() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget overlappingPlayer =
                overlappingPlayer("player-a", TeamSide.A);
        double halfStep = FIXED_STEP_SECONDS / 2.0;

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult secondHit = simulator.advance(
                halfStep,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a"))
        );
        simulator.advance(
                halfStep,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of()
        );
        BallSimulationAdvanceResult reentry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        );

        assertEquals(0, secondHit.executedSteps());
        assertTrue(secondHit.events().isEmpty());
        assertContactWithoutResponse(reentry, "player-a");
    }

    @Test
    void newHitAfterObservedLeaveCanRespondOnReentry() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget overlappingPlayer =
                overlappingPlayer("player-a", TeamSide.A);
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a"))
        );
        simulator.advance(FIXED_STEP_SECONDS, List.of(farPlayer), List.of());
        BallSimulationAdvanceResult arm = simulator.advance(
                0.0,
                List.of(farPlayer),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult reentry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        );

        assertEquals(0, arm.executedSteps());
        assertContactAndResponse(reentry, "player-a");
    }

    @Test
    void bufferedResponseUsesCurrentBallSnapshot() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                new VolleyballState(
                        new BallVector3(0.0, 1.0, 0.0),
                        new BallVector3(1.0, 2.0, 0.5)
                )
        );

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );
        VolleyballState expectedSnapshot = VolleyballSimulationMath.stepFreeFlight(
                simulator.getState(),
                FIXED_STEP_SECONDS
        );

        BallSimulationAdvanceResult responseResult = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        PlayerBallContactResponseEvent response = assertInstanceOf(
                PlayerBallContactResponseEvent.class,
                responseResult.events().get(1)
        );
        assertEquals(expectedSnapshot.position(), response.ballPosition());
        assertEquals(expectedSnapshot.velocity(), response.incomingVelocity());
    }

    @Test
    void bufferForAnotherPlayerDoesNotRespond() {
        FixedStepVolleyballSimulator simulator = newSimulator();

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-b", TeamSide.B)),
                List.of(pressedIntent("player-a"))
        );

        assertContactWithoutResponse(result, "player-b");
    }

    @Test
    void groundImpactRemainsTerminalWithBufferedHit() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                new VolleyballState(
                        new BallVector3(0.0, VolleyballConfig.RADIUS_METERS + 0.01, 0.0),
                        new BallVector3(0.0, -1.0, 0.0)
                )
        );

        simulator.advance(
                0.0,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult impact = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertEquals(2, impact.events().size());
        assertInstanceOf(PlayerBallContactEvent.class, impact.events().get(0));
        assertInstanceOf(BallGroundContactEvent.class, impact.events().get(1));
        assertTrue(impact.events().stream().noneMatch(
                PlayerBallContactResponseEvent.class::isInstance
        ));
        assertTrue(simulator.hasGroundContactOccurred());
    }

    @Test
    void resetClearsBufferedHits() {
        VolleyballState initialState = initialState();
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);

        simulator.advance(
                0.0,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );
        simulator.reset(initialState);
        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertContactWithoutResponse(entry, "player-a");
    }

    @Test
    void frontendPreviewWithBufferedHitLandsInOnSideA() {
        int contactStep = findPreviewContactStep();
        int pressedStep = contactStep - 3;
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                previewInitialState()
        );
        PlayerBallContactTarget playerB = previewPlayerB();
        List<BallSimulationEvent> events = new ArrayList<>();

        for (int step = 0; step < 240 && !simulator.hasGroundContactOccurred(); step++) {
            List<PlayerHitIntent> intents = step == pressedStep
                    ? List.of(pressedIntent("player-b"))
                    : List.of();
            events.addAll(simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(playerB),
                    intents
            ).events());
        }

        assertTrue(simulator.hasGroundContactOccurred());
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
        assertEquals(6.3, response.outgoingVelocity().y());
        assertEquals(-5.0, response.outgoingVelocity().z());
        assertEquals(CourtResult.IN, ground.courtResult());
        assertEquals(CourtSide.A, ground.courtSide());
    }

    private static void advanceWithoutHit(
            FixedStepVolleyballSimulator simulator,
            PlayerBallContactTarget target,
            int steps
    ) {
        for (int step = 0; step < steps; step++) {
            simulator.advance(FIXED_STEP_SECONDS, List.of(target), List.of());
        }
    }

    private static void assertContactAndResponse(
            BallSimulationAdvanceResult result,
            String playerId
    ) {
        assertEquals(2, result.events().size());
        PlayerBallContactEvent contact = assertInstanceOf(
                PlayerBallContactEvent.class,
                result.events().get(0)
        );
        PlayerBallContactResponseEvent response = assertInstanceOf(
                PlayerBallContactResponseEvent.class,
                result.events().get(1)
        );
        assertEquals(playerId, contact.playerId());
        assertEquals(playerId, response.playerId());
    }

    private static void assertContactWithoutResponse(
            BallSimulationAdvanceResult result,
            String playerId
    ) {
        assertEquals(1, result.events().size());
        PlayerBallContactEvent contact = assertInstanceOf(
                PlayerBallContactEvent.class,
                result.events().getFirst()
        );
        assertEquals(playerId, contact.playerId());
    }

    private static FixedStepVolleyballSimulator newSimulator() {
        return new FixedStepVolleyballSimulator(initialState());
    }

    private static VolleyballState initialState() {
        return new VolleyballState(
                new BallVector3(0.0, 1.0, 0.0),
                new BallVector3(0.0, 0.0, 0.0)
        );
    }

    private static PlayerBallContactTarget overlappingPlayer(
            String playerId,
            TeamSide teamSide
    ) {
        return new PlayerBallContactTarget(
                playerId,
                teamSide,
                new BallVector3(0.0, 0.0, 0.0)
        );
    }

    private static PlayerBallContactTarget farPlayer(String playerId, TeamSide teamSide) {
        return new PlayerBallContactTarget(
                playerId,
                teamSide,
                new BallVector3(10.0, 0.0, 0.0)
        );
    }

    private static PlayerHitIntent pressedIntent(String playerId) {
        return new PlayerHitIntent(playerId, true, true);
    }

    private static PlayerHitIntent heldIntent(String playerId) {
        return new PlayerHitIntent(playerId, true, false);
    }

    private static VolleyballState previewInitialState() {
        return new VolleyballState(
                new BallVector3(0.0, 3.0, -3.0),
                new BallVector3(0.0, 4.5, 6.0)
        );
    }

    private static PlayerBallContactTarget previewPlayerB() {
        return new PlayerBallContactTarget(
                "player-b",
                TeamSide.B,
                new BallVector3(0.0, 0.0, 4.5)
        );
    }

    private static int findPreviewContactStep() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                previewInitialState()
        );
        PlayerBallContactTarget playerB = previewPlayerB();

        for (int step = 0; step < 180 && !simulator.hasGroundContactOccurred(); step++) {
            BallSimulationAdvanceResult result = simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(playerB),
                    List.of()
            );
            if (result.events().stream().anyMatch(PlayerBallContactEvent.class::isInstance)) {
                return step;
            }
        }
        return fail("preview trajectory did not reach player B");
    }
}
