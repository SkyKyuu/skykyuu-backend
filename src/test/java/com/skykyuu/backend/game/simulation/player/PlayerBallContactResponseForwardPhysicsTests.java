package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballState;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerBallContactResponseForwardPhysicsTests {

    @ParameterizedTest(name = "{0}, team {2}, raw forward {3}: canonical speed {4}")
    @MethodSource("canonicalCases")
    void matchesF218CanonicalMatrixAndIgnoresIncomingZ(
            PlayerHitTimingGrade grade, double accuracy, TeamSide team, double raw, double speed
    ) {
        BallVector3 reference = null;
        for (double incomingZ : new double[]{-100.0, 0.0, 100.0}) {
            BallVector3 incoming = new BallVector3(0.25, -2.0, incomingZ);
            BallVector3 outgoing = PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                    incoming, team, grade, 0.375, accuracy, raw);
            BallVector3 neutral = PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                    incoming, team, grade, 0.375, accuracy);
            assertEquals(team == TeamSide.A ? speed : -speed, outgoing.z(), 1.0e-12);
            assertEquals(neutral.x(), outgoing.x());
            assertEquals(6.3, outgoing.y());
            if (reference != null) assertEquals(reference, outgoing);
            reference = outgoing;
        }
    }

    @ParameterizedTest
    @MethodSource("timingCases")
    void neutralMatchesB16AndEveryLegacyOverload(
            PlayerHitTimingGrade grade, double power, double accuracy, double[] speeds
    ) {
        BallVector3 incoming = new BallVector3(0.25, -2.0, 100.0);
        for (TeamSide team : TeamSide.values()) {
            double sign = team == TeamSide.A ? 1.0 : -1.0;
            BallVector3 expected = new BallVector3(0.25 + sign * (0.375 * accuracy) * 3.0,
                    6.3, sign * (5.0 * power));
            BallVector3 legacy = PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                    incoming, team, grade, 0.375, accuracy);
            assertEquals(expected, legacy);
            assertEquals(legacy, PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                    incoming, team, grade, 0.375));
            for (double zero : new double[]{0.0, -0.0}) {
                assertEquals(legacy, PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        incoming, team, grade, 0.375, accuracy, zero));
            }
            assertEquals(new BallVector3(0.25, 6.3, sign * (5.0 * power)),
                    PlayerBallContactResponseMath.getPlayerContactResponseVelocity(incoming, team, grade));
            PlayerBallContactEvent contact = new PlayerBallContactEvent(
                    "player", team, new BallVector3(0.0, 1.0, 0.0), incoming, new BallVector3(0.0, 0.0, 0.0));
            VolleyballState state = new VolleyballState(contact.ballPosition(), incoming);
            VolleyballState expectedState = new VolleyballState(state.position(), expected);
            assertEquals(expectedState, PlayerBallContactResponseMath.applyPlayerContactResponse(
                    state, contact, grade, 0.375, accuracy));
            assertEquals(expectedState, PlayerBallContactResponseMath.applyPlayerContactResponse(
                    state, contact, grade, 0.375));
            assertEquals(expectedState, PlayerBallContactResponseMath.applyPlayerContactResponse(
                    state, contact, grade, 0.375, accuracy, 0.0));
            assertEquals(new VolleyballState(state.position(), new BallVector3(0.25, 6.3, sign * (5.0 * power))),
                    PlayerBallContactResponseMath.applyPlayerContactResponse(state, contact, grade));
            if (grade == PlayerHitTimingGrade.PERFECT) {
                assertEquals(new BallVector3(0.25, 6.3, sign * 5.0),
                        PlayerBallContactResponseMath.getDefaultPlayerContactResponseVelocity(incoming, team));
                assertEquals(new VolleyballState(state.position(), new BallVector3(0.25, 6.3, sign * 5.0)),
                        PlayerBallContactResponseMath.applyPlayerContactResponse(state, contact));
            }
        }
    }

    @ParameterizedTest
    @MethodSource("timingCases")
    void preservesDirectionAndTeamSymmetryAtBothExtremes(
            PlayerHitTimingGrade grade, double power, double accuracy, double[] speeds
    ) {
        for (double raw : new double[]{-1.0, 1.0}) {
            BallVector3 incoming = new BallVector3(0.25, -2.0, -100.0);
            double teamA = PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                    incoming, TeamSide.A, grade, 0.0, accuracy, raw).z();
            double teamB = PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                    incoming, TeamSide.B, grade, 0.0, accuracy, raw).z();
            assertTrue(teamA > 0.0);
            assertTrue(teamB < 0.0);
            assertEquals(-teamA, teamB);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-0.7071067811865476, 0.7071067811865476, 0.5})
    void preservesAnalogMagnitudeAndDiagonalComposition(double raw) {
        for (TeamSide team : TeamSide.values()) {
            double sign = team == TeamSide.A ? 1.0 : -1.0;
            assertEquals(new BallVector3(0.25 + sign * 3.0, 6.3, sign * 5.0 + sign * raw * 2.0),
                    PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                            new BallVector3(0.25, -2.0, 100.0), team, PlayerHitTimingGrade.PERFECT, 1.0, 1.0, raw));
        }
    }

    @Test
    void composesCanonicalPerfectDiagonalWithoutRenormalization() {
        BallVector3 incoming = new BallVector3(0.25, -2.0, 100.0);
        assertEquals(new BallVector3(3.25, 6.3, 7.0), PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                incoming, TeamSide.A, PlayerHitTimingGrade.PERFECT, 1.0, 1.0, 1.0));
        assertEquals(new BallVector3(-2.75, 6.3, -7.0), PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                incoming, TeamSide.B, PlayerHitTimingGrade.PERFECT, 1.0, 1.0, 1.0));
    }

    @Test
    void appliesSuppliedAccuracyOnceAndPowerOnlyToBase() {
        BallVector3 incoming = new BallVector3(0.25, -2.0, 100.0);
        assertEquals(4.5 + 1.7, PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                incoming, TeamSide.A, PlayerHitTimingGrade.EARLY, 0.0, 0.85, 1.0).z());
        assertEquals(4.5 + 2.0, PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                incoming, TeamSide.A, PlayerHitTimingGrade.EARLY, 0.0, 1.0, 1.0).z());
        assertEquals(5.0 + 1.7, PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                incoming, TeamSide.A, PlayerHitTimingGrade.PERFECT, 0.0, 0.85, 1.0).z());
    }

    @Test
    void appliesForwardResponseWithoutChangingPositionOrSourceState() {
        VolleyballState state = new VolleyballState(new BallVector3(1.0, 2.0, 3.0), new BallVector3(0.25, -2.0, 100.0));
        PlayerBallContactEvent contact = new PlayerBallContactEvent(
                "player", TeamSide.B, state.position(), state.velocity(), new BallVector3(1.0, 0.0, 3.0));
        assertEquals(new VolleyballState(state.position(), new BallVector3(-2.75, 6.3, -7.0)),
                PlayerBallContactResponseMath.applyPlayerContactResponse(
                        state, contact, PlayerHitTimingGrade.PERFECT, 1.0, 1.0, 1.0));
        assertEquals(new BallVector3(0.25, -2.0, 100.0), state.velocity());
        assertEquals(state.velocity(), contact.ballVelocity());
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidRawForward(double raw) {
        assertThrows(IllegalArgumentException.class, () -> PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                new BallVector3(0.0, 0.0, 0.0), TeamSide.A, PlayerHitTimingGrade.PERFECT, 0.0, 1.0, raw));
    }

    private static Stream<Arguments> canonicalCases() {
        return timingCases().flatMap(timing -> Stream.of(TeamSide.values()).flatMap(team -> {
            double[] speeds = (double[]) timing.get()[3];
            return Stream.of(-1.0, 0.0, 1.0).map(raw -> Arguments.of(
                    timing.get()[0], timing.get()[2], team, raw, speeds[(int) (raw + 1.0)]));
        }));
    }

    private static Stream<Arguments> timingCases() {
        return Stream.of(
                Arguments.of(PlayerHitTimingGrade.VERY_EARLY, 0.75, 0.60, new double[]{2.55, 3.75, 4.95}),
                Arguments.of(PlayerHitTimingGrade.EARLY, 0.90, 0.85, new double[]{2.8, 4.5, 6.2}),
                Arguments.of(PlayerHitTimingGrade.PERFECT, 1.00, 1.00, new double[]{3.0, 5.0, 7.0}),
                Arguments.of(PlayerHitTimingGrade.LATE, 0.90, 0.85, new double[]{2.8, 4.5, 6.2}),
                Arguments.of(PlayerHitTimingGrade.VERY_LATE, 0.75, 0.60, new double[]{2.55, 3.75, 4.95}));
    }
}
