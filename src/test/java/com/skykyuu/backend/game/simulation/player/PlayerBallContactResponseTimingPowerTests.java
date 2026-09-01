package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.simulation.ball.PlayerBallContactEvent;
import com.skykyuu.backend.game.simulation.ball.VolleyballState;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerBallContactResponseTimingPowerTests {

    @ParameterizedTest(name = "{0} Team {1} produces forward velocity {2}")
    @MethodSource("timingPowerCases")
    void appliesOnlyTimingGradeForwardPower(
            PlayerHitTimingGrade grade,
            TeamSide teamSide,
            double expectedForwardVelocity
    ) {
        BallVector3 incomingVelocity = new BallVector3(0.25, -2.0, 9.0);

        BallVector3 outgoingVelocity =
                PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                        incomingVelocity,
                        teamSide,
                        grade
                );

        assertEquals(0.25, outgoingVelocity.x());
        assertEquals(6.3, outgoingVelocity.y());
        assertEquals(expectedForwardVelocity, outgoingVelocity.z());
    }

    @Test
    void legacyVelocityOverloadIsEquivalentToPerfect() {
        BallVector3 incomingVelocity = new BallVector3(0.25, -2.0, 9.0);

        for (TeamSide teamSide : TeamSide.values()) {
            assertEquals(
                    PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                            incomingVelocity,
                            teamSide,
                            PlayerHitTimingGrade.PERFECT
                    ),
                    PlayerBallContactResponseMath.getDefaultPlayerContactResponseVelocity(
                            incomingVelocity,
                            teamSide
                    )
            );
        }
    }

    @Test
    void legacyApplyOverloadIsEquivalentToPerfect() {
        BallVector3 position = new BallVector3(1.0, 2.0, 3.0);
        BallVector3 incomingVelocity = new BallVector3(0.25, -2.0, 9.0);
        VolleyballState state = new VolleyballState(position, incomingVelocity);
        PlayerBallContactEvent contact = new PlayerBallContactEvent(
                "player-a",
                TeamSide.A,
                position,
                incomingVelocity,
                new BallVector3(0.0, 0.0, 4.5)
        );

        assertEquals(
                PlayerBallContactResponseMath.applyPlayerContactResponse(
                        state,
                        contact,
                        PlayerHitTimingGrade.PERFECT
                ),
                PlayerBallContactResponseMath.applyPlayerContactResponse(state, contact)
        );
    }

    private static Stream<Arguments> timingPowerCases() {
        return Stream.of(
                timingPowerCase(PlayerHitTimingGrade.VERY_EARLY, 3.75),
                timingPowerCase(PlayerHitTimingGrade.EARLY, 4.50),
                timingPowerCase(PlayerHitTimingGrade.PERFECT, 5.00),
                timingPowerCase(PlayerHitTimingGrade.LATE, 4.50),
                timingPowerCase(PlayerHitTimingGrade.VERY_LATE, 3.75)
        ).flatMap(arguments -> {
            Object[] values = arguments.get();
            return Stream.of(
                    Arguments.of(values[0], TeamSide.A, values[1]),
                    Arguments.of(values[0], TeamSide.B,
                            -((double) values[1]))
            );
        });
    }

    private static Arguments timingPowerCase(
            PlayerHitTimingGrade grade,
            double expectedMagnitude
    ) {
        return Arguments.of(grade, expectedMagnitude);
    }
}
