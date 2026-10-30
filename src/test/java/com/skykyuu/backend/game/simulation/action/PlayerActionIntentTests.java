package com.skykyuu.backend.game.simulation.action;

import com.skykyuu.backend.game.simulation.ball.BallSimulationAdvanceResult;
import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.FixedStepVolleyballSimulator;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactResponseEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballSimulationConfig;
import com.skykyuu.backend.game.simulation.ball.VolleyballState;
import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerActionIntentTests {

    @ParameterizedTest
    @ValueSource(strings = {"player-a", "player-b", " jogador/α 01 "})
    void preservesIdentityWithoutTrimmingAndAllLegacyFields(String playerId) {
        PlayerHitIntent intent = new PlayerHitIntent(playerId, true, true, 0.375, -0.25);
        assertEquals(new PlayerActionIntent(PlayerActionType.GENERIC_HIT, playerId, true, true, 0.375, -0.25),
                PlayerActionIntent.fromHitIntent(intent));
        assertEquals(new PlayerHitIntent(playerId, true, true, 0.375, -0.25), intent);
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "true,true", "false,true"})
    void preservesHeldAndPressedWithoutDecidingHitValidity(boolean held, boolean pressed) {
        PlayerActionIntent action = PlayerActionIntent.fromHitIntent(new PlayerHitIntent("player", held, pressed));
        assertEquals(held, action.hitHeld());
        assertEquals(pressed, action.hitPressed());
    }

    @ParameterizedTest
    @MethodSource("localAimCases")
    void preservesF31LocalAnalogValuesWithoutWorldConversion(double lateral, double forward) {
        PlayerHitIntent intent = new PlayerHitIntent("player", true, true, lateral, forward);
        PlayerActionIntent action = PlayerActionIntent.fromHitIntent(intent);
        assertEquals(lateral, action.aimLateral());
        assertEquals(forward, action.aimForward());
        assertEquals(Double.doubleToRawLongBits(lateral), Double.doubleToRawLongBits(action.aimLateral()));
        assertEquals(Double.doubleToRawLongBits(forward), Double.doubleToRawLongBits(action.aimForward()));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void keepsNumericValidationInExistingHitFlowForDirectAndAdaptedActions(double value) {
        PlayerActionIntent direct = new PlayerActionIntent(PlayerActionType.GENERIC_HIT,
                "player", false, false, value, value);
        PlayerActionIntent adapted = PlayerActionIntent.fromHitIntent(
                new PlayerHitIntent("player", false, false, value, value));
        assertEquals(value, direct.aimLateral());
        assertEquals(value, direct.aimForward());
        assertEquals(direct, adapted);
    }

    @Test
    void isAnImmutableRecordWithOnlyTheSixApprovedComponents() {
        assertTrue(PlayerActionIntent.class.isRecord());
        assertTrue(Modifier.isFinal(PlayerActionIntent.class.getModifiers()));
        assertEquals(List.of("actionType", "playerId", "hitHeld", "hitPressed", "aimLateral", "aimForward"),
                Arrays.stream(PlayerActionIntent.class.getRecordComponents()).map(component -> component.getName()).toList());
        assertTrue(Arrays.stream(PlayerActionIntent.class.getDeclaredFields())
                .allMatch(field -> Modifier.isPrivate(field.getModifiers()) && Modifier.isFinal(field.getModifiers())));
    }

    @Test
    void conversionIsDeterministicWithoutSharedStateOrSourceMutation() {
        PlayerHitIntent first = new PlayerHitIntent("first", true, true, 0.375, -0.25);
        PlayerHitIntent second = new PlayerHitIntent("second", false, false, -1.0, 1.0);
        PlayerActionIntent original = PlayerActionIntent.fromHitIntent(first);
        assertEquals("second", PlayerActionIntent.fromHitIntent(second).playerId());
        PlayerActionIntent repeated = PlayerActionIntent.fromHitIntent(first);
        assertEquals(original, repeated);
        assertNotSame(original, repeated);
        assertEquals(new PlayerHitIntent("first", true, true, 0.375, -0.25), first);
        assertEquals(new PlayerHitIntent("second", false, false, -1.0, 1.0), second);
    }

    @Test
    void preservesBothLegacyHitIntentConstructors() {
        assertEquals(new PlayerActionIntent(PlayerActionType.GENERIC_HIT, "player", false, false, 0.0, 0.0),
                PlayerActionIntent.fromHitIntent(new PlayerHitIntent("player", false, false)));
        assertEquals(new PlayerActionIntent(PlayerActionType.GENERIC_HIT, "player", true, true, -0.75, 0.0),
                PlayerActionIntent.fromHitIntent(new PlayerHitIntent("player", true, true, -0.75)));
    }

    @Test
    void rejectsNullActionType() {
        assertThrows(NullPointerException.class,
                () -> new PlayerActionIntent(null, "player", false, false, 0.0, 0.0));
    }

    @Test
    void rejectsNullPlayerIdWithLegacyIdentityContract() {
        assertThrows(NullPointerException.class,
                () -> new PlayerActionIntent(PlayerActionType.GENERIC_HIT, null, false, false, 0.0, 0.0));
        assertThrows(NullPointerException.class, () -> new PlayerHitIntent(null, false, false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\r\n", "\u2003"})
    void rejectsBlankPlayerIdWithLegacyIdentityContract(String playerId) {
        assertThrows(IllegalArgumentException.class,
                () -> new PlayerActionIntent(PlayerActionType.GENERIC_HIT, playerId, false, false, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new PlayerHitIntent(playerId, false, false));
    }

    @Test
    void rejectsMissingSourceIntent() {
        assertThrows(NullPointerException.class, () -> PlayerActionIntent.fromHitIntent(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"A", "B"})
    void adaptationLeavesLegacySimulationAndLocalAimUnchangedForBothTeams(String teamName) {
        TeamSide team = TeamSide.valueOf(teamName);
        assertEquals(run(team, false), run(team, true));
    }

    private static SimulationResult run(TeamSide team, boolean adapt) {
        PlayerHitIntent intent = new PlayerHitIntent("player", true, true,
                0.7071067811865476, -0.7071067811865476);
        if (adapt) {
            PlayerActionIntent action = PlayerActionIntent.fromHitIntent(intent);
            assertEquals(0.7071067811865476, action.aimLateral());
            assertEquals(-0.7071067811865476, action.aimForward());
        }
        FixedStepVolleyballSimulator simulator = new FixedStepVolleyballSimulator(new VolleyballState(
                new BallVector3(0.0, 1.0, 0.0), new BallVector3(0.25, 0.0, 0.0)));
        double step = VolleyballSimulationConfig.FIXED_STEP_SECONDS;
        BallSimulationAdvanceResult result = simulator.advance(step, List.of(new PlayerBallContactTarget(
                "player", team, new BallVector3(0.0, 0.0, 0.0))), List.of(intent));
        PlayerBallContactResponseEvent event = result.events().stream()
                .filter(PlayerBallContactResponseEvent.class::isInstance)
                .map(PlayerBallContactResponseEvent.class::cast).findFirst().orElseThrow();
        double sign = team == TeamSide.A ? 1.0 : -1.0;
        assertEquals(new BallVector3(0.25 + sign * 0.7071067811865476 * 3.0, 6.3,
                sign * 5.0 - sign * 0.7071067811865476 * 2.0), event.outgoingVelocity());
        VolleyballState responseState = simulator.getState();
        BallSimulationAdvanceResult following = simulator.advance(step);
        return new SimulationResult(result, responseState, following, simulator.getState());
    }

    private static Stream<Arguments> localAimCases() {
        return Stream.of(
                Arguments.of(-1.0, 1.0), Arguments.of(0.0, 0.0), Arguments.of(1.0, -1.0),
                Arguments.of(-0.0, -0.0), Arguments.of(0.375, -0.25),
                Arguments.of(-0.7071067811865476, 0.7071067811865476),
                Arguments.of(0.7071067811865476, -0.7071067811865476));
    }

    private record SimulationResult(BallSimulationAdvanceResult response, VolleyballState responseState,
                                    BallSimulationAdvanceResult following, VolleyballState followingState) {
    }
}
