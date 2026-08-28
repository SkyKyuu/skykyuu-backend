package com.skykyuu.backend.game.simulation.ball;

import java.util.Objects;

public final class VolleyballSimulationMath {

    private VolleyballSimulationMath() {
    }

    public static VolleyballState stepFreeFlight(VolleyballState state, double deltaSeconds) {
        Objects.requireNonNull(state, "state must not be null");

        BallVector3 position = state.position();
        BallVector3 velocity = state.velocity();
        double gravity = VolleyballSimulationConfig.GRAVITY_METERS_PER_SECOND_SQUARED;
        double deltaSecondsSquared = deltaSeconds * deltaSeconds;

        BallVector3 nextPosition = new BallVector3(
                position.x() + velocity.x() * deltaSeconds,
                position.y() + velocity.y() * deltaSeconds
                        - 0.5 * gravity * deltaSecondsSquared,
                position.z() + velocity.z() * deltaSeconds
        );
        BallVector3 nextVelocity = new BallVector3(
                velocity.x(),
                velocity.y() - gravity * deltaSeconds,
                velocity.z()
        );

        return new VolleyballState(nextPosition, nextVelocity);
    }
}
