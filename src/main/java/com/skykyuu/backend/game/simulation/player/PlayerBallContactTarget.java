package com.skykyuu.backend.game.simulation.player;

import com.skykyuu.backend.game.simulation.ball.BallVector3;
import com.skykyuu.backend.game.team.TeamSide;

import java.util.Objects;

public record PlayerBallContactTarget(
        String playerId,
        TeamSide teamSide,
        BallVector3 position
) {

    public PlayerBallContactTarget {
        Objects.requireNonNull(playerId, "playerId must not be null");
        Objects.requireNonNull(teamSide, "teamSide must not be null");
        Objects.requireNonNull(position, "position must not be null");
    }
}
