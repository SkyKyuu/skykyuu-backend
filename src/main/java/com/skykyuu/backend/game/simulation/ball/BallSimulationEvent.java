package com.skykyuu.backend.game.simulation.ball;

public sealed interface BallSimulationEvent permits BallGroundContactEvent,
        PlayerBallContactEvent {
}
