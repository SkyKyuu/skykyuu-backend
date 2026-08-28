package com.skykyuu.backend.game.simulation.ball;

import java.util.Objects;
import java.util.OptionalDouble;

public final class VolleyballSimulationMath {

    private static final double CONTACT_TIME_EPSILON_SECONDS = 1.0e-12;
    private static final double DESCENDING_VELOCITY_EPSILON = 1.0e-12;

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

    public static OptionalDouble findGroundContactTime(
            VolleyballState state,
            double maximumDeltaSeconds
    ) {
        Objects.requireNonNull(state, "state must not be null");

        if (!Double.isFinite(maximumDeltaSeconds) || maximumDeltaSeconds < 0.0) {
            return OptionalDouble.empty();
        }

        double initialY = state.position().y();
        double initialVelocityY = state.velocity().y();
        if (!Double.isFinite(initialY) || !Double.isFinite(initialVelocityY)) {
            return OptionalDouble.empty();
        }

        double gravity = VolleyballSimulationConfig.GRAVITY_METERS_PER_SECOND_SQUARED;
        double heightAboveGround = initialY - VolleyballConfig.RADIUS_METERS;
        double discriminant = initialVelocityY * initialVelocityY
                + 2.0 * gravity * heightAboveGround;
        if (!Double.isFinite(discriminant) || discriminant < 0.0) {
            return OptionalDouble.empty();
        }

        double squareRoot = Math.sqrt(discriminant);
        double firstCandidate = (initialVelocityY - squareRoot) / gravity;
        double secondCandidate = (initialVelocityY + squareRoot) / gravity;

        OptionalDouble firstContact = validDescendingContact(
                firstCandidate,
                initialVelocityY,
                gravity,
                maximumDeltaSeconds
        );
        if (firstContact.isPresent()) {
            return firstContact;
        }

        return validDescendingContact(
                secondCandidate,
                initialVelocityY,
                gravity,
                maximumDeltaSeconds
        );
    }

    private static OptionalDouble validDescendingContact(
            double candidateSeconds,
            double initialVelocityY,
            double gravity,
            double maximumDeltaSeconds
    ) {
        if (!Double.isFinite(candidateSeconds)
                || candidateSeconds < -CONTACT_TIME_EPSILON_SECONDS
                || candidateSeconds
                > maximumDeltaSeconds + CONTACT_TIME_EPSILON_SECONDS) {
            return OptionalDouble.empty();
        }

        double impactVelocityY = initialVelocityY - gravity * candidateSeconds;
        if (impactVelocityY > DESCENDING_VELOCITY_EPSILON) {
            return OptionalDouble.empty();
        }

        double normalizedContactTime = Math.clamp(
                candidateSeconds,
                0.0,
                maximumDeltaSeconds
        );
        return OptionalDouble.of(normalizedContactTime);
    }
}
