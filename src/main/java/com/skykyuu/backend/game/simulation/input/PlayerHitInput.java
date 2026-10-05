package com.skykyuu.backend.game.simulation.input;

import java.util.Objects;

/**
 * Raw hit input. {@code aimLateral} is player-local: -1 is the player's left,
 * +1 is the player's right, and 0 is neutral.
 * {@code aimForward} is also player-local: -1 backward, 0 neutral, +1 forward.
 */
public record PlayerHitInput(
        String playerId,
        boolean hitHeld,
        double aimLateral,
        double aimForward
) {

    public PlayerHitInput(String playerId, boolean hitHeld) {
        this(playerId, hitHeld, 0.0, 0.0);
    }

    public PlayerHitInput(String playerId, boolean hitHeld, double aimLateral) {
        this(playerId, hitHeld, aimLateral, 0.0);
    }

    public PlayerHitInput {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
    }
}
