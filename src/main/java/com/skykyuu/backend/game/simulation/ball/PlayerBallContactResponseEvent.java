package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;

import java.util.Objects;

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
        double hitAimVelocityX
) implements BallSimulationEvent {

    public PlayerBallContactResponseEvent {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(teamSide, "teamSide must not be null");
        Objects.requireNonNull(ballPosition, "ballPosition must not be null");
        Objects.requireNonNull(incomingVelocity, "incomingVelocity must not be null");
        Objects.requireNonNull(outgoingVelocity, "outgoingVelocity must not be null");
        Objects.requireNonNull(hitTimingGrade, "hitTimingGrade must not be null");
    }
}
