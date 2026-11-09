package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.action.ForearmPassIntent;
import com.skykyuu.backend.game.simulation.action.PlayerActionIntent;
import com.skykyuu.backend.game.simulation.action.PlayerActionType;
import com.skykyuu.backend.game.simulation.action.PlayerCommandIntent;
import com.skykyuu.backend.game.simulation.action.SetIntent;
import com.skykyuu.backend.game.simulation.ball.ActionSimulationAdvanceResult.ActionStepObservation;
import com.skykyuu.backend.game.simulation.ball.ActionSimulationAdvanceResult.ExecutedPlayerAction;
import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static com.skykyuu.backend.game.simulation.action.PlayerActionType.FOREARM_PASS;
import static com.skykyuu.backend.game.simulation.action.PlayerActionType.GENERIC_HIT;
import static com.skykyuu.backend.game.simulation.action.PlayerActionType.SET;
import static org.junit.jupiter.api.Assertions.*;

class FixedStepVolleyballActionsTests {

    private static final double STEP = VolleyballSimulationConfig.FIXED_STEP_SECONDS;
    private static final VolleyballState INITIAL = new VolleyballState(
            new BallVector3(0, 1, 0), new BallVector3(0.25, 0, 0)
    );
    private static final VolleyballState NEAR_GROUND = new VolleyballState(
            new BallVector3(0, VolleyballConfig.RADIUS_METERS + 0.001, 0), new BallVector3(0, -1, 0)
    );
    private static final List<PlayerBallContactTarget> TARGET_A = targets(TeamSide.A);

    private static List<PlayerBallContactTarget> targets(TeamSide team) {
        return List.of(new PlayerBallContactTarget("a", team, new BallVector3(0, 0, 0)));
    }

    private static PlayerCommandIntent command(PlayerActionType action, double lateral, double forward) {
        return switch (action) {
            case GENERIC_HIT -> PlayerActionIntent.fromHitIntent(new PlayerHitIntent("a", true, true, lateral, forward));
            case FOREARM_PASS -> new ForearmPassIntent("a", true, true, lateral, forward);
            case SET -> new SetIntent("a", true, true, lateral, forward);
        };
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1.01, -1.01})
    void invalidBatchLeavesPriorCapturesAndPhysicalStateUnchanged(double invalid) {
        var sim = new FixedStepVolleyballSimulator(INITIAL);
        var armed = sim.advanceWithActions(0, TARGET_A, List.of(command(FOREARM_PASS, 0.25, -0.5)));
        assertEquals(0, armed.executedSteps());
        assertTrue(armed.events().isEmpty());
        assertTrue(armed.actionSteps().isEmpty());
        var captured = armed.lastActionStep().selectedActions().getFirst();

        assertThrows(IllegalArgumentException.class, () -> sim.advanceWithActions(
                STEP, TARGET_A, List.of(command(FOREARM_PASS, 0.75, 0.75), command(GENERIC_HIT, invalid, 0))
        ));
        assertThrows(IllegalArgumentException.class, () -> sim.advanceWithActions(
                STEP, TARGET_A, List.of(command(GENERIC_HIT, 0, 0), command(FOREARM_PASS, 0, invalid))
        ));
        assertThrows(IllegalArgumentException.class, () -> sim.advanceWithActions(
                STEP, TARGET_A, List.of(new ForearmPassIntent("a", true, false, invalid, 0))
        ));
        assertEquals(INITIAL, sim.getState());
        assertEquals(0, sim.getTotalSimulationSteps());
        assertEquals(0.0, sim.getAccumulatorSeconds());
        assertSame(captured, sim.advanceWithActions(0, TARGET_A, List.of()).lastActionStep().selectedActions().getFirst());

        var response = sim.advanceWithActions(STEP, TARGET_A, List.of());
        assertSame(captured, response.lastActionStep().executedAction().action());
        assertTrue(sim.advanceWithActions(0, TARGET_A, List.of()).lastActionStep().selectedActions().isEmpty());
    }

    @Test
    void resultsExposeImmutableObservationsEventsAndSelections() {
        var sim = new FixedStepVolleyballSimulator(INITIAL);
        var result = sim.advanceWithActions(STEP, TARGET_A, List.of(command(FOREARM_PASS, 0, 0)));
        assertThrows(UnsupportedOperationException.class, () -> result.events().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.actionSteps().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.lastActionStep().selectedActions().clear());
        var response = assertInstanceOf(PlayerForearmPassResponseEvent.class, result.events().getLast());
        var velocity = response.outgoingVelocity();
        assertEquals(sim.getState().velocity(), velocity);
        sim.advanceWithActions(STEP, List.of(), List.of());
        assertEquals(new BallVector3(0.25, 5.0, 3.5), response.outgoingVelocity());
        assertEquals(velocity, result.lastActionStep().executedAction().outgoingVelocity());
    }

    @Test
    void wrongModeChangesNeitherPhysicsNorCapturesAndResetAllowsBothDirections() {
        var sim = new FixedStepVolleyballSimulator(INITIAL);
        assertThrows(IllegalArgumentException.class, () -> sim.advanceWithActions(
                STEP, TARGET_A, List.of(command(FOREARM_PASS, 1, 2))
        ));
        sim.advance(STEP / 2, TARGET_A, List.of(new PlayerHitIntent("a", true, true)));
        var before = sim.getState();
        assertThrows(IllegalStateException.class, () -> sim.advanceWithActions(
                STEP, TARGET_A, List.of(command(FOREARM_PASS, 0, 0))
        ));
        assertEquals(before, sim.getState());
        assertEquals(STEP / 2, sim.getAccumulatorSeconds());
        assertEquals(0, sim.getTotalSimulationSteps());
        assertInstanceOf(PlayerBallContactResponseEvent.class, sim.advance(STEP / 2, TARGET_A).events().getLast());

        sim.reset(INITIAL);
        var capture = sim.advanceWithActions(STEP / 2, TARGET_A, List.of(command(FOREARM_PASS, 0, 0)));
        assertEquals(0, capture.executedSteps());
        assertThrows(IllegalStateException.class, () -> sim.advance(STEP));
        assertEquals(0, sim.getTotalSimulationSteps());
        assertEquals(STEP / 2, sim.getAccumulatorSeconds());
        assertEquals(INITIAL, sim.getState());
        var actions = sim.advanceWithActions(STEP / 2, TARGET_A, List.of());
        assertSame(capture.lastActionStep().selectedActions().getFirst(), actions.lastActionStep().executedAction().action());

        sim.reset(INITIAL);
        assertEquals(0, sim.getTotalSimulationSteps());
        assertEquals(0.0, sim.getAccumulatorSeconds());
        assertEquals(List.of(PlayerBallContactEvent.class),
                sim.advance(STEP, TARGET_A).events().stream().map(Object::getClass).toList());
    }

    @Test
    void groundWinsClearsAllActionsAndResetClearsTheSession() {
        var sim = new FixedStepVolleyballSimulator(NEAR_GROUND);
        var ground = sim.advanceWithActions(3 * STEP, TARGET_A, List.of(command(GENERIC_HIT, 0, 0), command(FOREARM_PASS, 0, 0)));
        assertEquals(1, ground.executedSteps());
        assertInstanceOf(BallGroundContactEvent.class, ground.events().getLast());
        assertTrue(ground.lastActionStep().selectedActions().isEmpty());
        assertNull(ground.lastActionStep().executedAction());
        assertThrows(IllegalStateException.class, () -> sim.advance(STEP));
        assertTrue(sim.advanceWithActions(STEP, TARGET_A, List.of(command(FOREARM_PASS, 0, 0))).events().isEmpty());
        assertTrue(sim.hasGroundContactOccurred());
        sim.reset(INITIAL);
        assertFalse(sim.hasGroundContactOccurred());
        assertEquals(0, sim.getTotalSimulationSteps());
        var noCommand = sim.advanceWithActions(STEP, TARGET_A, List.of());
        assertInstanceOf(PlayerBallContactEvent.class, noCommand.events().getFirst());
        assertNull(noCommand.lastActionStep().executedAction());
        assertTrue(noCommand.lastActionStep().selectedActions().isEmpty());
        var late = sim.advanceWithActions(STEP, TARGET_A, List.of(command(FOREARM_PASS, 0, 0)));
        var response = assertInstanceOf(PlayerForearmPassResponseEvent.class, late.events().getFirst());
        assertEquals(0, response.contactStep());
        assertEquals(1, response.pressStep());
        assertEquals(PlayerHitTimingGrade.LATE, response.timingGrade());
    }

    @ParameterizedTest
    @EnumSource(TeamSide.class)
    void genericRouteMatchesLegacyInStateEventsRepressOverlapExpiryGroundAndReset(TeamSide team) {
        var pair = new LegacyComparison(INITIAL, targets(team));
        pair.frame(0, false, new PlayerHitIntent("a", true, true, -0.25, 0.375));
        pair.frame(2, false);
        pair.frame(1, false, new PlayerHitIntent("a", true, true, 0.375, -0.25));
        var first = assertInstanceOf(PlayerBallContactResponseEvent.class, pair.frame(1, true).getLast());
        assertEquals(0.375, first.hitAimLateral());
        assertEquals(-0.25, first.hitAimForward());
        assertEquals(-1, first.hitTimingOffsetSteps());
        assertTrue(pair.frame(0, true, new PlayerHitIntent("a", true, true)).isEmpty());
        assertTrue(pair.frame(1, true, new PlayerHitIntent("a", true, true)).isEmpty());
        pair.frame(1, false);
        assertEquals(1, pair.frame(1, true).size());

        for (int age : List.of(5, 6)) {
            pair.reset(INITIAL);
            pair.frame(0, false, new PlayerHitIntent("a", true, true, 0.5, -0.375));
            pair.frame(age, false);
            var events = pair.frame(1, true);
            assertEquals(age == 5 ? 2 : 1, events.size());
        }

        pair.reset(NEAR_GROUND);
        assertInstanceOf(BallGroundContactEvent.class,
                pair.frame(3, true, new PlayerHitIntent("a", true, true)).getLast());
        assertTrue(pair.frame(1, true, new PlayerHitIntent("a", true, true)).isEmpty());
        pair.reset(INITIAL);
        assertInstanceOf(PlayerBallContactEvent.class, pair.frame(1, true).getFirst());
    }

    @ParameterizedTest
    @EnumSource(TeamSide.class)
    void appliesOnlyTheSelectedSetAndPublishesThePhysicalState(TeamSide team) {
        var sim = new FixedStepVolleyballSimulator(INITIAL);
        var targets = targets(team);
        var capture = sim.advanceWithActions(0, targets,
                List.of(command(GENERIC_HIT, 0, 0), command(FOREARM_PASS, 0, 0), command(SET, 0, 0)));
        var selected = capture.lastActionStep().selectedActions().getFirst();
        assertEquals(SET, selected.actionType());
        assertEquals(0, capture.executedSteps());
        assertTrue(capture.events().isEmpty());
        var frame = sim.advanceWithActions(STEP, targets, List.of());
        assertEquals(2, frame.events().size());
        var contact = assertInstanceOf(PlayerBallContactEvent.class, frame.events().getFirst());
        var event = assertInstanceOf(PlayerSetResponseEvent.class, frame.events().getLast());
        assertEquals(team, event.teamSide());
        assertEquals(SET, event.actionType());
        assertEquals(contact.ballPosition(), event.ballPosition());
        assertEquals(contact.ballVelocity(), event.incomingVelocity());
        assertEquals(sim.getState().velocity(), event.outgoingVelocity());
        assertEquals(sim.getState().position(), event.ballPosition());
        assertEquals(new BallVector3(0.0625, 7.0, team == TeamSide.A ? 0.8 : -0.8), event.outgoingVelocity());
        var execution = frame.lastActionStep().executedAction();
        assertSame(selected, execution.action());
        assertEquals(event.outgoingVelocity(), execution.outgoingVelocity());
        assertEquals(event.timingGrade(), execution.timingGrade());
        var remaining = sim.advanceWithActions(0, targets, List.of()).lastActionStep().selectedActions();
        assertEquals(List.of(FOREARM_PASS), remaining.stream().map(action -> action.actionType()).toList());
        assertThrows(UnsupportedOperationException.class, () -> frame.events().clear());
        assertTrue(sim.advanceWithActions(STEP, targets, List.of(command(SET, 1, 1))).events().isEmpty());
        assertEquals(new BallVector3(0.0625, 7.0, team == TeamSide.A ? 0.8 : -0.8), event.outgoingVelocity());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsInvalidSetAimAtomicallyEvenWithoutAPressEdge(boolean pressed) {
        var sim = new FixedStepVolleyballSimulator(INITIAL);
        var before = sim.advanceWithActions(0, TARGET_A, List.of(command(SET, 0.5, -0.25)));
        var selected = before.lastActionStep().selectedActions().getFirst();
        assertThrows(IllegalArgumentException.class, () -> sim.advanceWithActions(
                STEP, TARGET_A, List.of(command(SET, 1, 1), new SetIntent("a", true, pressed, 0, Double.NaN))
        ));
        assertEquals(INITIAL, sim.getState());
        assertEquals(0, sim.getTotalSimulationSteps());
        assertEquals(0.0, sim.getAccumulatorSeconds());
        var after = sim.advanceWithActions(STEP, TARGET_A, List.of());
        assertSame(selected, after.lastActionStep().executedAction().action());
        var response = assertInstanceOf(PlayerSetResponseEvent.class, after.events().getLast());
        assertEquals(0.5, response.aimLateral());
        assertEquals(-0.25, response.aimForward());
    }

    @Test
    void groundTakesPriorityOverSetAndChangingModeStillRequiresReset() {
        var sim = new FixedStepVolleyballSimulator(NEAR_GROUND);
        var result = sim.advanceWithActions(STEP, TARGET_A, List.of(command(SET, 0, 0)));
        assertEquals(List.of(PlayerBallContactEvent.class, BallGroundContactEvent.class),
                result.events().stream().map(Object::getClass).toList());
        assertTrue(result.lastActionStep().selectedActions().isEmpty());
        assertNull(result.lastActionStep().executedAction());
        assertThrows(IllegalStateException.class, () -> sim.advance(STEP));
        assertEquals(0, sim.advanceWithActions(STEP, TARGET_A, List.of(command(SET, 0, 0))).executedSteps());

        sim.reset(INITIAL);
        assertInstanceOf(PlayerBallContactResponseEvent.class,
                sim.advance(STEP, TARGET_A, List.of(new PlayerHitIntent("a", true, true))).events().getLast());
        sim.reset(INITIAL);
        assertNull(sim.advanceWithActions(STEP, TARGET_A, List.of()).lastActionStep().executedAction());
    }

    private static final class LegacyComparison {
        private final FixedStepVolleyballSimulator legacy;
        private final FixedStepVolleyballSimulator actions;
        private final List<PlayerBallContactTarget> targets;

        LegacyComparison(VolleyballState initial, List<PlayerBallContactTarget> targets) {
            legacy = new FixedStepVolleyballSimulator(initial);
            actions = new FixedStepVolleyballSimulator(initial);
            this.targets = targets;
        }

        List<BallSimulationEvent> frame(int steps, boolean contact, PlayerHitIntent... intents) {
            var frameTargets = contact ? targets : List.<PlayerBallContactTarget>of();
            var expected = legacy.advance(steps * STEP, frameTargets, List.of(intents));
            var actual = actions.advanceWithActions(
                    steps * STEP, frameTargets, Stream.of(intents).map(PlayerActionIntent::fromHitIntent).toList()
            );
            assertEquals(expected.executedSteps(), actual.executedSteps());
            assertEquals(expected.events(), actual.events());
            assertEquals(legacy.getState(), actions.getState());
            assertEquals(legacy.getAccumulatorSeconds(), actions.getAccumulatorSeconds());
            assertEquals(legacy.getTotalSimulationSteps(), actions.getTotalSimulationSteps());
            return actual.events();
        }

        void reset(VolleyballState initial) {
            legacy.reset(initial);
            actions.reset(initial);
            assertEquals(legacy.getState(), actions.getState());
            assertEquals(legacy.getTotalSimulationSteps(), actions.getTotalSimulationSteps());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "GENERIC_HIT, , 0, GENERIC_HIT",
            "FOREARM_PASS, , 0, FOREARM_PASS",
            "GENERIC_HIT, FOREARM_PASS, 0, FOREARM_PASS",
            "GENERIC_HIT, FOREARM_PASS, 4, GENERIC_HIT",
            "FOREARM_PASS, GENERIC_HIT, 4, FOREARM_PASS",
            "GENERIC_HIT, FOREARM_PASS, 6, FOREARM_PASS"
    })
    void replaysEquivalentLogicalCapturesAt30_60_120Fps(
            PlayerActionType first, PlayerActionType second, int entryStep, PlayerActionType winner
    ) {
        List<ScheduledCommand> commands = new ArrayList<>();
        commands.add(new ScheduledCommand(0, first));
        if (second != null) {
            commands.add(new ScheduledCommand(entryStep == 0 ? 0 : 2, second));
        }
        var at60 = replay(60, commands, entryStep, winner);
        assertEquals(at60, replay(30, commands, entryStep, winner));
        assertEquals(at60, replay(120, commands, entryStep, winner));
    }

    private static List<Replay> replay(int fps, List<ScheduledCommand> commands, int entryStep, PlayerActionType winner) {
        var sim = new FixedStepVolleyballSimulator(INITIAL);
        var beforeReset = session(sim, fps, commands, entryStep, winner);
        sim.reset(INITIAL);
        var afterReset = session(sim, fps, List.of(new ScheduledCommand(2, FOREARM_PASS)), 2, FOREARM_PASS);
        assertTrue(afterReset.observations().getFirst().selectedActions().isEmpty());
        return List.of(beforeReset, afterReset);
    }

    private static Replay session(
            FixedStepVolleyballSimulator sim, int fps, List<ScheduledCommand> commands, int entryStep, PlayerActionType winner
    ) {
        List<ActionStepObservation> observations = new ArrayList<>();
        List<BallSimulationEvent> events = new ArrayList<>();
        long lastSampledStep = -1;
        for (int frame = 0; frame < fps / 5; frame++) {
            long step = sim.getTotalSimulationSteps();
            var intents = step == lastSampledStep ? List.<PlayerCommandIntent>of() : commands.stream()
                    .filter(command -> command.step() == step)
                    .map(command -> command(command.type(), 0.375, -0.25)).toList();
            lastSampledStep = step;
            var result = sim.advanceWithActions(1.0 / fps, step >= entryStep ? TARGET_A : List.of(), intents);
            observations.addAll(result.actionSteps());
            events.addAll(result.events());
        }
        assertEquals(12, sim.getTotalSimulationSteps());
        List<ExecutedPlayerAction> executed = observations.stream().map(ActionStepObservation::executedAction)
                .filter(java.util.Objects::nonNull).toList();
        assertEquals(1, executed.size());
        assertEquals(entryStep, executed.getFirst().responseStep());
        assertEquals(winner, executed.getFirst().action().actionType());
        return new Replay(observations, events, sim.getState());
    }

    private record ScheduledCommand(long step, PlayerActionType type) {
    }

    private record Replay(List<ActionStepObservation> observations, List<BallSimulationEvent> events, VolleyballState state) {
    }
}
