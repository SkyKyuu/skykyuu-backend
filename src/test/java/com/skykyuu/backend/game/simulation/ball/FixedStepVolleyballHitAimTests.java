package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStepVolleyballHitAimTests {

    private static final double FIXED_STEP_SECONDS =
            VolleyballSimulationConfig.FIXED_STEP_SECONDS;

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 0.0, 0.375, 1.0})
    void directPerfectResponseCarriesAimWithoutChangingPhysics(double aimLateral) {
        FixedStepVolleyballSimulator simulator = newSimulator();

        PlayerBallContactResponseEvent response = responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-b", TeamSide.B)),
                List.of(pressedIntent("player-b", aimLateral))
        ));

        assertEquals(aimLateral, response.hitAimLateral());
        assertEquals(PlayerHitTimingGrade.PERFECT, response.hitTimingGrade());
        assertEquals(1.0, response.hitTimingForwardMultiplier());
        assertEquals(new BallVector3(0.25, 6.3, -5.0), response.outgoingVelocity());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 0.0, 1.0})
    void bufferedEarlyResponseCarriesAimWithoutChangingTimingPower(double aimLateral) {
        FixedStepVolleyballSimulator simulator = newSimulator();

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer("player-b", TeamSide.B)),
                List.of(pressedIntent("player-b", aimLateral))
        );
        PlayerBallContactResponseEvent response = responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-b", TeamSide.B)),
                List.of()
        ));

        assertEquals(aimLateral, response.hitAimLateral());
        assertEquals(PlayerHitTimingGrade.EARLY, response.hitTimingGrade());
        assertEquals(0.90, response.hitTimingForwardMultiplier());
        assertEquals(new BallVector3(0.25, 6.3, -4.5), response.outgoingVelocity());
    }

    @Test
    void heldAimChangesDoNotReplaceLatchedAim() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a", -0.75))
        );
        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(heldIntent("player-a", 1.0))
        );
        PlayerBallContactResponseEvent response = responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of(heldIntent("player-a", 1.0))
        ));

        assertEquals(-0.75, response.hitAimLateral());
    }

    @Test
    void zeroStepAdvancePreservesLatchedAimForLaterResponse() {
        FixedStepVolleyballSimulator simulator = newSimulator();

        BallSimulationAdvanceResult arm = simulator.advance(
                0.0,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a", 0.5))
        );
        PlayerBallContactResponseEvent response = responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        ));

        assertEquals(0, arm.executedSteps());
        assertEquals(0.5, response.hitAimLateral());
        assertEquals(0L, response.hitTimingOffsetSteps());
    }

    @Test
    void legitimateRepressReplacesAimAndTimingState() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a", -1.0))
        );
        simulator.advance(
                0.0,
                List.of(farPlayer),
                List.of(new PlayerHitIntent("player-a", false, false, -1.0))
        );
        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a", 1.0))
        );
        PlayerBallContactResponseEvent response = responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        ));

        assertEquals(1.0, response.hitAimLateral());
        assertEquals(-1L, response.hitTimingOffsetSteps());
    }

    @Test
    void expiredAimDoesNotRespondAndNewHitUsesOnlyNewAim() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);
        PlayerBallContactTarget overlappingPlayer =
                overlappingPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farPlayer),
                List.of(pressedIntent("player-a", -0.75))
        );
        advanceWithoutHit(simulator, farPlayer, 5);
        BallSimulationAdvanceResult expiredEntry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        );
        simulator.advance(FIXED_STEP_SECONDS, List.of(farPlayer), List.of());
        simulator.advance(
                0.0,
                List.of(farPlayer),
                List.of(pressedIntent("player-a", 0.5))
        );
        PlayerBallContactResponseEvent newResponse = responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        ));

        assertNoResponse(expiredEntry);
        assertEquals(0.5, newResponse.hitAimLateral());
    }

    @Test
    void hitDuringRespondedOverlapDiscardsAimBeforeReentry() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget overlappingPlayer =
                overlappingPlayer("player-a", TeamSide.A);
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a", -1.0))
        );
        BallSimulationAdvanceResult discarded = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a", 1.0))
        );
        simulator.advance(FIXED_STEP_SECONDS, List.of(farPlayer), List.of());
        BallSimulationAdvanceResult reentry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        );

        assertNoResponse(discarded);
        assertNoResponse(reentry);
    }

    @Test
    void zeroStepHitDuringRespondedOverlapCannotLeaveStaleAim() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget overlappingPlayer =
                overlappingPlayer("player-a", TeamSide.A);
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a", -1.0))
        );
        BallSimulationAdvanceResult discarded = simulator.advance(
                0.0,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a", 1.0))
        );
        simulator.advance(FIXED_STEP_SECONDS, List.of(farPlayer), List.of());
        BallSimulationAdvanceResult reentry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        );

        assertEquals(0, discarded.executedSteps());
        assertNoResponse(discarded);
        assertNoResponse(reentry);
    }

    @Test
    void newHitAfterObservedLeaveUsesNewAim() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget overlappingPlayer =
                overlappingPlayer("player-a", TeamSide.A);
        PlayerBallContactTarget farPlayer = farPlayer("player-a", TeamSide.A);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent("player-a", -1.0))
        );
        simulator.advance(FIXED_STEP_SECONDS, List.of(farPlayer), List.of());
        simulator.advance(
                0.0,
                List.of(farPlayer),
                List.of(pressedIntent("player-a", 0.5))
        );
        PlayerBallContactResponseEvent response = responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of()
        ));

        assertEquals(0.5, response.hitAimLateral());
    }

    @Test
    void multiplePlayersUseAimFromRespondingTargetOnly() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farA = farPlayer("player-a", TeamSide.A);
        PlayerBallContactTarget farB = farPlayer("player-b", TeamSide.B);

        simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(farA, farB),
                List.of(
                        pressedIntent("player-a", -1.0),
                        pressedIntent("player-b", 0.25)
                )
        );
        PlayerBallContactResponseEvent response = responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(
                        overlappingPlayer("player-b", TeamSide.B),
                        overlappingPlayer("player-a", TeamSide.A)
                ),
                List.of()
        ));

        assertEquals("player-b", response.playerId());
        assertEquals(0.25, response.hitAimLateral());
    }

    @Test
    void resetClearsLatchedAimWithBufferedHit() {
        VolleyballState initialState = initialState();
        FixedStepVolleyballSimulator simulator =
                new FixedStepVolleyballSimulator(initialState);

        simulator.advance(
                0.0,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a", -0.75))
        );
        simulator.reset(initialState);
        BallSimulationAdvanceResult entry = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertNoResponse(entry);
    }

    @Test
    void groundImpactRemainsTerminalWithLatchedAim() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(
                new VolleyballState(
                        new BallVector3(0.0, VolleyballConfig.RADIUS_METERS + 0.01, 0.0),
                        new BallVector3(0.0, -1.0, 0.0)
                )
        );

        simulator.advance(
                0.0,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(pressedIntent("player-a", -0.75))
        );
        BallSimulationAdvanceResult impact = simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer("player-a", TeamSide.A)),
                List.of()
        );

        assertTrue(simulator.hasGroundContactOccurred());
        assertInstanceOf(BallGroundContactEvent.class, impact.events().get(1));
        assertNoResponse(impact);
    }

    @ParameterizedTest
    @ValueSource(doubles = {
            -1.01,
            1.01,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    void rejectsInvalidAimWhenHitPressedWouldLatch(double aimLateral) {
        FixedStepVolleyballSimulator simulator = newSimulator();

        assertThrows(
                IllegalArgumentException.class,
                () -> simulator.advance(
                        0.0,
                        List.of(farPlayer("player-a", TeamSide.A)),
                        List.of(pressedIntent("player-a", aimLateral))
                )
        );
    }

    @Test
    void doesNotConsumeOrValidateAimWhenHitIsNotPressed() {
        FixedStepVolleyballSimulator simulator = newSimulator();

        assertDoesNotThrow(() -> simulator.advance(
                0.0,
                List.of(farPlayer("player-a", TeamSide.A)),
                List.of(new PlayerHitIntent(
                        "player-a",
                        false,
                        false,
                        Double.NaN
                ))
        ));
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

    private static void assertNoResponse(BallSimulationAdvanceResult result) {
        assertFalse(result.events().stream().anyMatch(
                PlayerBallContactResponseEvent.class::isInstance
        ));
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

    private static FixedStepVolleyballSimulator newSimulator() {
        return new FixedStepVolleyballSimulator(initialState());
    }

    private static VolleyballState initialState() {
        return new VolleyballState(
                new BallVector3(0.0, 1.0, 0.0),
                new BallVector3(0.25, 0.0, 0.0)
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

    private static PlayerBallContactTarget farPlayer(
            String playerId,
            TeamSide teamSide
    ) {
        return new PlayerBallContactTarget(
                playerId,
                teamSide,
                new BallVector3(10.0, 0.0, 0.0)
        );
    }

    private static PlayerHitIntent pressedIntent(
            String playerId,
            double aimLateral
    ) {
        return new PlayerHitIntent(playerId, true, true, aimLateral);
    }

    private static PlayerHitIntent heldIntent(
            String playerId,
            double aimLateral
    ) {
        return new PlayerHitIntent(playerId, true, false, aimLateral);
    }
}
