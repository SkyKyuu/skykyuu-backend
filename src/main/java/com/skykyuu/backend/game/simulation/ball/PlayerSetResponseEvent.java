package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.simulation.action.PlayerActionType;
import com.skykyuu.backend.game.simulation.player.PlayerHitTimingGrade;
import com.skykyuu.backend.game.team.TeamSide;

import java.util.Objects;

/** Applied response snapshot. Contact step is the entry step of the continuous overlap. */
public record PlayerSetResponseEvent(
        String playerId,
        TeamSide teamSide,
        long pressStep,
        long contactStep,
        BallVector3 ballPosition,
        BallVector3 incomingVelocity,
        BallVector3 outgoingVelocity,
        long timingOffsetSteps,
        double timingOffsetSeconds,
        PlayerHitTimingGrade timingGrade,
        double timingPowerMultiplier,
        double timingAccuracyMultiplier,
        double aimLateral,
        double aimForward,
        double effectiveAimLateral,
        double effectiveAimForward
) implements BallSimulationEvent {

    public PlayerSetResponseEvent {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(teamSide, "teamSide must not be null");
        Objects.requireNonNull(ballPosition, "ballPosition must not be null");
        Objects.requireNonNull(incomingVelocity, "incomingVelocity must not be null");
        Objects.requireNonNull(outgoingVelocity, "outgoingVelocity must not be null");
        Objects.requireNonNull(timingGrade, "timingGrade must not be null");
    }

    public PlayerActionType actionType() {
        return PlayerActionType.SET;
    }
}
