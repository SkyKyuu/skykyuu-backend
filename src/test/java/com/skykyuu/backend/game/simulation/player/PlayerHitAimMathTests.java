package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.team.TeamSide;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerHitAimMathTests {

    @ParameterizedTest(name = "Team {0}, local {1} maps to world Z {2}")
    @MethodSource("worldZCases")
    void mapsForwardToWorldZWithoutQuantization(TeamSide team, double raw, double worldZ) {
        assertEquals(worldZ, PlayerHitAimMath.forwardToWorldZ(team, raw));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.0, -0.0})
    void canonicalizesWorldZZeroForBothTeams(double raw) {
        for (TeamSide team : TeamSide.values()) {
            assertEquals(Double.doubleToRawLongBits(0.0),
                    Double.doubleToRawLongBits(PlayerHitAimMath.forwardToWorldZ(team, raw)));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void reusesForwardValidation(double raw) {
        for (TeamSide team : TeamSide.values()) {
            assertThrows(IllegalArgumentException.class,
                    () -> PlayerHitAimMath.forwardToWorldZ(team, raw));
        }
    }

    @Test
    void rejectsMissingForwardTeam() {
        assertThrows(NullPointerException.class, () -> PlayerHitAimMath.forwardToWorldZ(null, 1.0));
    }

    private static Stream<Arguments> worldZCases() {
        return Stream.of(TeamSide.values()).flatMap(team ->
                Stream.of(-1.0, 1.0, 0.5, -0.7071067811865476, 0.7071067811865476)
                        .map(raw -> Arguments.of(team, raw, team == TeamSide.A ? raw : -raw)));
    }

    @ParameterizedTest(name = "Team {0}, local {1} maps to world X {2}")
    @MethodSource("worldXCases")
    void mapsPlayerLocalAimToWorldX(
            TeamSide teamSide,
            double aimLateral,
            double expectedWorldX
    ) {
        assertEquals(
                expectedWorldX,
                PlayerHitAimMath.lateralToWorldX(teamSide, aimLateral)
        );
    }

    @ParameterizedTest(name = "Team {0}, local {1} contributes velocity X {2}")
    @MethodSource("velocityContributionCases")
    void convertsPlayerLocalAimToVelocityXContribution(
            TeamSide teamSide,
            double aimLateral,
            double expectedVelocityX
    ) {
        assertEquals(
                expectedVelocityX,
                PlayerHitAimMath.getVelocityXContribution(teamSide, aimLateral)
        );
    }

    @ParameterizedTest
    @ValueSource(doubles = {
            -1.01,
            1.01,
            Double.NaN,
            Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY
    })
    void reusesLateralAimValidation(double aimLateral) {
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerHitAimMath.lateralToWorldX(TeamSide.A, aimLateral)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> PlayerHitAimMath.getVelocityXContribution(
                        TeamSide.B,
                        aimLateral
                )
        );
    }

    private static Stream<Arguments> worldXCases() {
        return Stream.of(
                Arguments.of(TeamSide.A, -1.0, -1.0),
                Arguments.of(TeamSide.A, 0.0, 0.0),
                Arguments.of(TeamSide.A, 0.375, 0.375),
                Arguments.of(TeamSide.A, 1.0, 1.0),
                Arguments.of(TeamSide.B, -1.0, 1.0),
                Arguments.of(TeamSide.B, 0.0, 0.0),
                Arguments.of(TeamSide.B, 0.375, -0.375),
                Arguments.of(TeamSide.B, 1.0, -1.0)
        );
    }

    private static Stream<Arguments> velocityContributionCases() {
        return Stream.of(
                Arguments.of(TeamSide.A, -1.0, -3.0),
                Arguments.of(TeamSide.A, 0.0, 0.0),
                Arguments.of(TeamSide.A, 0.5, 1.5),
                Arguments.of(TeamSide.A, 1.0, 3.0),
                Arguments.of(TeamSide.B, -1.0, 3.0),
                Arguments.of(TeamSide.B, 0.0, 0.0),
                Arguments.of(TeamSide.B, 0.5, -1.5),
                Arguments.of(TeamSide.B, 1.0, -3.0)
        );
    }
}
