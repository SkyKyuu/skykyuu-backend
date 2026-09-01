package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballState;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerBallContactResponseAimPhysicsTests {

    private static final BallVector3 INCOMING_VELOCITY =
            new BallVector3(0.25, -2.0, 9.0);

    @ParameterizedTest(name = "Team {0}, aim {1} produces velocity X {2}")
    @MethodSource("perfectResponseCases")
    void appliesExactLateralAimToPerfectResponse(
            TeamSide teamSide,
            double aimLateral,
            double expectedVelocityX
    ) {
        BallVector3 outgoing =
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        INCOMING_VELOCITY,
                        teamSide,
                        PlayerHitTimingGrade.PERFECT,
                        aimLateral
                );

        assertEquals(expectedVelocityX, outgoing.x());
        assertEquals(6.3, outgoing.y());
        assertEquals(teamSide == TeamSide.A ? 5.0 : -5.0, outgoing.z());
    }

    @ParameterizedTest(name = "Team B +1 aim with {0}")
    @MethodSource("teamBTimingCases")
    void timingGradeChangesForwardVelocityWithoutChangingLateralAim(
            PlayerHitTimingGrade grade,
            double expectedForwardVelocity
    ) {
        BallVector3 outgoing =
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        INCOMING_VELOCITY,
                        TeamSide.B,
                        grade,
                        1.0
                );

        assertEquals(-2.75, outgoing.x());
        assertEquals(6.3, outgoing.y());
        assertEquals(expectedForwardVelocity, outgoing.z());
    }

    @ParameterizedTest
    @EnumSource(TeamSide.class)
    void timingAwareOverloadsRemainEquivalentToNeutralAim(TeamSide teamSide) {
        PlayerHitTimingGrade grade = PlayerHitTimingGrade.EARLY;
        VolleyballState state = state();
        PlayerBallContactEvent contact = contact(teamSide);

        assertEquals(
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        INCOMING_VELOCITY,
                        teamSide,
                        grade,
                        0.0
                ),
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        INCOMING_VELOCITY,
                        teamSide,
                        grade
                )
        );
        assertEquals(
                PlayerBallContactResponseMath.applyPlayerContactResponse(
                        state,
                        contact,
                        grade,
                        0.0
                ),
                PlayerBallContactResponseMath.applyPlayerContactResponse(
                        state,
                        contact,
                        grade
                )
        );
    }

    @ParameterizedTest
    @EnumSource(TeamSide.class)
    void defaultOverloadsRemainEquivalentToPerfectNeutralAim(TeamSide teamSide) {
        VolleyballState state = state();
        PlayerBallContactEvent contact = contact(teamSide);

        assertEquals(
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        INCOMING_VELOCITY,
                        teamSide,
                        PlayerHitTimingGrade.PERFECT,
                        0.0
                ),
                PlayerBallContactResponseMath.getDefaultPlayerContactResponseVelocity(
                        INCOMING_VELOCITY,
                        teamSide
                )
        );
        assertEquals(
                PlayerBallContactResponseMath.applyPlayerContactResponse(
                        state,
                        contact,
                        PlayerHitTimingGrade.PERFECT,
                        0.0
                ),
                PlayerBallContactResponseMath.applyPlayerContactResponse(state, contact)
        );
    }

    private static Stream<Arguments> perfectResponseCases() {
        return Stream.of(
                Arguments.of(TeamSide.A, -1.0, -2.75),
                Arguments.of(TeamSide.A, 0.0, 0.25),
                Arguments.of(TeamSide.A, 0.5, 1.75),
                Arguments.of(TeamSide.A, 1.0, 3.25),
                Arguments.of(TeamSide.B, -1.0, 3.25),
                Arguments.of(TeamSide.B, 0.0, 0.25),
                Arguments.of(TeamSide.B, 1.0, -2.75)
        );
    }

    private static Stream<Arguments> teamBTimingCases() {
        return Stream.of(
                Arguments.of(PlayerHitTimingGrade.EARLY, -4.5),
                Arguments.of(PlayerHitTimingGrade.PERFECT, -5.0),
                Arguments.of(PlayerHitTimingGrade.VERY_EARLY, -3.75)
        );
    }

    private static VolleyballState state() {
        return new VolleyballState(
                new BallVector3(1.0, 2.0, 3.0),
                INCOMING_VELOCITY
        );
    }

    private static PlayerBallContactEvent contact(TeamSide teamSide) {
        return new PlayerBallContactEvent(
                "player",
                teamSide,
                new BallVector3(1.0, 2.0, 3.0),
                INCOMING_VELOCITY,
                new BallVector3(0.0, 0.0, 4.5)
        );
    }
}
