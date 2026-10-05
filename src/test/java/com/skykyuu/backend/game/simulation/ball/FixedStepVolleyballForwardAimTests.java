package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.input.PlayerHitInput;
import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.input.PlayerHitIntentTracker;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStepVolleyballForwardAimTests {

    private static final double STEP = VolleyballSimulationConfig.FIXED_STEP_SECONDS;
    private static final VolleyballState INITIAL = new VolleyballState(
            new BallVector3(0.0, 1.0, 0.0), new BallVector3(0.25, 0.0, 0.0)
    );

    @ParameterizedTest(name = "Team {0}, local forward {1}")
    @MethodSource("localForwardCases")
    void preservesLocalForwardAndChangesOnlyTelemetry(TeamSide team, double forward) {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        FixedStepVolleyballSimulator neutral = new FixedStepVolleyballSimulator(INITIAL);
        PlayerBallContactResponseEvent event = response(simulator.advance(
                STEP, List.of(target("player", team, false)), List.of(intent(forward))
        ));
        PlayerBallContactResponseEvent neutralEvent = response(neutral.advance(
                STEP, List.of(target("player", team, false)), List.of(intent(0.0))
        ));
        assertEquals(forward, event.hitAimForward());
        assertEquals(neutralEvent, new PlayerBallContactResponseEvent(
                event.playerId(), event.teamSide(), event.ballPosition(), event.incomingVelocity(),
                event.outgoingVelocity(), event.hitTimingOffsetSteps(), event.hitTimingOffsetSeconds(),
                event.hitTimingGrade(), event.hitTimingForwardMultiplier(), event.hitTimingAccuracyMultiplier(),
                event.hitAimLateral(), event.hitAimWorldX(), event.hitEffectiveAimLateral(),
                event.hitEffectiveAimWorldX(), event.hitAimVelocityX()
        ));
        assertEquals(neutral.getState(), simulator.getState());
        assertTrue(pending(simulator).isEmpty());
    }

    @Test
    void capturesPressAndIgnoresHeldChangesWithExistingAccuracy() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        PlayerHitIntentTracker tracker = new PlayerHitIntentTracker();
        simulator.advance(STEP, List.of(target("player", TeamSide.B, true)), List.of(
                tracker.update(new PlayerHitInput("player", true, 0.375, 1.0))
        ));
        PlayerHitIntent held = tracker.update(new PlayerHitInput("player", true, 0.375, -1.0));
        assertFalse(held.hitPressed());
        PlayerBallContactResponseEvent event = response(simulator.advance(
                STEP, List.of(target("player", TeamSide.B, false)), List.of(held)
        ));
        assertEquals(1.0, event.hitAimForward());
        assertEquals(PlayerHitTimingGrade.EARLY, event.hitTimingGrade());
        assertEquals(0.85, event.hitTimingAccuracyMultiplier());
        assertEquals(0.85, event.hitEffectiveAimForward());
        assertEquals(-0.85, event.hitEffectiveAimWorldZ());
        assertEquals(-4.5, event.outgoingVelocity().z());
    }

    @Test
    void zeroStepBuffersForwardUntilResponse() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        assertEquals(0, simulator.advance(0.0, List.of(), List.of(intent(0.7071067811865476))).executedSteps());
        PlayerBallContactResponseEvent event = response(simulator.advance(
                STEP, List.of(target()), List.of(new PlayerHitIntent("player", true, false, 0.375, -1.0))
        ));
        assertEquals(0.7071067811865476, event.hitAimForward());
        assertEquals(0.7071067811865476, event.hitEffectiveAimForward());
        assertEquals(0.7071067811865476, event.hitEffectiveAimWorldZ());
    }

    @Test
    void repressReplacesForwardAndTimingTogether() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        PlayerHitIntentTracker tracker = new PlayerHitIntentTracker();
        simulator.advance(STEP, List.of(), List.of(tracker.update(new PlayerHitInput("player", true, 0.0, 1.0))));
        simulator.advance(0.0, List.of(), List.of(tracker.update(new PlayerHitInput("player", false, 0.0, -1.0))));
        simulator.advance(0.0, List.of(), List.of(tracker.update(new PlayerHitInput("player", true, 0.0, -1.0))));
        PlayerBallContactResponseEvent event = response(simulator.advance(STEP, List.of(target())));
        assertEquals(-1.0, event.hitAimForward());
        assertEquals(0L, event.hitTimingOffsetSteps());
    }

    @Test
    void expirationClearsForwardAndNextPressUsesNewValue() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        simulator.advance(0.0, List.of(), List.of(intent(1.0)));
        for (int step = 0; step < 6; step++) simulator.advance(STEP);
        assertTrue(pending(simulator).isEmpty());
        assertNoResponse(simulator.advance(STEP, List.of(target())));
        assertEquals(-0.5, response(simulator.advance(STEP, List.of(target()), List.of(intent(-0.5)))).hitAimForward());
    }

    @Test
    void resetClearsForwardAndPreventsStaleResponse() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        simulator.advance(0.0, List.of(), List.of(intent(1.0)));
        simulator.reset(INITIAL);
        assertTrue(pending(simulator).isEmpty());
        assertNoResponse(simulator.advance(STEP, List.of(target())));
        assertEquals(0.0, response(simulator.advance(STEP, List.of(target()), List.of(intent(0.0)))).hitAimForward());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, 0.016666666666666666})
    void sameOverlapDiscardsForwardBeforeReentry(double delta) {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        simulator.advance(STEP, List.of(target()), List.of(intent(1.0)));
        assertNoResponse(simulator.advance(delta, List.of(target()), List.of(intent(-1.0))));
        assertTrue(pending(simulator).isEmpty());
        simulator.advance(STEP, List.of(target("player", TeamSide.A, true)));
        assertNoResponse(simulator.advance(STEP, List.of(target())));
        assertEquals(0.5, response(simulator.advance(STEP, List.of(target()), List.of(intent(0.5)))).hitAimForward());
    }

    @Test
    void groundImpactClearsForwardAndRemainsTerminal() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(new VolleyballState(
                new BallVector3(0.0, VolleyballConfig.RADIUS_METERS + 0.01, 0.0),
                new BallVector3(0.0, -1.0, 0.0)
        ));
        simulator.advance(0.0, List.of(), List.of(intent(1.0)));
        BallSimulationAdvanceResult result = simulator.advance(STEP, List.of(target()));
        assertTrue(result.events().stream().anyMatch(BallGroundContactEvent.class::isInstance));
        assertNoResponse(result);
        assertTrue(pending(simulator).isEmpty());
        assertNoResponse(simulator.advance(STEP, List.of(target()), List.of(intent(-1.0))));
        assertTrue(pending(simulator).isEmpty());
    }

    @Test
    void isolatesPlayersAndConsumesOnlyRespondingForward() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        simulator.advance(0.0, List.of(), List.of(
                new PlayerHitIntent("a", true, true, 0.375, 1.0),
                new PlayerHitIntent("b", true, true, -0.375, -0.5)
        ));
        List<PlayerBallContactTarget> targets = List.of(target("a", TeamSide.A, false), target("b", TeamSide.B, false));
        assertEquals(1.0, response(simulator.advance(STEP, targets)).hitAimForward());
        assertEquals(-0.5, pending(simulator).get("b"));
        PlayerBallContactResponseEvent second = response(simulator.advance(STEP, targets));
        assertEquals("b", second.playerId());
        assertEquals(-0.5, second.hitAimForward());
        assertTrue(pending(simulator).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void invalidPressedForwardDoesNotPartiallyReplaceValidBuffer(double forward) {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        simulator.advance(0.0, List.of(), List.of(intent(1.0)));
        assertThrows(IllegalArgumentException.class, () -> simulator.advance(0.0, List.of(), List.of(intent(forward))));
        assertEquals(1.0, response(simulator.advance(STEP, List.of(target()))).hitAimForward());
    }

    @Test
    void doesNotCaptureOrValidateWithoutHitPressed() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        assertDoesNotThrow(() -> simulator.advance(0.0, List.of(), List.of(
                new PlayerHitIntent("player", true, false, 0.0, Double.NaN)
        )));
        assertTrue(pending(simulator).isEmpty());
        assertNoResponse(simulator.advance(STEP, List.of(target())));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void validatesPressedForwardEvenDuringConsumedOverlapLikeFrontend(double forward) {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        simulator.advance(STEP, List.of(target()), List.of(intent(1.0)));
        assertThrows(IllegalArgumentException.class,
                () -> simulator.advance(0.0, List.of(target()), List.of(intent(forward))));
        assertTrue(pending(simulator).isEmpty());
        assertNoResponse(simulator.advance(STEP, List.of(target())));
    }

    @Test
    void repeatedBufferedSequencesAreDeterministic() {
        assertEquals(runSequence(), runSequence());
    }

    private record SequenceResult(BallSimulationAdvanceResult result, VolleyballState state) {
    }

    private static SequenceResult runSequence() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        simulator.advance(STEP, List.of(), List.of(intent(0.7071067811865476)));
        BallSimulationAdvanceResult result = simulator.advance(STEP, List.of(target()));
        return new SequenceResult(result, simulator.getState());
    }

    private static Map<?, ?> pending(FixedStepVolleyballSimulator simulator) {
        try {
            Field field = FixedStepVolleyballSimulator.class.getDeclaredField("hitAimForwardByPlayer");
            field.setAccessible(true);
            return (Map<?, ?>) field.get(simulator);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static PlayerHitIntent intent(double forward) {
        return new PlayerHitIntent("player", true, true, 0.375, forward);
    }

    private static PlayerBallContactTarget target() {
        return target("player", TeamSide.A, false);
    }

    private static PlayerBallContactTarget target(String id, TeamSide team, boolean far) {
        return new PlayerBallContactTarget(id, team, new BallVector3(far ? 10.0 : 0.0, 0.0, 0.0));
    }

    private static PlayerBallContactResponseEvent response(BallSimulationAdvanceResult result) {
        return result.events().stream().filter(PlayerBallContactResponseEvent.class::isInstance)
                .map(PlayerBallContactResponseEvent.class::cast).findFirst().orElseThrow();
    }

    private static void assertNoResponse(BallSimulationAdvanceResult result) {
        assertFalse(result.events().stream().anyMatch(PlayerBallContactResponseEvent.class::isInstance));
    }

    private static Stream<Arguments> localForwardCases() {
        return Stream.of(TeamSide.values()).flatMap(team ->
                Stream.of(-1.0, 0.0, 1.0, -0.7071067811865476, 0.7071067811865476, 0.5)
                        .map(forward -> Arguments.of(team, forward)));
    }
}
