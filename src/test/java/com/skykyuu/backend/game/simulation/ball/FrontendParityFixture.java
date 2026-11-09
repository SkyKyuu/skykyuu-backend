package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.action.ForearmPassIntent;
import com.skykyuu.backend.game.simulation.action.PendingPlayerAction;
import com.skykyuu.backend.game.simulation.action.PlayerActionIntent;
import com.skykyuu.backend.game.simulation.action.PlayerActionType;
import com.skykyuu.backend.game.simulation.action.PlayerCommandIntent;
import com.skykyuu.backend.game.simulation.action.SetIntent;
import com.skykyuu.backend.game.simulation.ball.ActionSimulationAdvanceResult.ActionStepObservation;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.team.TeamSide;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FrontendParityFixture {

    private static final double EPSILON = 1.0e-12;
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private FrontendParityFixture() {
    }

    record Replay(List<BallSimulationEvent> events, List<ActionStepObservation> actions, VolleyballState state) {
        Replay {
            events = List.copyOf(events);
            actions = List.copyOf(actions);
        }
    }

    record Fixture(String name, JsonNode initial, JsonNode frames) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Fixture> fixtures(String fixtures) {
        return StreamSupport.stream(JSON.readTree(fixtures).spliterator(), false)
                .map(node -> new Fixture(node.get(0).stringValue(), node.get(1), node.get(2)));
    }

    static List<Replay> assertMatches(Fixture fixture) {
        VolleyballState initial = new VolleyballState(vector(fixture.initial().get(0)), vector(fixture.initial().get(1)));
        FixedStepVolleyballSimulator sim = new FixedStepVolleyballSimulator(initial);
        List<Replay> sessions = new ArrayList<>();
        List<BallSimulationEvent> events = new ArrayList<>();
        List<ActionStepObservation> actions = new ArrayList<>();
        int frameIndex = 0;
        for (JsonNode frame : fixture.frames()) {
            String path = fixture.name() + "/frame" + frameIndex++;
            if (frame.isNull()) {
                sessions.add(new Replay(events, actions, sim.getState()));
                events.clear();
                actions.clear();
                sim.reset(initial);
                assertEquals(initial, sim.getState(), path);
                assertEquals(0L, sim.getTotalSimulationSteps(), path);
                continue;
            }

            List<PlayerBallContactTarget> targets = new ArrayList<>();
            for (JsonNode target : frame.get(1)) {
                targets.add(new PlayerBallContactTarget(
                        target.get(0).stringValue(), TeamSide.valueOf(target.get(1).stringValue()),
                        new BallVector3(target.get(2).asDouble(), target.get(3).asDouble(), target.get(4).asDouble())
                ));
            }
            List<PlayerCommandIntent> intents = new ArrayList<>();
            for (JsonNode command : frame.get(2)) {
                String id = command.get(1).stringValue();
                double lateral = command.get(2).asDouble();
                double forward = command.get(3).asDouble();
                intents.add(switch (PlayerActionType.valueOf(command.get(0).stringValue())) {
                    case GENERIC_HIT -> new PlayerActionIntent(PlayerActionType.GENERIC_HIT, id, true, true, lateral, forward);
                    case FOREARM_PASS -> new ForearmPassIntent(id, true, true, lateral, forward);
                    case SET -> new SetIntent(id, true, true, lateral, forward);
                });
            }
            ActionSimulationAdvanceResult actual = sim.advanceWithActions(
                    frame.get(0).asDouble() * VolleyballSimulationConfig.FIXED_STEP_SECONDS, targets, intents
            );
            events.addAll(actual.events());
            actions.addAll(actual.actionSteps());
            assertEquals(frame.get(3).asInt(), actual.executedSteps(), path);
            assertMatch(frame.get(4), JSON.valueToTree(actual.events().stream().map(FrontendParityFixture::event).toList()),
                    path + "/events");
            assertMatch(frame.get(5), JSON.valueToTree(actual.actionSteps().stream().map(FrontendParityFixture::observation).toList()),
                    path + "/actionSteps");
            assertMatch(frame.get(6), JSON.valueToTree(observation(actual.lastActionStep())), path + "/lastActionStep");
            VolleyballState state = sim.getState();
            assertMatch(frame.get(7), JSON.valueToTree(List.of(vector(state.position()), vector(state.velocity()))), path + "/state");
        }
        sessions.add(new Replay(events, actions, sim.getState()));
        return sessions;
    }

    private static BallVector3 vector(JsonNode value) {
        return new BallVector3(value.get(0).asDouble(), value.get(1).asDouble(), value.get(2).asDouble());
    }

    private static List<Double> vector(BallVector3 value) {
        return List.of(value.x(), value.y(), value.z());
    }

    private static List<?> capture(PendingPlayerAction action) {
        return List.of(action.actionType().name(), action.playerId(), action.pressStep(), action.aimLateral(), action.aimForward());
    }

    private static List<?> observation(ActionStepObservation observation) {
        var executed = observation.executedAction();
        return Arrays.asList(
                observation.simulationStep(), observation.selectedActions().stream().map(FrontendParityFixture::capture).toList(),
                executed == null ? null : List.of(
                        capture(executed.action()), executed.responseStep(), executed.timingGrade().name(), vector(executed.outgoingVelocity())
                )
        );
    }

    private static List<?> event(BallSimulationEvent event) {
        return switch (event) {
            case BallGroundContactEvent e -> List.of(
                    "GROUND_CONTACT", vector(e.position()), vector(e.velocity()), e.courtResult().name(), e.courtSide().name()
            );
            case PlayerBallContactEvent e -> List.of(
                    "PLAYER_CONTACT", e.playerId(), e.teamSide().name(), vector(e.ballPosition()), vector(e.ballVelocity()),
                    vector(e.playerPosition())
            );
            case PlayerBallContactResponseEvent e -> List.of(
                    "PLAYER_CONTACT_RESPONSE", e.playerId(), e.teamSide().name(), vector(e.ballPosition()),
                    vector(e.incomingVelocity()), vector(e.outgoingVelocity()), e.hitTimingOffsetSteps(),
                    e.hitTimingOffsetSeconds(), e.hitTimingGrade().name(), e.hitTimingForwardMultiplier(),
                    e.hitTimingAccuracyMultiplier(), e.hitAimLateral(), e.hitAimForward(),
                    e.hitEffectiveAimLateral(), e.hitEffectiveAimForward(), e.hitAimWorldX(),
                    e.hitEffectiveAimWorldX(), e.hitEffectiveAimWorldZ(), e.hitAimVelocityX(), e.hitAimVelocityZ()
            );
            case PlayerForearmPassResponseEvent e -> List.of(
                    "PLAYER_FOREARM_PASS_RESPONSE", e.playerId(), e.teamSide().name(), vector(e.ballPosition()),
                    vector(e.incomingVelocity()), vector(e.outgoingVelocity()), e.timingOffsetSteps(),
                    e.timingOffsetSeconds(), e.timingGrade().name(), e.timingPowerMultiplier(),
                    e.timingAccuracyMultiplier(), e.aimLateral(), e.aimForward(), e.effectiveAimLateral(),
                    e.effectiveAimForward(), e.pressStep(), e.contactStep(), e.actionType().name()
            );
            case PlayerSetResponseEvent e -> List.of(
                    "PLAYER_SET_RESPONSE", e.playerId(), e.teamSide().name(), vector(e.ballPosition()),
                    vector(e.incomingVelocity()), vector(e.outgoingVelocity()), e.timingOffsetSteps(),
                    e.timingOffsetSeconds(), e.timingGrade().name(), e.timingPowerMultiplier(),
                    e.timingAccuracyMultiplier(), e.aimLateral(), e.aimForward(), e.effectiveAimLateral(),
                    e.effectiveAimForward(), e.pressStep(), e.contactStep(), e.actionType().name()
            );
        };
    }

    private static void assertMatch(JsonNode expected, JsonNode actual, String path) {
        if (expected.isArray()) {
            assertTrue(actual.isArray(), path);
            assertEquals(expected.size(), actual.size(), path);
            for (int i = 0; i < expected.size(); i++) {
                assertMatch(expected.get(i), actual.get(i), path + "/" + i);
            }
        } else if (expected.isNumber() && actual.isNumber()) {
            if (actual.isIntegralNumber()) {
                assertTrue(expected.isIntegralNumber(), path);
                assertEquals(expected.asLong(), actual.asLong(), path);
            } else {
                assertEquals(expected.asDouble(), actual.asDouble(), EPSILON, path);
            }
        } else {
            assertEquals(expected, actual, path);
        }
    }
}
