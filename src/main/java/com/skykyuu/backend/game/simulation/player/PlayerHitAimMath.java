package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.team.TeamSide;

import java.util.Objects;

public final class PlayerHitAimMath {

    private PlayerHitAimMath() {
    }

    public static double lateralToWorldX(TeamSide teamSide, double aimLateral) {
        Objects.requireNonNull(teamSide, "teamSide must not be null");
        double validatedAim = PlayerHitAim.validateLateral(aimLateral);
        return playerLocalAimToWorldAxis(teamSide, validatedAim);
    }

    public static double forwardToWorldZ(TeamSide teamSide, double aimForward) {
        Objects.requireNonNull(teamSide, "teamSide must not be null");
        return playerLocalAimToWorldAxis(teamSide, PlayerHitAim.validateForward(aimForward));
    }

    private static double playerLocalAimToWorldAxis(TeamSide teamSide, double validatedAim) {
        double canonicalAim = validatedAim == 0.0 ? 0.0 : validatedAim;
        return switch (teamSide) {
            case A -> canonicalAim;
            case B -> canonicalAim == 0.0 ? 0.0 : -canonicalAim;
        };
    }

    public static double getVelocityXContribution(
            TeamSide teamSide,
            double aimLateral
    ) {
        return lateralToWorldX(teamSide, aimLateral)
                * PlayerHitAimPhysicsConfig
                .MAX_LATERAL_VELOCITY_CONTRIBUTION_METERS_PER_SECOND;
    }
}
