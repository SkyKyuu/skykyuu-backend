package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedStepVolleyballHitTimingAccuracyTests {

    private static final double FIXED_STEP_SECONDS =
            VolleyballSimulationConfig.FIXED_STEP_SECONDS;

    @ParameterizedTest(name = "{1} {2} carries accuracy {4} without changing aim")
    @MethodSource("aimPhysicsInvariantCases")
    void accuracyTelemetryDoesNotModifyB12AimPhysics(
            long offsetSteps,
            PlayerHitTimingGrade expectedGrade,
            TeamSide teamSide,
            double expectedForwardMultiplier,
            double expectedAccuracyMultiplier,
            double expectedAimWorldX,
            double expectedAimVelocityX,
            double expectedVelocityX,
            double expectedVelocityZ
    ) {
        PlayerBallContactResponseEvent response = responseForOffset(
                offsetSteps,
                teamSide,
                1.0
        );

        assertEquals(expectedGrade, response.hitTimingGrade());
        assertEquals(
                expectedForwardMultiplier,
                response.hitTimingForwardMultiplier()
        );
        assertEquals(
                expectedAccuracyMultiplier,
                response.hitTimingAccuracyMultiplier()
        );
        assertEquals(1.0, response.hitAimLateral());
        assertEquals(expectedAimWorldX, response.hitAimWorldX());
        assertEquals(expectedAimVelocityX, response.hitAimVelocityX());
        assertEquals(0.25, response.incomingVelocity().x());
        assertEquals(
                new BallVector3(expectedVelocityX, 6.3, expectedVelocityZ),
                response.outgoingVelocity()
        );
    }

    private static Stream<Arguments> aimPhysicsInvariantCases() {
        return Stream.of(
                Arguments.of(
                        -4L, PlayerHitTimingGrade.VERY_EARLY, TeamSide.B,
                        0.75, 0.60, -1.0, -3.0, -2.75, -3.75
                ),
                Arguments.of(
                        -1L, PlayerHitTimingGrade.EARLY, TeamSide.B,
                        0.90, 0.85, -1.0, -3.0, -2.75, -4.5
                ),
                Arguments.of(
                        0L, PlayerHitTimingGrade.PERFECT, TeamSide.B,
                        1.00, 1.00, -1.0, -3.0, -2.75, -5.0
                ),
                Arguments.of(
                        -4L, PlayerHitTimingGrade.VERY_EARLY, TeamSide.A,
                        0.75, 0.60, 1.0, 3.0, 3.25, 3.75
                ),
                Arguments.of(
                        -1L, PlayerHitTimingGrade.EARLY, TeamSide.A,
                        0.90, 0.85, 1.0, 3.0, 3.25, 4.5
                ),
                Arguments.of(
                        0L, PlayerHitTimingGrade.PERFECT, TeamSide.A,
                        1.00, 1.00, 1.0, 3.0, 3.25, 5.0
                )
        );
    }

    private static PlayerBallContactResponseEvent responseForOffset(
            long offsetSteps,
            TeamSide teamSide,
            double aimLateral
    ) {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer(teamSide);
        PlayerBallContactTarget overlappingPlayer = overlappingPlayer(teamSide);

        if (offsetSteps < 0L) {
            simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(farPlayer),
                    List.of(pressedIntent(aimLateral))
            );
            for (long step = 1L; step < -offsetSteps; step++) {
                simulator.advance(FIXED_STEP_SECONDS, List.of(farPlayer), List.of());
            }
            return responseEvent(simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(overlappingPlayer),
                    List.of()
            ));
        }

        return responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent(aimLateral))
        ));
    }

    private static PlayerBallContactResponseEvent responseEvent(
            BallSimulationAdvanceResult result
    ) {
        return result.events().stream()
                .filter(PlayerBallContactResponseEvent.class::isInstance)
                .map(PlayerBallContactResponseEvent.class::cast)
                .findFirst()
                .orElseThrow();
    }

    private static FixedStepVolleyballSimulator newSimulator() {
        return new FixedStepVolleyballSimulator(
                new VolleyballState(
                        new BallVector3(0.0, 1.0, 0.0),
                        new BallVector3(0.25, 0.0, 0.0)
                )
        );
    }

    private static PlayerBallContactTarget farPlayer(TeamSide teamSide) {
        return new PlayerBallContactTarget(
                "player",
                teamSide,
                new BallVector3(10.0, 0.0, 0.0)
        );
    }

    private static PlayerBallContactTarget overlappingPlayer(TeamSide teamSide) {
        return new PlayerBallContactTarget(
                "player",
                teamSide,
                new BallVector3(0.0, 0.0, 0.0)
        );
    }

    private static PlayerHitIntent pressedIntent(double aimLateral) {
        return new PlayerHitIntent("player", true, true, aimLateral);
    }
}
