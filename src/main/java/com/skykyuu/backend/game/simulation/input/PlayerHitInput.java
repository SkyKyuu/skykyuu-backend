package com.skykyuu.backend.game.simulation.input;

import java.util.Objects;

/**
 * Raw hit input. {@code aimLateral} is player-local: -1 is the player's left,
 * +1 is the player's right, and 0 is neutral.
 */
public record PlayerHitInput(
        String playerId,
        boolean hitHeld,
        double aimLateral
) {

    public PlayerHitInput(String playerId, boolean hitHeld) {
        this(playerId, hitHeld, 0.0);
    }

    public PlayerHitInput {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
    }
}
