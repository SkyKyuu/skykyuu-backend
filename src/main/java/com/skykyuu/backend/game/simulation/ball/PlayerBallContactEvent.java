package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.team.TeamSide;

import java.util.Objects;

public record PlayerBallContactEvent(
        String playerId,
        TeamSide teamSide,
        BallVector3 ballPosition,
        BallVector3 ballVelocity,
        BallVector3 playerPosition
) implements BallSimulationEvent {

    public PlayerBallContactEvent {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(teamSide, "teamSide must not be null");
        Objects.requireNonNull(ballPosition, "ballPosition must not be null");
        Objects.requireNonNull(ballVelocity, "ballVelocity must not be null");
        Objects.requireNonNull(playerPosition, "playerPosition must not be null");
    }
}
