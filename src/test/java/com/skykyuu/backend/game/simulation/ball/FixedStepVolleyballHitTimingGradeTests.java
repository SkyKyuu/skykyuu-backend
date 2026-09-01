package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.input.PlayerHitIntent;
import com.skykyuu.backend.game.simulation.player.PlayerBallContactTarget;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedStepVolleyballHitTimingGradeTests {

    private static final double FIXED_STEP_SECONDS =
            VolleyballSimulationConfig.FIXED_STEP_SECONDS;
    private static final double TOLERANCE = 1.0e-12;

    @ParameterizedTest(name = "{1} offset {0} applies multiplier {2} for Team {3}")
    @MethodSource("gradePhysicsCases")
    void everyGradeAppliesForwardPowerWithoutChangingLateralOrUpwardVelocity(
            long offsetSteps,
            PlayerHitTimingGrade expectedGrade,
            double expectedMultiplier,
            TeamSide teamSide
    ) {
        PlayerBallContactResponseEvent response = responseForOffset(offsetSteps, teamSide);

        assertEquals(offsetSteps, response.hitTimingOffsetSteps());
        assertEquals(
                offsetSteps * FIXED_STEP_SECONDS,
                response.hitTimingOffsetSeconds(),
                TOLERANCE
        );
        assertEquals(expectedGrade, response.hitTimingGrade());
        assertEquals(expectedMultiplier, response.hitTimingForwardMultiplier());
        assertEquals(response.incomingVelocity().x(), response.outgoingVelocity().x());
        assertEquals(6.3, response.outgoingVelocity().y());
        assertEquals(teamSide == TeamSide.A
                        ? 5.0 * expectedMultiplier
                        : -5.0 * expectedMultiplier,
                response.outgoingVelocity().z());
    }

    @Test
    void zeroStepHitCanProducePerfectGrade() {
        FixedStepVolleyballSimulator simulator = newSimulator();
        double halfStep = FIXED_STEP_SECONDS / 2.0;

        BallSimulationAdvanceResult arm = simulator.advance(
                halfStep,
                List.of(farPlayer(TeamSide.A)),
                List.of(pressedIntent())
        );
        BallSimulationAdvanceResult entry = simulator.advance(
                halfStep,
                List.of(overlappingPlayer(TeamSide.A)),
                List.of()
        );

        assertEquals(0, arm.executedSteps());
        PlayerBallContactResponseEvent response = responseEvent(entry);
        assertEquals(0L, response.hitTimingOffsetSteps());
        assertEquals(PlayerHitTimingGrade.PERFECT, response.hitTimingGrade());
        assertEquals(1.0, response.hitTimingForwardMultiplier());
        assertEquals(5.0, response.outgoingVelocity().z());
    }

    private static Stream<Arguments> gradePhysicsCases() {
        return Stream.of(
                gradePhysicsCase(-4L, PlayerHitTimingGrade.VERY_EARLY, 0.75, TeamSide.A),
                gradePhysicsCase(-1L, PlayerHitTimingGrade.EARLY, 0.90, TeamSide.A),
                gradePhysicsCase(0L, PlayerHitTimingGrade.PERFECT, 1.00, TeamSide.A),
                gradePhysicsCase(1L, PlayerHitTimingGrade.LATE, 0.90, TeamSide.A),
                gradePhysicsCase(4L, PlayerHitTimingGrade.VERY_LATE, 0.75, TeamSide.A),
                gradePhysicsCase(-4L, PlayerHitTimingGrade.VERY_EARLY, 0.75, TeamSide.B),
                gradePhysicsCase(-1L, PlayerHitTimingGrade.EARLY, 0.90, TeamSide.B),
                gradePhysicsCase(0L, PlayerHitTimingGrade.PERFECT, 1.00, TeamSide.B),
                gradePhysicsCase(1L, PlayerHitTimingGrade.LATE, 0.90, TeamSide.B),
                gradePhysicsCase(4L, PlayerHitTimingGrade.VERY_LATE, 0.75, TeamSide.B)
        );
    }

    private static Arguments gradePhysicsCase(
            long offsetSteps,
            PlayerHitTimingGrade grade,
            double multiplier,
            TeamSide teamSide
    ) {
        return Arguments.of(offsetSteps, grade, multiplier, teamSide);
    }

    private static PlayerBallContactResponseEvent responseForOffset(
            long offsetSteps,
            TeamSide teamSide
    ) {
        FixedStepVolleyballSimulator simulator = newSimulator();
        PlayerBallContactTarget farPlayer = farPlayer(teamSide);
        PlayerBallContactTarget overlappingPlayer = overlappingPlayer(teamSide);

        if (offsetSteps < 0L) {
            simulator.advance(
                    FIXED_STEP_SECONDS,
                    List.of(farPlayer),
                    List.of(pressedIntent())
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
                    List.of(pressedIntent())
            ));
        }

        simulator.advance(FIXED_STEP_SECONDS, List.of(overlappingPlayer), List.of());
        for (long step = 1L; step < offsetSteps; step++) {
            simulator.advance(FIXED_STEP_SECONDS, List.of(overlappingPlayer), List.of());
        }
        return responseEvent(simulator.advance(
                FIXED_STEP_SECONDS,
                List.of(overlappingPlayer),
                List.of(pressedIntent())
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
                        new BallVector3(1.25, 0.0, 0.0)
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

    private static PlayerHitIntent pressedIntent() {
        return new PlayerHitIntent("player", true, true);
    }
}
