package com.skykyuu.backend.game.simulation.input;

import java.util.Objects;

/**
 * Derived hit intent. Both aim axes remain player-local, not world X/Z.
 * {@code aimForward}: -1 is backward, 0 neutral, and +1 forward.
 */
public record PlayerHitIntent(
        String playerId,
        boolean hitHeld,
        boolean hitPressed,
        double aimLateral,
        double aimForward
) {

    public PlayerHitIntent(String playerId, boolean hitHeld, boolean hitPressed) {
        this(playerId, hitHeld, hitPressed, 0.0, 0.0);
    }

    public PlayerHitIntent(String playerId, boolean hitHeld, boolean hitPressed, double aimLateral) {
        this(playerId, hitHeld, hitPressed, aimLateral, 0.0);
    }

    public PlayerHitIntent {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
    }
}
