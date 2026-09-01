package com.skykyuu.backend.game.simulation.input;

import java.util.Objects;

/**
 * Derived hit intent. {@code aimLateral} remains player-local and is not a
 * world-X value.
 */
public record PlayerHitIntent(
        String playerId,
        boolean hitHeld,
        boolean hitPressed,
        double aimLateral
) {

    public PlayerHitIntent(String playerId, boolean hitHeld, boolean hitPressed) {
        this(playerId, hitHeld, hitPressed, 0.0);
    }

    public PlayerHitIntent {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
    }
}
