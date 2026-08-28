package com.skykyuu.backend.game.simulation.ball;

import java.util.Objects;

public record VolleyballState(BallVector3 position, BallVector3 velocity) {

    public VolleyballState {
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(velocity, "velocity must not be null");
    }
}
