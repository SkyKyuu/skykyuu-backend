package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballState;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerBallContactResponseTimingAccuracyAimTests {

    private static final BallVector3 INCOMING = new BallVector3(0.25, -2.0, 9.0);
    private static final double DOUBLE_TOLERANCE = 1.0e-12;

    @ParameterizedTest(name = "{0} {1}: outgoing ({3}, 6.3, {4})")
    @MethodSource("allGradesBothTeams")
    void appliesAccuracyOnlyToLateralHitContribution(
            TeamSide teamSide,
            PlayerHitTimingGrade grade,
            double accuracyMultiplier,
            double expectedVelocityX,
            double expectedVelocityZ
    ) {
        BallVector3 outgoing =
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        INCOMING,
                        teamSide,
                        grade,
                        1.0,
                        accuracyMultiplier
                );

        assertEquals(expectedVelocityX, outgoing.x(), DOUBLE_TOLERANCE);
        assertEquals(6.3, outgoing.y(), DOUBLE_TOLERANCE);
        assertEquals(expectedVelocityZ, outgoing.z(), DOUBLE_TOLERANCE);
    }

    @Test
    void preservesIncomingMomentumInsteadOfScalingEntireVelocityX() {
        BallVector3 outgoing =
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        new BallVector3(2.0, -2.0, 9.0),
                        TeamSide.B,
                        PlayerHitTimingGrade.EARLY,
                        1.0,
                        0.85
                );

        assertEquals(-0.55, outgoing.x(), DOUBLE_TOLERANCE);
    }

    @ParameterizedTest
    @EnumSource(PlayerHitTimingGrade.class)
    void neutralAimNeverCreatesLateralMovement(PlayerHitTimingGrade grade) {
        double accuracyMultiplier = PlayerHitTimingAccuracy.getAccuracyMultiplier(grade);

        for (TeamSide teamSide : TeamSide.values()) {
            BallVector3 outgoing =
                    PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                            INCOMING,
                            teamSide,
                            grade,
                            0.0,
                            accuracyMultiplier
                    );

            assertEquals(INCOMING.x(), outgoing.x(), DOUBLE_TOLERANCE);
        }
    }

    @ParameterizedTest
    @EnumSource(TeamSide.class)
    void perfectAccuracyReproducesB12Exactly(TeamSide teamSide) {
        for (double aimLateral : new double[]{-1.0, 0.0, 0.5, 1.0}) {
            assertEquals(
                    PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                            INCOMING,
                            teamSide,
                            PlayerHitTimingGrade.PERFECT,
                            aimLateral
                    ),
                    PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                            INCOMING,
                            teamSide,
                            PlayerHitTimingGrade.PERFECT,
                            aimLateral,
                            1.0
                    )
            );
        }
    }

    @ParameterizedTest
    @EnumSource(TeamSide.class)
    void legacyOverloadsUseFullAimRegardlessOfTimingGrade(TeamSide teamSide) {
        VolleyballState state = new VolleyballState(
                new BallVector3(1.0, 2.0, 3.0),
                INCOMING
        );
        PlayerBallContactEvent contact = new PlayerBallContactEvent(
                "player",
                teamSide,
                state.position(),
                INCOMING,
                new BallVector3(0.0, 0.0, 4.5)
        );

        assertEquals(
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        INCOMING,
                        teamSide,
                        PlayerHitTimingGrade.EARLY,
                        1.0,
                        1.0
                ),
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        INCOMING,
                        teamSide,
                        PlayerHitTimingGrade.EARLY,
                        1.0
                )
        );
        assertEquals(
                PlayerBallContactResponseMath.applyPlayerContactResponse(
                        state,
                        contact,
                        PlayerHitTimingGrade.EARLY,
                        1.0,
                        1.0
                ),
                PlayerBallContactResponseMath.applyPlayerContactResponse(
                        state,
                        contact,
                        PlayerHitTimingGrade.EARLY,
                        1.0
                )
        );
    }

    @Test
    void teamBNegativeAimReversesToPositiveWorldContribution() {
        BallVector3 outgoing =
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        INCOMING,
                        TeamSide.B,
                        PlayerHitTimingGrade.EARLY,
                        -1.0,
                        0.85
                );

        assertEquals(2.80, outgoing.x(), DOUBLE_TOLERANCE);
        assertEquals(-4.5, outgoing.z(), DOUBLE_TOLERANCE);
    }

    private static Stream<Arguments> allGradesBothTeams() {
        return Stream.of(
                accuracyCase(PlayerHitTimingGrade.VERY_EARLY, 0.60, 2.05, 3.75),
                accuracyCase(PlayerHitTimingGrade.EARLY, 0.85, 2.80, 4.50),
                accuracyCase(PlayerHitTimingGrade.PERFECT, 1.00, 3.25, 5.00),
                accuracyCase(PlayerHitTimingGrade.LATE, 0.85, 2.80, 4.50),
                accuracyCase(PlayerHitTimingGrade.VERY_LATE, 0.60, 2.05, 3.75)
        ).flatMap(arguments -> {
            Object[] values = arguments.get();
            PlayerHitTimingGrade grade = (PlayerHitTimingGrade) values[0];
            double accuracy = (double) values[1];
            double teamAX = (double) values[2];
            double forwardMagnitude = (double) values[3];
            return Stream.of(
                    Arguments.of(TeamSide.A, grade, accuracy, teamAX, forwardMagnitude),
                    Arguments.of(
                            TeamSide.B,
                            grade,
                            accuracy,
                            0.5 - teamAX,
                            -forwardMagnitude
                    )
            );
        });
    }

    private static Arguments accuracyCase(
            PlayerHitTimingGrade grade,
            double accuracy,
            double teamAX,
            double forwardMagnitude
    ) {
        return Arguments.of(grade, accuracy, teamAX, forwardMagnitude);
    }
}
