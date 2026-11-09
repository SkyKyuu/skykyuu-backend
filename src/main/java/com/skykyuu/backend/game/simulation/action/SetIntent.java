package com.skykyuu.backend.game.simulation.action;

import java.util.Objects;

/** Captured player-local aim and button state for a set. */
public record SetIntent(
        String playerId,
        boolean setHeld,
        boolean setPressed,
        double aimLateral,
        double aimForward
) implements PlayerCommandIntent {

    public SetIntent {
        Objects.requireNonNull(playerId, "playerId must not be null");
        if (playerId.isBlank()) {
            throw new IllegalArgumentException("playerId must not be blank");
        }
    }

    @Override
    public PlayerActionType actionType() {
        return PlayerActionType.SET;
    }

    @Override
    public boolean pressed() {
        return setPressed;
    }
}
