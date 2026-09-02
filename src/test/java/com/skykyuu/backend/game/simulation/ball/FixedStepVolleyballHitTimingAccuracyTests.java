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
    private static final double DOUBLE_TOLERANCE = 1.0e-12;

    @ParameterizedTest(name = "{1} {2} applies accuracy {4} only to hit aim")
    @MethodSource("aimPhysicsInvariantCases")
    void accuracyAppliesOnlyToLateralHitContribution(
            long offsetSteps,
            PlayerHitTimingGrade expectedGrade,
            TeamSide teamSide,
            double expectedForwardMultiplier,
            double expectedAccuracyMultiplier,
            double expectedAimWorldX,
            double expectedEffectiveAimLateral,
            double expectedEffectiveAimWorldX,
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
        assertEquals(1.0, response.hitAimLateral(), DOUBLE_TOLERANCE);
        assertEquals(expectedAimWorldX, response.hitAimWorldX(), DOUBLE_TOLERANCE);
        assertEquals(
                expectedEffectiveAimLateral,
                response.hitEffectiveAimLateral(),
                DOUBLE_TOLERANCE
        );
        assertEquals(
                expectedEffectiveAimWorldX,
                response.hitEffectiveAimWorldX(),
                DOUBLE_TOLERANCE
        );
        assertEquals(
                expectedAimVelocityX,
                response.hitAimVelocityX(),
                DOUBLE_TOLERANCE
        );
        assertEquals(0.25, response.incomingVelocity().x(), DOUBLE_TOLERANCE);
        assertEquals(expectedVelocityX, response.outgoingVelocity().x(), DOUBLE_TOLERANCE);
        assertEquals(6.3, response.outgoingVelocity().y(), DOUBLE_TOLERANCE);
        assertEquals(expectedVelocityZ, response.outgoingVelocity().z(), DOUBLE_TOLERANCE);
    }

    private static Stream<Arguments> aimPhysicsInvariantCases() {
        return Stream.of(
                Arguments.of(
                        -4L, PlayerHitTimingGrade.VERY_EARLY, TeamSide.B,
                        0.75, 0.60, -1.0, 0.60, -0.60, -1.80, -1.55, -3.75
                ),
                Arguments.of(
                        -1L, PlayerHitTimingGrade.EARLY, TeamSide.B,
                        0.90, 0.85, -1.0, 0.85, -0.85, -2.55, -2.30, -4.5
                ),
                Arguments.of(
                        0L, PlayerHitTimingGrade.PERFECT, TeamSide.B,
                        1.00, 1.00, -1.0, 1.00, -1.00, -3.00, -2.75, -5.0
                ),
                Arguments.of(
                        1L, PlayerHitTimingGrade.LATE, TeamSide.B,
                        0.90, 0.85, -1.0, 0.85, -0.85, -2.55, -2.30, -4.5
                ),
                Arguments.of(
                        4L, PlayerHitTimingGrade.VERY_LATE, TeamSide.B,
                        0.75, 0.60, -1.0, 0.60, -0.60, -1.80, -1.55, -3.75
                ),
                Arguments.of(
                        -4L, PlayerHitTimingGrade.VERY_EARLY, TeamSide.A,
                        0.75, 0.60, 1.0, 0.60, 0.60, 1.80, 2.05, 3.75
                ),
                Arguments.of(
                        -1L, PlayerHitTimingGrade.EARLY, TeamSide.A,
                        0.90, 0.85, 1.0, 0.85, 0.85, 2.55, 2.80, 4.5
                ),
                Arguments.of(
                        0L, PlayerHitTimingGrade.PERFECT, TeamSide.A,
                        1.00, 1.00, 1.0, 1.00, 1.00, 3.00, 3.25, 5.0
                ),
                Arguments.of(
                        1L, PlayerHitTimingGrade.LATE, TeamSide.A,
                        0.90, 0.85, 1.0, 0.85, 0.85, 2.55, 2.80, 4.5
                ),
                Arguments.of(
                        4L, PlayerHitTimingGrade.VERY_LATE, TeamSide.A,
                        0.75, 0.60, 1.0, 0.60, 0.60, 1.80, 2.05, 3.75
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

        if (offsetSteps == 0L) {
            return responseEvent(simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(overlappingPlayer),
                    List.of(pressedIntent(aimLateral))
            ));
        }

        simulator.advance(FIXED_STEP_SECONDS, List.of(overlappingPlayer), List.of());
        for (long step = 1L; step < offsetSteps; step++) {
            simulator.advance(FIXED_STEP_SECONDS, List.of(overlappingPlayer), List.of());
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
