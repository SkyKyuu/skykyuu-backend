package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.player.PlayerHitAimMath;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingAccuracyAim;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;

import java.util.Objects;

/**
 * Contact response snapshot. Forward aim is captured player-local input;
 * effective forward applies timing accuracy, then effective world Z maps the team sign.
 * All forward aim fields are telemetry only.
 */
public record PlayerBallContactResponseEvent(
        String playerId,
        TeamSide teamSide,
        BallVector3 ballPosition,
        BallVector3 incomingVelocity,
        BallVector3 outgoingVelocity,
        long hitTimingOffsetSteps,
        double hitTimingOffsetSeconds,
        PlayerHitTimingGrade hitTimingGrade,
        double hitTimingForwardMultiplier,
        double hitTimingAccuracyMultiplier,
        double hitAimLateral,
        double hitAimWorldX,
        double hitEffectiveAimLateral,
        double hitEffectiveAimWorldX,
        double hitAimVelocityX,
        double hitAimForward,
        double hitEffectiveAimForward,
        double hitEffectiveAimWorldZ
) implements BallSimulationEvent {

    /** Preserves the B15 constructor, deriving fidelity from the supplied snapshot. */
    public PlayerBallContactResponseEvent(
            String playerId,
            TeamSide teamSide,
            BallVector3 ballPosition,
            BallVector3 incomingVelocity,
            BallVector3 outgoingVelocity,
            long hitTimingOffsetSteps,
            double hitTimingOffsetSeconds,
            PlayerHitTimingGrade hitTimingGrade,
            double hitTimingForwardMultiplier,
            double hitTimingAccuracyMultiplier,
            double hitAimLateral,
            double hitAimWorldX,
            double hitEffectiveAimLateral,
            double hitEffectiveAimWorldX,
            double hitAimVelocityX,
            double hitAimForward
    ) {
        this(playerId, teamSide, ballPosition, incomingVelocity, outgoingVelocity,
                hitTimingOffsetSteps, hitTimingOffsetSeconds, hitTimingGrade,
                hitTimingForwardMultiplier, hitTimingAccuracyMultiplier,
                hitAimLateral, hitAimWorldX, hitEffectiveAimLateral, hitEffectiveAimWorldX,
                hitAimVelocityX, hitAimForward,
                PlayerHitTimingAccuracyAim.getEffectiveAimForward(
                        hitAimForward, hitTimingAccuracyMultiplier),
                PlayerHitAimMath.forwardToWorldZ(teamSide,
                        PlayerHitTimingAccuracyAim.getEffectiveAimForward(
                                hitAimForward, hitTimingAccuracyMultiplier)));
    }

    public PlayerBallContactResponseEvent(
            String playerId,
            TeamSide teamSide,
            BallVector3 ballPosition,
            BallVector3 incomingVelocity,
            BallVector3 outgoingVelocity,
            long hitTimingOffsetSteps,
            double hitTimingOffsetSeconds,
            PlayerHitTimingGrade hitTimingGrade,
            double hitTimingForwardMultiplier,
            double hitTimingAccuracyMultiplier,
            double hitAimLateral,
            double hitAimWorldX,
            double hitEffectiveAimLateral,
            double hitEffectiveAimWorldX,
            double hitAimVelocityX
    ) {
        this(playerId, teamSide, ballPosition, incomingVelocity, outgoingVelocity,
                hitTimingOffsetSteps, hitTimingOffsetSeconds, hitTimingGrade,
                hitTimingForwardMultiplier, hitTimingAccuracyMultiplier,
                hitAimLateral, hitAimWorldX, hitEffectiveAimLateral, hitEffectiveAimWorldX,
                hitAimVelocityX, 0.0, 0.0, 0.0);
    }

    public PlayerBallContactResponseEvent {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(teamSide, "teamSide must not be null");
        Objects.requireNonNull(ballPosition, "ballPosition must not be null");
        Objects.requireNonNull(incomingVelocity, "incomingVelocity must not be null");
        Objects.requireNonNull(outgoingVelocity, "outgoingVelocity must not be null");
        Objects.requireNonNull(hitTimingGrade, "hitTimingGrade must not be null");
    }
}
