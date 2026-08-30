package com.skykyuu.backend.game.simulation.input;

import java.util.Objects;

public record PlayerHitIntent(
        String playerId,
        boolean hitHeld,
        boolean hitPressed
) {

    public PlayerHitIntent {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
    }
}
