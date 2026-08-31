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

class FixedStepVolleyballHitTimingTests {

    private static final double FIXED_STEP_SECONDS =
            VolleyballSimulationConfig.FIXED_STEP_SECONDS;
    private static final double TOLERANCE = 1.0e-12;

    @Test
    void hitOneStepBeforeEntryProducesNegativeOneOffset() {
        FixedStepVolleyballSimulator simulator = newSimulator();

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertTiming(responseEvent(entry), -1L);
    }

    @Test
    void multiSubstepAdvanceKeepsOriginalPressStep() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                new VolleyballState(
                        new BallVector3(-0.75, 1.0, 0.0),
                        new BallVector3(6.0, 0.0, 0.0)
                )
        );

        BallSimulationAdvanceResult result = simulator.advance(
                4.0 * FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );

        assertEquals(4, result.executedSteps());
        assertTiming(responseEvent(result), -3L);
    }

    @Test
    void hitOnEntryStepProducesZeroOffset() {
        FixedStepVolleyballSimulator simulator = newSimulator();

        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );

        assertTiming(responseEvent(result), 0L);
    }

    @Test
    void lateHitTwoStepsAfterEntryProducesPositiveTwoOffset() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget player = overlappingPlayer("player-a", TeamSide.A);

        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of()
        );
        simulator.advance(FIXED_STEP_SECONDS, List.of(player), List.of());
        BallSimulationAdvanceResult lateHit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );

        assertEquals(1, entry.events().size());
        assertEquals(1, lateHit.events().size());
        assertInstanceOf(PlayerBallContactResponseEvent.class,
                lateHit.events().getFirst());
        assertTiming(responseEvent(lateHit), 2L);
    }

    @Test
    void zeroStepHitAndNextStepEntryProduceZeroOffset() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        double halfStep = FIXED_STEP_SECONDS / 2.0;

        BallSimulationAdvanceResult arm = simulator.advance(
                halfStep,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult entry = simulator.advance(
                halfStep,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertEquals(0, arm.executedSteps());
        assertTiming(responseEvent(entry), 0L);
    }

    @Test
    void timingKeepsCurrentResponseSnapshotAndPhysics() {
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
        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        PlayerBallContactResponseEvent response = responseEvent(result);
        assertTiming(response, -1L);
        assertEquals(expectedSnapshot.position(), response.ballPosition());
        assertEquals(expectedSnapshot.velocity(), response.incomingVelocity());
        assertEquals(expectedSnapshot.velocity().x(), response.outgoingVelocity().x());
        assertEquals(6.3, response.outgoingVelocity().y());
        assertEquals(5.0, response.outgoingVelocity().z());
    }

    @Test
    void expiredBufferProducesNoResponseTimingEvent() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a"))
        );
        advanceWithoutHit(simulator, farPlayer, 5);
        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertContactWithoutResponse(entry, "player-a");
    }

    @Test
    void consumedTimingDoesNotSurviveLeaveAndReentry() {
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

        assertTiming(responseEvent(response), -1L);
        assertContactWithoutResponse(reentry, "player-a");
    }

    @Test
    void secondHitDuringRespondedOverlapDoesNotCreateStaleTiming() {
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
    void zeroStepSecondHitDuringRespondedOverlapDoesNotCreateStaleTiming() {
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
        assertContactWithoutResponse(reentry, "player-a");
    }

    @Test
    void newHitAfterObservedLeaveUsesNewTimingState() {
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
        simulator.advance(
                0.0,
                List.of(farPlayer),
                List.of(pressedIntent("player-a"))
        );
        BallSimulationAdvanceResult reentry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        );

        assertTiming(responseEvent(reentry), 0L);
    }

    @Test
    void resetDoesNotReuseBufferedHitOrContactEntryTiming() {
        VolleyballState initialState = initialState();
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(initialState);
        PlayerBallContactTarget player = overlappingPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a"))
        );
        simulator.reset(initialState);
        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of()
        );
        BallSimulationAdvanceResult lateHit = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(player),
                List.of(pressedIntent("player-a"))
        );

        assertEquals(1, entry.events().size());
        assertTiming(responseEvent(lateHit), 1L);
    }

    @Test
    void multiplePlayersUseTimingFromTheRespondingTarget() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farA = farPlayer("player-a", TeamSide.A);
        PlayerBallContactTarget farB = farPlayer("player-b", TeamSide.B);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farA, farB),
                List.of(pressedIntent("player-a"))
        );
        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farA, farB),
                List.of(pressedIntent("player-b"))
        );
        BallSimulationAdvanceResult result = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(
                        overlappingPlayer("player-b", TeamSide.B),
                        overlappingPlayer("player-a", TeamSide.A)
                ),
                List.of()
        );

        PlayerBallContactResponseEvent response = responseEvent(result);
        assertEquals("player-b", response.playerId());
        assertTiming(response, -1L);
        assertEquals(-5.0, response.outgoingVelocity().z());
    }

    @Test
    void groundImpactEmitsNoResponseTimingEvent() {
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
    }

    @Test
    void frontendPreviewReportsEarlyTimingAndLandsInOnSideA() {
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
        assertTiming(response, -3L);
        assertEquals(6.3, response.outgoingVelocity().y());
        assertEquals(-5.0, response.outgoingVelocity().z());
        assertEquals(CourtResult.IN, ground.courtResult());
        assertEquals(CourtSide.A, ground.courtSide());
    }

    private static PlayerBallContactResponseEvent responseEvent(
            BallSimulationAdvanceResult result
    ) {
        return result.events().stream()
                .filter(PlayerBallContactResponseEvent.class::isInstance)
                .map(PlayerBallContactResponseEvent.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static void assertTiming(
            PlayerBallContactResponseEvent response,
            long expectedOffsetSteps
    ) {
        assertEquals(expectedOffsetSteps, response.hitTimingOffsetSteps());
        assertEquals(
                expectedOffsetSteps * FIXED_STEP_SECONDS,
                response.hitTimingOffsetSeconds(),
                TOLERANCE
        );
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
