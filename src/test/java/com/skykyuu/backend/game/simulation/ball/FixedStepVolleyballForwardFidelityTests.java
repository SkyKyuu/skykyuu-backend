package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedStepVolleyballForwardFidelityTests {

    private static final double STEP = VolleyballSimulationConfig.FIXED_STEP_SECONDS;
    private static final VolleyballState INITIAL = new VolleyballState(
            new BallVector3(0.0, 1.0, 0.0), new BallVector3(0.25, 0.0, 0.0));

    @ParameterizedTest(name = "{2}, team {1}, forward {5}")
    @MethodSource("fidelityCases")
    void matchesF217TelemetryWithoutChangingAnyPhysics(
            long offset, TeamSide team, PlayerHitTimingGrade grade,
            double accuracy, double power, double raw
    ) {
        Run actual = run(team, offset, raw);
        Run neutral = run(team, offset, 0.0);
        PlayerBallContactResponseEvent event = actual.event();
        assertEquals(offset, event.hitTimingOffsetSteps());
        assertEquals(grade, event.hitTimingGrade());
        assertEquals(accuracy, event.hitTimingAccuracyMultiplier());
        assertEquals(power, event.hitTimingForwardMultiplier());
        assertEquals(raw, event.hitAimForward());
        assertEquals(raw * accuracy, event.hitEffectiveAimForward());
        double worldZ = raw == 0.0 ? 0.0 : (team == TeamSide.A ? raw * accuracy : -(raw * accuracy));
        assertEquals(worldZ, event.hitEffectiveAimWorldZ());
        assertEquals(neutral.event(), withoutForwardTelemetry(event));
        assertEquals(neutral.event().outgoingVelocity(), event.outgoingVelocity());
        assertEquals(team == TeamSide.A ? 5.0 * power : -5.0 * power, event.outgoingVelocity().z());
        assertEquals(neutral.responseState(), actual.responseState());
        assertEquals(neutral.nextState(), actual.nextState());
        assertEquals(actual, run(team, offset, raw));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, 1.0})
    void teamBEarlyMatchesCanonicalExamples(double raw) {
        PlayerBallContactResponseEvent event = run(TeamSide.B, -1L, raw).event();
        assertEquals(raw * 0.85, event.hitEffectiveAimForward());
        assertEquals(-raw * 0.85, event.hitEffectiveAimWorldZ());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.0})
    void canonicalizesWorldZTelemetryForBothTeams(double raw) {
        for (TeamSide team : TeamSide.values()) {
            assertEquals(Double.doubleToRawLongBits(0.0),
                    Double.doubleToRawLongBits(run(team, -1L, raw).event().hitEffectiveAimWorldZ()));
        }
    }

    @Test
    void forwardMinusOneZeroAndOneHaveExactlyIdenticalPhysics() {
        for (TeamSide team : TeamSide.values()) {
            Run backward = run(team, -1L, -1.0);
            Run neutral = run(team, -1L, 0.0);
            Run forward = run(team, -1L, 1.0);
            assertEquals(neutral.responseState(), backward.responseState());
            assertEquals(neutral.responseState(), forward.responseState());
            assertEquals(neutral.nextState(), backward.nextState());
            assertEquals(neutral.nextState(), forward.nextState());
            assertEquals(neutral.event(), withoutForwardTelemetry(backward.event()));
            assertEquals(neutral.event(), withoutForwardTelemetry(forward.event()));
        }
    }

    @Test
    void repeatedBufferedSequenceHasExactlyIdenticalEventsAndStates() {
        assertEquals(sequence(), sequence());
    }

    private static Sequence sequence() {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        List<BallSimulationEvent> events = new ArrayList<>();
        List<VolleyballState> states = new ArrayList<>();
        simulator.advance(0.0, List.of(), List.of(intent(0.7071067811865476, true)));
        events.addAll(simulator.advance(STEP).events());
        states.add(simulator.getState());
        events.addAll(simulator.advance(STEP, List.of(target(TeamSide.B)),
                List.of(intent(-1.0, false))).events());
        states.add(simulator.getState());
        events.addAll(simulator.advance(STEP).events());
        states.add(simulator.getState());
        PlayerBallContactResponseEvent event = response(events);
        assertEquals(0.7071067811865476 * 0.85, event.hitEffectiveAimForward());
        assertEquals(-(0.7071067811865476 * 0.85), event.hitEffectiveAimWorldZ());
        return new Sequence(events, states);
    }

    private static Run run(TeamSide team, long offset, double raw) {
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(INITIAL);
        PlayerBallContactTarget target = target(team);
        if (offset < 0L) {
            simulator.advance(STEP, List.of(), List.of(intent(raw, true)));
            for (long step = 1L; step < -offset; step++) simulator.advance(STEP);
        } else {
            for (long step = 0L; step < offset; step++) simulator.advance(STEP, List.of(target));
        }
        BallSimulationAdvanceResult result = simulator.advance(STEP, List.of(target),
                List.of(intent(offset < 0L ? -raw : raw, offset >= 0L)));
        VolleyballState responseState = simulator.getState();
        simulator.advance(STEP);
        return new Run(response(result.events()), responseState, simulator.getState());
    }

    private static PlayerHitIntent intent(double raw, boolean pressed) {
        return new PlayerHitIntent("player", true, pressed, 0.375, raw);
    }

    private static PlayerBallContactTarget target(TeamSide team) {
        return new PlayerBallContactTarget("player", team, new BallVector3(0.0, 0.0, 0.0));
    }

    private static PlayerBallContactResponseEvent response(List<BallSimulationEvent> events) {
        return events.stream().filter(PlayerBallContactResponseEvent.class::isInstance)
                .map(PlayerBallContactResponseEvent.class::cast).findFirst().orElseThrow();
    }

    private static PlayerBallContactResponseEvent withoutForwardTelemetry(PlayerBallContactResponseEvent event) {
        return new PlayerBallContactResponseEvent(
                event.playerId(), event.teamSide(), event.ballPosition(), event.incomingVelocity(),
                event.outgoingVelocity(), event.hitTimingOffsetSteps(), event.hitTimingOffsetSeconds(),
                event.hitTimingGrade(), event.hitTimingForwardMultiplier(), event.hitTimingAccuracyMultiplier(),
                event.hitAimLateral(), event.hitAimWorldX(), event.hitEffectiveAimLateral(),
                event.hitEffectiveAimWorldX(), event.hitAimVelocityX(), 0.0, 0.0, 0.0);
    }

    private static Stream<Arguments> fidelityCases() {
        return Stream.of(
                Arguments.of(-4L, PlayerHitTimingGrade.VERY_EARLY, 0.60, 0.75),
                Arguments.of(-1L, PlayerHitTimingGrade.EARLY, 0.85, 0.90),
                Arguments.of(0L, PlayerHitTimingGrade.PERFECT, 1.00, 1.00),
                Arguments.of(1L, PlayerHitTimingGrade.LATE, 0.85, 0.90),
                Arguments.of(4L, PlayerHitTimingGrade.VERY_LATE, 0.60, 0.75)
        ).flatMap(timing -> Stream.of(TeamSide.values()).flatMap(team ->
                Stream.of(-1.0, 0.0, 1.0, 0.5, -0.7071067811865476, 0.7071067811865476)
                        .map(raw -> Arguments.of(timing.get()[0], team, timing.get()[1],
                                timing.get()[2], timing.get()[3], raw))));
    }

    private record Run(PlayerBallContactResponseEvent event, VolleyballState responseState, VolleyballState nextState) {
    }

    private record Sequence(List<BallSimulationEvent> events, List<VolleyballState> states) {
    }
}
