package com.skykyuu.backend.game.simulation.input;

import java.util.Objects;

public record PlayerHitInput(
        String playerId,
        boolean hitHeld
) {

    public PlayerHitInput {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
    }
}
