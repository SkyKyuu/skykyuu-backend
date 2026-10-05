package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerHitTimingAccuracyAimTests {

    @ParameterizedTest(name = "{0}, team {2}, aim {3}, incoming X {4}")
    @MethodSource("responseCases")
    void matchesFrontendMathForEveryGradeTeamAimAndIncomingSign(
            PlayerHitTimingGrade grade, double accuracy, TeamSide team,
            double rawAim, double incomingX
    ) {
        assertEquals(accuracy, PlayerHitTimingAccuracy.getAccuracyMultiplier(grade));
        double effectiveAim = PlayerHitTimingAccuracyAim.getEffectiveAimLateral(rawAim, accuracy);
        assertEquals(rawAim * accuracy, effectiveAim);
        double expectedWorldX = team == TeamSide.A ? effectiveAim : -effectiveAim;
        assertEquals(expectedWorldX, PlayerHitAimMath.lateralToWorldX(team, effectiveAim), 1.0e-12);
        BallVector3 incoming = new BallVector3(incomingX, -2.0, 9.0);
        BallVector3 outgoing = PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                incoming, team, grade, rawAim, accuracy
        );
        assertEquals(incomingX + expectedWorldX * 3.0, outgoing.x(), 1.0e-12);
        assertEquals(incomingX, incoming.x());
        assertEquals(6.3, outgoing.y());
        double forward = 5.0 * PlayerHitTimingPower.getForwardMultiplier(grade);
        assertEquals(team == TeamSide.A ? forward : -forward, outgoing.z());
        assertEquals(outgoing, PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                incoming, team, grade, rawAim
        ));
        assertEquals(outgoing, PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                incoming, team, grade, rawAim, accuracy
        ));
    }

    @Test
    void preservesIncomingTwoWithEarlyTeamBPositiveAim() {
        double effective = PlayerHitTimingAccuracyAim.getEffectiveAimLateral(1.0, 0.85);
        assertEquals(0.85, effective);
        assertEquals(-0.85, PlayerHitAimMath.lateralToWorldX(TeamSide.B, effective));
        assertEquals(-2.55, PlayerHitAimMath.getVelocityXContribution(TeamSide.B, effective), 1.0e-12);
        BallVector3 outgoing = PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                new BallVector3(2.0, 0.0, 0.0), TeamSide.B,
                PlayerHitTimingGrade.EARLY, 1.0, 0.85
        );
        assertEquals(-0.55, outgoing.x(), 1.0e-12);
    }

    @Test
    void responseUsesSuppliedAccuracyInsteadOfRecalculatingFromGrade() {
        BallVector3 outgoing = PlayerBallContactResponseMath.getPlayerContactResponseVelocity(
                new BallVector3(2.0, 0.0, 0.0), TeamSide.B,
                PlayerHitTimingGrade.PERFECT, 1.0, 0.85
        );
        assertEquals(-0.55, outgoing.x(), 1.0e-12);
        assertEquals(-5.0, outgoing.z());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.1, 1.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void rejectsInvalidAccuracyLikeFrontend(double accuracy) {
        assertThrows(IllegalArgumentException.class,
                () -> PlayerHitTimingAccuracyAim.getEffectiveAimLateral(0.0, accuracy));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void validatesRawAimBeforeApplyingAccuracy(double aim) {
        assertThrows(IllegalArgumentException.class,
                () -> PlayerHitTimingAccuracyAim.getEffectiveAimLateral(aim, 0.60));
    }

    private static Stream<Arguments> responseCases() {
        return Stream.of(
                Arguments.of(PlayerHitTimingGrade.VERY_EARLY, 0.60),
                Arguments.of(PlayerHitTimingGrade.EARLY, 0.85),
                Arguments.of(PlayerHitTimingGrade.PERFECT, 1.00),
                Arguments.of(PlayerHitTimingGrade.LATE, 0.85),
                Arguments.of(PlayerHitTimingGrade.VERY_LATE, 0.60)
        ).flatMap(timing -> Stream.of(TeamSide.values()).flatMap(team ->
                Stream.of(-1.0, 0.0, 0.375, 1.0).flatMap(aim ->
                        Stream.of(-2.0, 2.0).map(incoming -> Arguments.of(
                                timing.get()[0], timing.get()[1], team, aim, incoming
                        )))));
    }
}
