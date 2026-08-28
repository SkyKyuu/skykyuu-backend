package com.skykyuu.backend.game.simulation.ball;

import com.skykyuu.backend.game.court.CourtResult;
import com.skykyuu.backend.game.court.CourtSide;

import java.util.Objects;

public record BallGroundContactEvent(
        BallVector3 position,
        BallVector3 velocity,
        CourtResult courtResult,
        CourtSide courtSide
) implements BallSimulationEvent {

    public BallGroundContactEvent {
        Objects.requireNonNull(position, "position must not be null");
        Objects.requireNonNull(velocity, "velocity must not be null");
        Objects.requireNonNull(courtResult, "courtResult must not be null");
        Objects.requireNonNull(courtSide, "courtSide must not be null");
    }
}
